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
import { deepTrim, isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { allowedOriginatorFields, FetchTo, SvMapOption } from '@home/components/rule-node/rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
  selector: 'tb-enrichment-node-originator-fields-config',
  templateUrl: './originator-fields-config.component.html'
})
export class OriginatorFieldsConfigComponent extends RuleNodeConfigurationComponent {

  originatorFieldsConfigForm: FormGroup;
  public originatorFields: SvMapOption[] = [];

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {
    super();
    for (const field of allowedOriginatorFields) {
      this.originatorFields.push({
        value: field.value,
        name: this.translate.instant(field.name)
      });
    }
  }

  protected configForm(): FormGroup {
    return this.originatorFieldsConfigForm;
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return deepTrim(configuration);
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      dataMapping: isDefinedAndNotNull(configuration?.dataMapping) ? configuration.dataMapping : null,
      ignoreNullStrings: isDefinedAndNotNull(configuration?.ignoreNullStrings) ? configuration.ignoreNullStrings : null,
      fetchTo: isDefinedAndNotNull(configuration?.fetchTo) ? configuration.fetchTo : FetchTo.METADATA
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.originatorFieldsConfigForm = this.fb.group({
      dataMapping: [configuration.dataMapping, [Validators.required]],
      ignoreNullStrings: [configuration.ignoreNullStrings, []],
      fetchTo: [configuration.fetchTo, []]
    });
  }
}
