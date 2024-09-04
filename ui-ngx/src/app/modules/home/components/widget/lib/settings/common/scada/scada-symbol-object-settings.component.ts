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

import { ChangeDetectorRef, Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  defaultScadaSymbolObjectSettings,
  parseScadaSymbolMetadataFromContent,
  ScadaSymbolBehaviorType,
  ScadaSymbolMetadata,
  ScadaSymbolObjectSettings,
  ScadaSymbolPropertyType
} from '@home/components/widget/lib/scada/scada-symbol.models';
import { IAliasController } from '@core/api/widget-api.models';
import { TargetDevice, widgetType } from '@shared/models/widget.models';
import { isDefinedAndNotNull, mergeDeep } from '@core/utils';
import {
  ScadaSymbolBehaviorGroup,
  ScadaSymbolPropertyRow,
  toBehaviorGroups,
  toPropertyRows
} from '@home/components/widget/lib/settings/common/scada/scada-symbol-object-settings.models';
import { merge, Observable, of, Subscription } from 'rxjs';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { ImageService } from '@core/http/image.service';
import { map } from 'rxjs/operators';

@Component({
  selector: 'tb-scada-symbol-object-settings',
  templateUrl: './scada-symbol-object-settings.component.html',
  styleUrls: ['./scada-symbol-object-settings.component.scss', './../../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolObjectSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolObjectSettingsComponent),
      multi: true
    }
  ]
})
export class ScadaSymbolObjectSettingsComponent implements OnInit, OnChanges, ControlValueAccessor, Validator {

  ScadaSymbolBehaviorType = ScadaSymbolBehaviorType;

  ScadaSymbolPropertyType = ScadaSymbolPropertyType;

  @Input()
  disabled: boolean;

  @Input()
  scadaSymbolUrl: string;

  @Input()
  scadaSymbolContent: string;

  @Input()
  scadaSymbolMetadata: ScadaSymbolMetadata;

  @Input()
  aliasController: IAliasController;

  @Input()
  targetDevice: TargetDevice;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Input()
  widgetType: widgetType;

  private modelValue: ScadaSymbolObjectSettings;

  private propagateChange = null;

  private validatorTriggers: string[];
  private validatorSubscription: Subscription;

  public scadaSymbolObjectSettingsFormGroup: UntypedFormGroup;

  metadata: ScadaSymbolMetadata;
  behaviorGroups: ScadaSymbolBehaviorGroup[];
  propertyRows: ScadaSymbolPropertyRow[];

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private imageService: ImageService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.scadaSymbolObjectSettingsFormGroup = this.fb.group({
      behavior: this.fb.group({}),
      properties: this.fb.group({})
    });
    this.scadaSymbolObjectSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.loadMetadata();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['scadaSymbolUrl', 'scadaSymbolContent', 'scadaSymbolMetadata'].includes(propName)) {
          this.loadMetadata();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.scadaSymbolObjectSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.scadaSymbolObjectSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: ScadaSymbolObjectSettings): void {
    this.modelValue = value || { behavior: {}, properties: {} };
    this.setupValue();
  }

  validate(_c: UntypedFormControl) {
    const valid = this.scadaSymbolObjectSettingsFormGroup.valid;
    return valid ? null : {
      scadaSymbolObjectSettings: {
        valid: false,
      },
    };
  }

  private loadMetadata() {
    if (this.validatorSubscription) {
      this.validatorSubscription.unsubscribe();
      this.validatorSubscription = null;
    }
    this.validatorTriggers = [];

    let metadata$: Observable<ScadaSymbolMetadata>;
    if (this.scadaSymbolMetadata) {
      metadata$ = of(this.scadaSymbolMetadata);
    } else {
      let content$: Observable<string>;
      if (this.scadaSymbolContent) {
        content$ = of(this.scadaSymbolContent);
      } else if (this.scadaSymbolUrl) {
        content$ = this.imageService.getImageString(this.scadaSymbolUrl);
      } else {
        content$ = of('<svg></svg>');
      }
      metadata$ = content$.pipe(
        map(content => parseScadaSymbolMetadataFromContent(content))
      );
    }
    metadata$.subscribe(
      (metadata) => {
        this.metadata = metadata;
        this.behaviorGroups = toBehaviorGroups(this.metadata.behavior);
        this.propertyRows = toPropertyRows(this.metadata.properties);
        const behaviorFormGroup =  this.scadaSymbolObjectSettingsFormGroup.get('behavior') as UntypedFormGroup;
        for (const control of Object.keys(behaviorFormGroup.controls)) {
          behaviorFormGroup.removeControl(control, {emitEvent: false});
        }
        const propertiesFormGroup =  this.scadaSymbolObjectSettingsFormGroup.get('properties') as UntypedFormGroup;
        for (const control of Object.keys(propertiesFormGroup.controls)) {
          propertiesFormGroup.removeControl(control, {emitEvent: false});
        }
        for (const behavior of this.metadata.behavior) {
          behaviorFormGroup.addControl(behavior.id, this.fb.control(null, []), {emitEvent: false});
        }
        for (const property of this.metadata.properties) {
          if (property.disableOnProperty) {
            if (!this.validatorTriggers.includes(property.disableOnProperty)) {
              this.validatorTriggers.push(property.disableOnProperty);
            }
          }
          const validators: ValidatorFn[] = [];
          if (property.required) {
            validators.push(Validators.required);
          }
          if (property.type === ScadaSymbolPropertyType.number) {
            if (isDefinedAndNotNull(property.min)) {
              validators.push(Validators.min(property.min));
            }
            if (isDefinedAndNotNull(property.max)) {
              validators.push(Validators.max(property.max));
            }
          }
          propertiesFormGroup.addControl(property.id, this.fb.control(null, validators), {emitEvent: false});
        }
        if (this.validatorTriggers.length) {
          const observables: Observable<any>[] = [];
          for (const trigger of this.validatorTriggers) {
            if (propertiesFormGroup.get(trigger)) {
              observables.push(propertiesFormGroup.get(trigger).valueChanges);
            }
          }
          if (observables.length) {
            this.validatorSubscription = merge(...observables).subscribe(() => {
              this.updateValidators();
            });
          }
        }
        this.setupValue();
        this.cd.markForCheck();
      }
    );
  }

  private updateValidators() {
    const propertiesFormGroup =  this.scadaSymbolObjectSettingsFormGroup.get('properties') as UntypedFormGroup;
    for (const trigger of this.validatorTriggers) {
      const value: boolean = propertiesFormGroup.get(trigger).value;
      this.metadata.properties.filter(p => p.disableOnProperty === trigger).forEach(
        (p) => {
          const control = propertiesFormGroup.get(p.id);
          if (value) {
            control.enable({emitEvent: false});
          } else {
            control.disable({emitEvent: false});
          }
        }
      );
    }
  }

  private setupValue() {
    if (this.metadata) {
      const defaults = defaultScadaSymbolObjectSettings(this.metadata);
      this.modelValue = mergeDeep<ScadaSymbolObjectSettings>(defaults, this.modelValue);
      this.scadaSymbolObjectSettingsFormGroup.patchValue(
        this.modelValue, {emitEvent: false}
      );
      this.setDisabledState(this.disabled);
    }
  }

  private updateModel() {
    this.modelValue = this.scadaSymbolObjectSettingsFormGroup.getRawValue();
    this.propagateChange(this.modelValue);
  }
}
