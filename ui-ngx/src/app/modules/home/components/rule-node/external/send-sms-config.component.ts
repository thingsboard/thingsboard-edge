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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-external-node-send-sms-config',
  templateUrl: './send-sms-config.component.html',
  styleUrls: []
})
export class SendSmsConfigComponent extends RuleNodeConfigurationComponent {

  sendSmsConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.sendSmsConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.sendSmsConfigForm = this.fb.group({
      numbersToTemplate: [configuration ? configuration.numbersToTemplate : null, [Validators.required]],
      smsMessageTemplate: [configuration ? configuration.smsMessageTemplate : null, [Validators.required]],
      useSystemSmsSettings: [configuration ? configuration.useSystemSmsSettings : false, []],
      smsProviderConfiguration: [configuration ? configuration.smsProviderConfiguration : null, []],
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemSmsSettings'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemSmsSettings: boolean = this.sendSmsConfigForm.get('useSystemSmsSettings').value;
    if (useSystemSmsSettings) {
      this.sendSmsConfigForm.get('smsProviderConfiguration').setValidators([]);
    } else {
      this.sendSmsConfigForm.get('smsProviderConfiguration').setValidators([Validators.required]);
    }
    this.sendSmsConfigForm.get('smsProviderConfiguration').updateValueAndValidity({emitEvent});
  }

}
