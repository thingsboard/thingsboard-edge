///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
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
import { JsFuncModulesComponent } from '@shared/components/js-func-modules.component';
import { ResourceSubType } from '@shared/models/resource.models';
import { Observable } from 'rxjs';
import { ResourceAutocompleteComponent } from '@shared/components/resource/resource-autocomplete.component';
import { HttpClient } from '@angular/common/http';
import { loadModuleMarkdownDescription, loadModuleMarkdownSourceCode } from '@shared/models/js-function.models';
import { TranslateService } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface JsFuncModuleRow {
  alias: string;
  moduleLink: string;
}

export const moduleValid = (module: JsFuncModuleRow): boolean => !(!module.alias || !module.moduleLink);

@Component({
  selector: 'tb-js-func-module-row',
  templateUrl: './js-func-module-row.component.html',
  styleUrls: ['./js-func-module-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsFuncModuleRowComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => JsFuncModuleRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class JsFuncModuleRowComponent implements ControlValueAccessor, OnInit, Validator {

  ResourceSubType = ResourceSubType;

  @ViewChild('resourceAutocomplete')
  resourceAutocomplete: ResourceAutocompleteComponent;

  @Input()
  index: number;

  @Output()
  moduleRemoved = new EventEmitter();

  moduleRowFormGroup: UntypedFormGroup;

  modelValue: JsFuncModuleRow;

  moduleDescription = this.loadModuleDescription.bind(this);

  moduleSourceCode = this.loadModuleSourceCode.bind(this);

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private modulesComponent: JsFuncModulesComponent,
              private http: HttpClient,
              private translate: TranslateService,
              private destroyRef: DestroyRef) {}

  ngOnInit() {
    this.moduleRowFormGroup = this.fb.group({
      alias: [null, [this.moduleAliasValidator(), Validators.pattern(/^[$_\p{ID_Start}][$\p{ID_Continue}]*$/u)]],
      moduleLink: [null, [Validators.required]]
    });
    this.moduleRowFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => this.updateModel()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  writeValue(value: JsFuncModuleRow): void {
    this.modelValue = value;
    this.moduleRowFormGroup.patchValue(
      {
        alias: value?.alias,
        moduleLink: value?.moduleLink
      }, {emitEvent: false}
    );
    this.cd.markForCheck();
  }

  public validate(_c: UntypedFormControl) {
    const aliasControl = this.moduleRowFormGroup.get('alias');
    if (aliasControl.hasError('moduleAliasNotUnique') || aliasControl.hasError('pattern')) {
      aliasControl.updateValueAndValidity({onlySelf: false, emitEvent: false});
    }
    if (aliasControl.hasError('moduleAliasNotUnique')) {
      this.moduleRowFormGroup.get('alias').markAsTouched();
      return {
        moduleAliasNotUnique: true
      };
    }
    if (aliasControl.hasError('pattern')) {
      this.moduleRowFormGroup.get('alias').markAsTouched();
      return {
        invalidVariableName: true
      };
    }
    const module: JsFuncModuleRow = {...this.modelValue, ...this.moduleRowFormGroup.value};
    if (!moduleValid(module)) {
      return {
        module: true
      };
    }
    return null;
  }

  private loadModuleDescription(): Observable<string> | null {
    const moduleLink = this.moduleRowFormGroup.get('moduleLink').value;
    if (moduleLink) {
      const resource = this.resourceAutocomplete.resource;
      return loadModuleMarkdownDescription(this.http, this.translate, resource);
    } else {
      return null;
    }
  }

  private loadModuleSourceCode(): Observable<string> | null {
    const moduleLink = this.moduleRowFormGroup.get('moduleLink').value;
    if (moduleLink) {
      const resource = this.resourceAutocomplete.resource;
      return loadModuleMarkdownSourceCode(this.http, this.translate, resource);
    } else {
      return null;
    }
  }

  private moduleAliasValidator(): ValidatorFn {
    return control => {
      if (!control.value) {
        return {
          required: true
        };
      }
      if (!this.modulesComponent.moduleAliasUnique(control.value, this.index)) {
        return {
          moduleAliasNotUnique: true
        };
      }
      return null;
    };
  }

  private updateModel() {
    const value: JsFuncModuleRow = this.moduleRowFormGroup.value;
    this.modelValue = {...this.modelValue, ...value};
    this.propagateChange(this.modelValue);
  }

}
