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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { deepTrim, isDefinedAndNotNull } from '@app/core/utils';

@Component({
  selector: 'tb-enrichment-node-calculate-delta-config',
  templateUrl: './calculate-delta-config.component.html'
})
export class CalculateDeltaConfigComponent extends RuleNodeConfigurationComponent {

  calculateDeltaConfigForm: FormGroup;

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  constructor(public translate: TranslateService,
              private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.calculateDeltaConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.calculateDeltaConfigForm = this.fb.group({
      inputValueKey: [configuration.inputValueKey, [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      outputValueKey: [configuration.outputValueKey, [Validators.required, Validators.pattern(/(?:.|\s)*\S(&:.|\s)*/)]],
      useCache: [configuration.useCache, []],
      addPeriodBetweenMsgs: [configuration.addPeriodBetweenMsgs, []],
      periodValueKey: [configuration.periodValueKey, []],
      round: [configuration.round, [Validators.min(0), Validators.max(15)]],
      tellFailureIfDeltaIsNegative: [configuration.tellFailureIfDeltaIsNegative, []],
      excludeZeroDeltas: [configuration.excludeZeroDeltas, []]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      inputValueKey: isDefinedAndNotNull(configuration?.inputValueKey) ? configuration.inputValueKey : null,
      outputValueKey: isDefinedAndNotNull(configuration?.outputValueKey) ? configuration.outputValueKey : null,
      useCache: isDefinedAndNotNull(configuration?.useCache) ? configuration.useCache : true,
      addPeriodBetweenMsgs: isDefinedAndNotNull(configuration?.addPeriodBetweenMsgs) ? configuration.addPeriodBetweenMsgs : false,
      periodValueKey: isDefinedAndNotNull(configuration?.periodValueKey) ? configuration.periodValueKey : null,
      round: isDefinedAndNotNull(configuration?.round) ? configuration.round : null,
      tellFailureIfDeltaIsNegative: isDefinedAndNotNull(configuration?.tellFailureIfDeltaIsNegative) ?
        configuration.tellFailureIfDeltaIsNegative : true,
      excludeZeroDeltas: isDefinedAndNotNull(configuration?.excludeZeroDeltas) ? configuration.excludeZeroDeltas : false
    };
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return deepTrim(configuration);
  }

  protected updateValidators(emitEvent: boolean) {
    const addPeriodBetweenMsgs: boolean = this.calculateDeltaConfigForm.get('addPeriodBetweenMsgs').value;
    if (addPeriodBetweenMsgs) {
      this.calculateDeltaConfigForm.get('periodValueKey').setValidators([Validators.required]);
    } else {
      this.calculateDeltaConfigForm.get('periodValueKey').setValidators([]);
    }
    this.calculateDeltaConfigForm.get('periodValueKey').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['addPeriodBetweenMsgs'];
  }
}
