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
import { isDefinedAndNotNull, isObject } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { FetchTo } from '@home/components/rule-node/rule-node-config.models';

@Component({
  selector: 'tb-enrichment-node-device-attributes-config',
  templateUrl: './device-attributes-config.component.html',
  styleUrls: []
})
export class DeviceAttributesConfigComponent extends RuleNodeConfigurationComponent {

  deviceAttributesConfigForm: FormGroup;

  constructor(public translate: TranslateService,
              private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.deviceAttributesConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deviceAttributesConfigForm = this.fb.group({
      deviceRelationsQuery: [configuration.deviceRelationsQuery, [Validators.required]],
      tellFailureIfAbsent: [configuration.tellFailureIfAbsent, []],
      fetchTo: [configuration.fetchTo, []],
      attributesControl: [configuration.attributesControl, []]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    if (isObject(configuration)) {
      configuration.attributesControl = {
        clientAttributeNames: isDefinedAndNotNull(configuration?.clientAttributeNames) ? configuration.clientAttributeNames : [],
        latestTsKeyNames: isDefinedAndNotNull(configuration?.latestTsKeyNames) ? configuration.latestTsKeyNames : [],
        serverAttributeNames: isDefinedAndNotNull(configuration?.serverAttributeNames) ? configuration.serverAttributeNames : [],
        sharedAttributeNames: isDefinedAndNotNull(configuration?.sharedAttributeNames) ? configuration.sharedAttributeNames : [],
        getLatestValueWithTs: isDefinedAndNotNull(configuration?.getLatestValueWithTs) ? configuration.getLatestValueWithTs : false,
      };
    }

    return {
      deviceRelationsQuery: isDefinedAndNotNull(configuration?.deviceRelationsQuery) ? configuration.deviceRelationsQuery : null,
      tellFailureIfAbsent: isDefinedAndNotNull(configuration?.tellFailureIfAbsent) ? configuration.tellFailureIfAbsent : true,
      fetchTo: isDefinedAndNotNull(configuration?.fetchTo) ? configuration.fetchTo : FetchTo.METADATA,
      attributesControl: configuration ? configuration.attributesControl : null
    };
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    for (const key of Object.keys(configuration.attributesControl)) {
      configuration[key] = configuration.attributesControl[key];
    }
    delete configuration.attributesControl;
    return configuration;
  }
}
