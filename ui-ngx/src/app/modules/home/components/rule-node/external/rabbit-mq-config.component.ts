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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-external-node-rabbit-mq-config',
  templateUrl: './rabbit-mq-config.component.html',
  styleUrls: []
})
export class RabbitMqConfigComponent extends RuleNodeConfigurationComponent {

  rabbitMqConfigForm: UntypedFormGroup;

  messageProperties: string[] = [
    null,
    'BASIC',
    'TEXT_PLAIN',
    'MINIMAL_BASIC',
    'MINIMAL_PERSISTENT_BASIC',
    'PERSISTENT_BASIC',
    'PERSISTENT_TEXT_PLAIN'
  ];

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.rabbitMqConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.rabbitMqConfigForm = this.fb.group({
      exchangeNamePattern: [configuration ? configuration.exchangeNamePattern : null, []],
      routingKeyPattern: [configuration ? configuration.routingKeyPattern : null, []],
      messageProperties: [configuration ? configuration.messageProperties : null, []],
      host: [configuration ? configuration.host : null, [Validators.required]],
      port: [configuration ? configuration.port : null, [Validators.required, Validators.min(1), Validators.max(65535)]],
      virtualHost: [configuration ? configuration.virtualHost : null, []],
      username: [configuration ? configuration.username : null, []],
      password: [configuration ? configuration.password : null, []],
      automaticRecoveryEnabled: [configuration ? configuration.automaticRecoveryEnabled : false, []],
      connectionTimeout: [configuration ? configuration.connectionTimeout : null, [Validators.min(0)]],
      handshakeTimeout: [configuration ? configuration.handshakeTimeout : null, [Validators.min(0)]],
      clientProperties: [configuration ? configuration.clientProperties : null, []]
    });
  }
}
