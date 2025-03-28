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
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { TimeUnit, timeUnitTranslations } from '../rule-node-config.models';
import { EntityType } from '@app/shared/models/entity-type.models';

@Component({
  selector: 'tb-analytics-node-alarms-count-v2-config',
  templateUrl: './alarms-count-v2-config.component.html',
  styleUrls: ['./alarms-count-v2-config.component.scss']
})
export class AlarmsCountV2ConfigComponent extends RuleNodeConfigurationComponent {

  alarmsCountConfigForm: UntypedFormGroup;

  timeUnits = Object.keys(TimeUnit);
  timeUnitsTranslationMap = timeUnitTranslations;

  propagationEntityTypes: EntityType[] = [
    EntityType.DEVICE,
    EntityType.ASSET,
    EntityType.ENTITY_VIEW,
    EntityType.TENANT,
    EntityType.CUSTOMER,
    EntityType.USER,
    EntityType.DASHBOARD,
    EntityType.RULE_CHAIN,
    EntityType.RULE_NODE,
    EntityType.ENTITY_GROUP,
    EntityType.CONVERTER,
    EntityType.INTEGRATION,
    EntityType.SCHEDULER_EVENT,
    EntityType.BLOB_ENTITY
  ];

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.alarmsCountConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.alarmsCountConfigForm = this.fb.group({
      alarmsCountMappings: [configuration ? configuration.alarmsCountMappings : null, [Validators.required]],
      countAlarmsForPropagationEntities: [configuration ? configuration.alarmsCountMappings : true, [Validators.required]],
      propagationEntityTypes: [configuration && configuration.propagationEntityTypes ? configuration.propagationEntityTypes : [], []],
      outMsgType: [configuration ? configuration.outMsgType : null, [Validators.required]]
    });
  }
}
