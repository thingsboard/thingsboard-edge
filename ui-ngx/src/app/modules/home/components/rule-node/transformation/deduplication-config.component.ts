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
import { deduplicationStrategiesTranslations, FetchMode } from '@home/components/rule-node/rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-transformation-node-deduplication-config',
  templateUrl: './deduplication-config.component.html',
  styleUrls: []
})

export class DeduplicationConfigComponent extends RuleNodeConfigurationComponent {

  deduplicationConfigForm: FormGroup;
  deduplicationStrategie = FetchMode;
  deduplicationStrategies = Object.keys(this.deduplicationStrategie);
  deduplicationStrategiesTranslations = deduplicationStrategiesTranslations;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.deduplicationConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deduplicationConfigForm = this.fb.group({
      interval: [isDefinedAndNotNull(configuration?.interval) ? configuration.interval : null, [Validators.required,
        Validators.min(1)]],
      strategy: [isDefinedAndNotNull(configuration?.strategy) ? configuration.strategy : null, [Validators.required]],
      outMsgType: [isDefinedAndNotNull(configuration?.outMsgType) ? configuration.outMsgType : null, [Validators.required]],
      maxPendingMsgs: [isDefinedAndNotNull(configuration?.maxPendingMsgs) ? configuration.maxPendingMsgs : null, [Validators.required,
        Validators.min(1), Validators.max(1000)]],
      maxRetries: [isDefinedAndNotNull(configuration?.maxRetries) ? configuration.maxRetries : null,
        [Validators.required, Validators.min(0), Validators.max(100)]]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    if (!configuration) {
      configuration = {};
    }
    if (!configuration.outMsgType) {
      configuration.outMsgType = 'POST_TELEMETRY_REQUEST';
    }
    return super.prepareInputConfig(configuration);
  }

  protected updateValidators(emitEvent: boolean) {
    if (this.deduplicationConfigForm.get('strategy').value === this.deduplicationStrategie.ALL) {
      this.deduplicationConfigForm.get('outMsgType').enable({emitEvent: false});
    } else {
      this.deduplicationConfigForm.get('outMsgType').disable({emitEvent: false});
    }
    this.deduplicationConfigForm.get('outMsgType').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['strategy'];
  }
}
