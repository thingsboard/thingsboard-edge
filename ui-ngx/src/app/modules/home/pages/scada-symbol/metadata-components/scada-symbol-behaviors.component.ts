///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import {
  Component,
  DestroyRef,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  QueryList,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import {
  defaultGetValueSettings,
  ScadaSymbolBehavior,
  ScadaSymbolBehaviorType
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { ValueType } from '@shared/models/constants';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import {
  behaviorValid,
  ScadaSymbolBehaviorRowComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-behavior-row.component';
import { GetValueSettings, ValueToDataType } from '@shared/models/action-widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { mergeDeep } from '@core/utils';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-scada-symbol-metadata-behaviors',
  templateUrl: './scada-symbol-behaviors.component.html',
  styleUrls: ['./scada-symbol-behaviors.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolBehaviorsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolBehaviorsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolBehaviorsComponent implements ControlValueAccessor, OnInit, Validator {

  @HostBinding('style.display') styleDisplay = 'flex';
  @HostBinding('style.overflow') styleOverflow = 'hidden';

  @ViewChildren(ScadaSymbolBehaviorRowComponent)
  behaviorRows: QueryList<ScadaSymbolBehaviorRowComponent>;

  @Input()
  aliasController: IAliasController;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Input()
  disabled: boolean;

  behaviorsFormGroup: UntypedFormGroup;

  errorText = '';

  get dragEnabled(): boolean {
    return !this.disabled && this.behaviorsFormArray().controls.length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.behaviorsFormGroup = this.fb.group({
      behaviors: this.fb.array([])
    });
    this.behaviorsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let behaviors: ScadaSymbolBehavior[] = this.behaviorsFormGroup.get('behaviors').value;
        if (behaviors) {
          behaviors = behaviors.filter(b => behaviorValid(b));
        }
        this.propagateChange(behaviors);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.behaviorsFormGroup.disable({emitEvent: false});
    } else {
      this.behaviorsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ScadaSymbolBehavior[] | undefined): void {
    const behaviors= value || [];
    this.behaviorsFormGroup.setControl('behaviors', this.prepareBehaviorsFormArray(behaviors), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    this.errorText = '';
    const behaviorsArray = this.behaviorsFormGroup.get('behaviors') as UntypedFormArray;
    const notUniqueControls =
      behaviorsArray.controls.filter(control => control.hasError('behaviorIdNotUnique'));
    for (const control of notUniqueControls) {
      control.updateValueAndValidity({onlySelf: false, emitEvent: false});
      if (control.hasError('behaviorIdNotUnique')) {
        this.errorText = this.translate.instant('scada.behavior.not-unique-behavior-ids-error');
      }
    }
    const valid =  this.behaviorsFormGroup.valid;
    return valid ? null : {
      behaviors: {
        valid: false,
      },
    };
  }

  public behaviorIdUnique(id: string, index: number): boolean {
    const behaviorsArray = this.behaviorsFormGroup.get('behaviors') as UntypedFormArray;
    for (let i = 0; i < behaviorsArray.controls.length; i++) {
      if (i !== index) {
        const otherControl = behaviorsArray.controls[i];
        if (id === otherControl.value.id) {
          return false;
        }
      }
    }
    return true;
  }

  behaviorDrop(event: CdkDragDrop<string[]>) {
    const behaviorsArray = this.behaviorsFormGroup.get('behaviors') as UntypedFormArray;
    const behavior = behaviorsArray.at(event.previousIndex);
    behaviorsArray.removeAt(event.previousIndex, {emitEvent: false});
    behaviorsArray.insert(event.currentIndex, behavior, {emitEvent: true});
  }

  behaviorsFormArray(): UntypedFormArray {
    return this.behaviorsFormGroup.get('behaviors') as UntypedFormArray;
  }

  trackByBehavior(index: number, behaviorControl: AbstractControl): any {
    return behaviorControl;
  }

  removeBehavior(index: number, emitEvent = true) {
    (this.behaviorsFormGroup.get('behaviors') as UntypedFormArray).removeAt(index, {emitEvent});
  }

  addBehavior() {
    const behavior: ScadaSymbolBehavior = {
      id: '',
      name: '',
      type: ScadaSymbolBehaviorType.value,
      valueType: ValueType.BOOLEAN,
      defaultGetValueSettings: mergeDeep({} as GetValueSettings<any>, defaultGetValueSettings(ValueType.BOOLEAN))
    };
    const behaviorsArray = this.behaviorsFormGroup.get('behaviors') as UntypedFormArray;
    const behaviorControl = this.fb.control(behavior, []);
    behaviorsArray.push(behaviorControl);
    setTimeout(() => {
      const behaviorRow = this.behaviorRows.get(this.behaviorRows.length-1);
      behaviorRow.onAdd(() => {
        this.removeBehavior(behaviorsArray.length-1);
      });
    });
  }

  private prepareBehaviorsFormArray(behaviors: ScadaSymbolBehavior[] | undefined): UntypedFormArray {
    const behaviorsControls: Array<AbstractControl> = [];
    if (behaviors) {
      behaviors.forEach((behavior) => {
        behaviorsControls.push(this.fb.control(behavior, []));
      });
    }
    return this.fb.array(behaviorsControls);
  }
}
