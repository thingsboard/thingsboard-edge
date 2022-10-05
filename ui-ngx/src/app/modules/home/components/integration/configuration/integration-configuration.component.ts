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

import { ChangeDetectorRef, Component, forwardRef, Input, OnDestroy, TemplateRef } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { IntegrationType } from '@shared/models/integration.models';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-integration-configuration-new',
  templateUrl: './integration-configuration.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => IntegrationConfigurationComponent),
    multi: true
  }]
})
export class IntegrationConfigurationComponent implements ControlValueAccessor, OnDestroy {

  integrationConfigurationForm: FormGroup;
  integrationTypes = IntegrationType;

  @Input() executeRemotelyTemplate: TemplateRef<any>;
  @Input() genericAdditionalInfoTemplate: TemplateRef<any>;

  private integrationTypeValue: IntegrationType;
  @Input()
  set integrationType(value: IntegrationType) {
    this.integrationTypeValue = value || IntegrationType.CUSTOM;
    // this.integrationConfigurationForm.setControl('configuration', this.fb.control(null));
  }

  get integrationType(): IntegrationType {
    return this.integrationTypeValue;
  }

  @Input()
  disabled: boolean;

  private destroy$ = new Subject();
  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private cd: ChangeDetectorRef) {
    this.integrationConfigurationForm = this.fb.group({
      configuration: [null, Validators.required],
      test: ['']
    });
    this.integrationConfigurationForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => this.updateModel(value.configuration));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) { }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.integrationConfigurationForm.disable({emitEvent: false});
    } else {
      this.integrationConfigurationForm.enable({emitEvent: false});
    }
  }

  writeValue(value: any) {
    this.integrationConfigurationForm.get('configuration').patchValue(value, {emitEvents: false});
  }

  private updateModel(value: any) {
    this.propagateChange(value);
    this.integrationConfigurationForm.updateValueAndValidity({emitEvent: false});
    this.cd.markForCheck();
  }
}
