///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { isNotEmptyStr, isString } from '@core/utils';
import { IntegrationType, integrationTypeInfoMap } from '@shared/models/integration.models';
import { Observable, of } from 'rxjs';
import { mergeMap, share, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';

@Component({
  selector: 'tb-integration-type-select',
  templateUrl: 'integration-type-select.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => IntegrationTypeSelectComponent),
    multi: true
  }]
})
export class IntegrationTypeSelectComponent implements ControlValueAccessor, OnInit {

  integrationTypeFormGroup: FormGroup;
  searchText = '';

  filteredIntegrationTypes: Observable<Array<string>>;

  private modelValue: IntegrationType;
  private pristine = true;

  private integrationTypesTranslation = new Map<IntegrationType, string>();
  private readonly integrationTypesList: Array<string>;

  @ViewChild('integrationTypeInput', {static: true}) integrationTypeInput: ElementRef;
  @ViewChild(MatAutocompleteTrigger) autocomplete: MatAutocompleteTrigger;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
    if (this.requiredValue) {
      this.integrationTypeFormGroup.get('type').setValidators(Validators.required);
    } else {
      this.integrationTypeFormGroup.get('type').clearValidators();
    }
    this.integrationTypeFormGroup.get('type').updateValueAndValidity({emitEvent: false});
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    this.integrationTypeFormGroup = this.fb.group({
      type: ['']
    });
    Object.values(IntegrationType)
      .map(type => this.integrationTypesTranslation.set(type, this.translate.instant(integrationTypeInfoMap.get(type).name)));
    this.integrationTypesList = Array.from(this.integrationTypesTranslation.values());
  }

  ngOnInit() {
    this.filteredIntegrationTypes = this.integrationTypeFormGroup.get('type').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (!isString(value) || !this.integrationTypesList.includes(value)) {
            modelValue = null;
          } else {
            modelValue = Array.from(this.integrationTypesTranslation.keys())
              .find(key => this.integrationTypesTranslation.get(key) === value);
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        mergeMap(name => this.fetchIntegrationTypes(name)),
        share()
      );
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.integrationTypeFormGroup.disable({emitEvent: false});
    } else {
      this.integrationTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: IntegrationType) {
    this.searchText = '';
    let integrationType = null;
    if (value != null && this.integrationTypesTranslation.has(value)) {
      integrationType = value;
    }
    if (integrationType) {
      this.modelValue = integrationType;
      this.integrationTypeFormGroup.get('type').patchValue(this.integrationTypesTranslation.get(integrationType), {emitEvent: false});
    } else {
      this.modelValue = null;
      this.integrationTypeFormGroup.get('type').patchValue('', {emitEvent: false});
    }
    this.pristine = true;
  }

  onFocus() {
    if (this.pristine) {
      this.integrationTypeFormGroup.get('type').updateValueAndValidity({onlySelf: true});
      this.pristine = false;
    }
  }

  selectedType() {
    if (isNotEmptyStr(this.searchText)) {
      const result = this.filterIntegrationType(this.searchText);
      if (result.length === 1) {
        this.integrationTypeFormGroup.get('type').patchValue(result[0]);
        this.autocomplete.closePanel();
      }
    }
  }

  updateView(value: IntegrationType | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  clear() {
    this.integrationTypeFormGroup.get('type').patchValue('');
    setTimeout(() => {
      this.integrationTypeInput.nativeElement.blur();
      this.integrationTypeInput.nativeElement.focus();
    }, 0);
  }

  private fetchIntegrationTypes(searchText?: string): Observable<Array<string>> {
    this.searchText = searchText;
    let result = this.integrationTypesList;
    if (isNotEmptyStr(searchText)) {
      result = this.filterIntegrationType(searchText);
    }
    return of(result);
  }

  private filterIntegrationType(searchText: string): Array<string> {
    const regex = new RegExp(searchText, 'i');
    return this.integrationTypesList.filter((integrationType) => regex.test(integrationType));
  }
}
