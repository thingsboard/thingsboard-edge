///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { UtilsService } from '@core/services/utils.service';
import { isDefinedAndNotNull } from '@core/utils';
import { Subscription } from 'rxjs';
import {
  NotificationDeliveryMethod,
  NotificationTemplateTypeTranslateMap,
  NotificationUserSetting
} from '@shared/models/notification.models';

@Component({
  selector: 'tb-notification-setting-form',
  templateUrl: './notification-setting-form.component.html',
  styleUrls: ['./notification-setting-form.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NotificationSettingFormComponent),
      multi: true
    }
  ]
})
export class NotificationSettingFormComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input()
  disabled: boolean;

  @Input()
  deliveryMethods: NotificationDeliveryMethod[] = [];

  @Input()
  allowDeliveryMethods: NotificationDeliveryMethod[] = [];

  notificationSettingsFormGroup: UntypedFormGroup;

  notificationTemplateTypeTranslateMap = NotificationTemplateTypeTranslateMap;

  private propagateChange = null;

  private valueChange$: Subscription = null;

  constructor(private utils: UtilsService,
              private fb: UntypedFormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    const deliveryMethod = {};
    this.deliveryMethods.forEach(value => {
      deliveryMethod[value] = true;
    });
    this.notificationSettingsFormGroup = this.fb.group(
      {
        name: [''],
        enabled: [true],
        enabledDeliveryMethods: this.fb.group({
          ...deliveryMethod
        })
      });
    this.valueChange$ = this.notificationSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
      this.valueChange$ = null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.notificationSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.notificationSettingsFormGroup.enable({emitEvent: false});
    }
  }

  toggleEnabled() {
    this.notificationSettingsFormGroup.get('enabled').patchValue(!this.notificationSettingsFormGroup.get('enabled').value);
  }

  getChecked(deliveryMethod: NotificationDeliveryMethod): boolean {
    return this.notificationSettingsFormGroup.get('enabledDeliveryMethods').get(deliveryMethod).value;
  }

  toggleDeliviryMethod(deliveryMethod: NotificationDeliveryMethod) {
    this.notificationSettingsFormGroup.get('enabledDeliveryMethods').get(deliveryMethod)
      .patchValue(!this.notificationSettingsFormGroup.get('enabledDeliveryMethods').get(deliveryMethod).value);
  }

  writeValue(value: NotificationUserSetting): void {
    if (isDefinedAndNotNull(value)) {
      this.notificationSettingsFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateModel() {
      this.propagateChange(this.notificationSettingsFormGroup.value);
  }
}
