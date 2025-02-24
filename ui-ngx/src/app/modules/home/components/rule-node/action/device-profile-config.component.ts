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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-action-node-device-profile-config',
  templateUrl: './device-profile-config.component.html',
  styleUrls: []
})
export class DeviceProfileConfigComponent extends RuleNodeConfigurationComponent {

  deviceProfile: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.deviceProfile;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deviceProfile = this.fb.group({
      persistAlarmRulesState: [configuration ? configuration.persistAlarmRulesState : false],
      fetchAlarmRulesStateOnStart: [configuration ? configuration.fetchAlarmRulesStateOnStart : false]
    });
  }

  protected validatorTriggers(): string[] {
    return ['persistAlarmRulesState'];
  }

  protected updateValidators(emitEvent: boolean) {
    if (this.deviceProfile.get('persistAlarmRulesState').value) {
      this.deviceProfile.get('fetchAlarmRulesStateOnStart').enable({emitEvent: false});
    } else {
      this.deviceProfile.get('fetchAlarmRulesStateOnStart').setValue(false, {emitEvent: false});
      this.deviceProfile.get('fetchAlarmRulesStateOnStart').disable({emitEvent: false});
    }
    this.deviceProfile.get('fetchAlarmRulesStateOnStart').updateValueAndValidity({emitEvent});
  }

}
