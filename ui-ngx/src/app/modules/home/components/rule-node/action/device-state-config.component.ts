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

import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  MessageType,
  messageTypeNames,
  RuleNodeConfiguration,
  RuleNodeConfigurationComponent
} from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-device-state-config',
    templateUrl: './device-state-config.component.html',
    styleUrls: []
})
export class DeviceStateConfigComponent extends RuleNodeConfigurationComponent {

    deviceState: FormGroup;

    public messageTypeNames = messageTypeNames;
    public eventOptions: MessageType[] = [
        MessageType.CONNECT_EVENT,
        MessageType.ACTIVITY_EVENT,
        MessageType.DISCONNECT_EVENT,
        MessageType.INACTIVITY_EVENT
    ];

    constructor(private fb: FormBuilder) {
        super();
    }

    protected configForm(): FormGroup {
        return this.deviceState;
    }

    protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
        return {
            event: isDefinedAndNotNull(configuration?.event) ? configuration.event : MessageType.ACTIVITY_EVENT
        };
    }

    protected onConfigurationSet(configuration: RuleNodeConfiguration) {
        this.deviceState = this.fb.group({
            event: [configuration.event, [Validators.required]]
        });
    }

}
