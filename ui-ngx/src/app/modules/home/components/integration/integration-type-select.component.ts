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
import { IntegrationType, IntegrationTypeInfo, integrationTypeInfoMap } from '@shared/models/integration.models';
import { Observable, of } from 'rxjs';
import { distinctUntilChanged, map, mergeMap, share, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';

type IntegrationInfo = IntegrationTypeInfo & {type: IntegrationType};

@Component({
  selector: 'tb-integration-type-select',
  templateUrl: 'integration-type-select.component.html',
  styleUrls: ['integration-type-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => IntegrationTypeSelectComponent),
    multi: true
  }]
})
export class IntegrationTypeSelectComponent implements ControlValueAccessor, OnInit {

  integrationTypeFormGroup: FormGroup;
  searchText = '';

  filteredIntegrationTypes: Observable<Array<IntegrationInfo>>;
  modelValue: IntegrationInfo;

  private pristine = true;

  private integrationTypesInfo: Array<IntegrationInfo> = [];

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
    Object.values(IntegrationType).forEach(integrationType => {
      const integration = integrationTypeInfoMap.get(integrationType);
      this.integrationTypesInfo.push({
        type: integrationType,
        ...integration,
        name: this.translate.instant(integration.name),
        description: integration.description ? this.translate.instant(integration.description) : ''
      });
    });
  }

  ngOnInit() {
    this.filteredIntegrationTypes = this.integrationTypeFormGroup.get('type').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (isString(value) || !value) {
            modelValue = null;
          } else {
            modelValue = this.integrationTypesInfo.find(info => info.type === value.type);
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (isString(value) ? value.trim() : value.type) : ''),
        distinctUntilChanged(),
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
    const integrationType = value != null && this.integrationTypesInfo.find(integration => integration.type === value);
    if (integrationType) {
      this.modelValue = integrationType;
      this.integrationTypeFormGroup.get('type').patchValue(this.modelValue, {emitEvent: false});
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

  clear() {
    this.integrationTypeFormGroup.get('type').patchValue('');
    setTimeout(() => {
      this.integrationTypeInput.nativeElement.blur();
      this.integrationTypeInput.nativeElement.focus();
    }, 0);
  }

  displayIntegrationTypeFn(inegration?: IntegrationInfo): string | undefined {
    return inegration ? inegration.name : undefined;
  }

  private updateView(value: IntegrationInfo | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue?.type || null);
    }
  }

  private fetchIntegrationTypes(searchText?: string): Observable<Array<IntegrationInfo>> {
    this.searchText = searchText;
    let result = this.integrationTypesInfo;
    if (isNotEmptyStr(searchText)) {
      result = this.filterIntegrationType(searchText);
    }
    return of(result);
  }

  private filterIntegrationType(searchText: string): Array<IntegrationInfo> {
    const regex = new RegExp(searchText, 'i');
    return this.integrationTypesInfo.filter((integrationInfo) =>
      regex.test(integrationInfo.name) || regex.test(integrationInfo.description) ||
      searchText === integrationInfo.type || regex.test(integrationInfo.tags?.toString()));
  }
}
