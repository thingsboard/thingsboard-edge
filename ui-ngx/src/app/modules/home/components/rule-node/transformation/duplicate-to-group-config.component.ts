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
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { allowedEntityGroupTypes } from '@home/components/rule-node/rule-node-config.models';

@Component({
  selector: 'tb-transformation-node-duplicate-to-group-config',
  templateUrl: './duplicate-to-group-config.component.html',
  styleUrls: ['./duplicate-to-group-config.component.scss']
})

export class DuplicateToGroupConfigComponent extends RuleNodeConfigurationComponent {

  isTypeSelected = false;
  duplicateToGroupConfigForm: FormGroup;

  entityGroupTypes = allowedEntityGroupTypes;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.duplicateToGroupConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.duplicateToGroupConfigForm = this.fb.group({
      entityGroupIsMessageOriginator: [configuration ? configuration.entityGroupIsMessageOriginator : false, []],
      entityGroupId: [configuration ? configuration.entityGroupId : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['entityGroupIsMessageOriginator'];
  }

  protected updateValidators(emitEvent: boolean) {
    const entityGroupIsMessageOriginator: boolean = this.duplicateToGroupConfigForm.get('entityGroupIsMessageOriginator').value;
    const entityGroupId: string = this.duplicateToGroupConfigForm.get('entityGroupId').value;
    if (emitEvent) {
      if (entityGroupIsMessageOriginator && entityGroupId) {
        this.duplicateToGroupConfigForm.get('entityGroupId').reset(null, {emitEvent: false});
      }
    }
    if (entityGroupIsMessageOriginator) {
      this.duplicateToGroupConfigForm.get('entityGroupId').setValidators([]);
    } else {
      this.duplicateToGroupConfigForm.get('entityGroupId').setValidators([Validators.required]);
    }
    this.duplicateToGroupConfigForm.get('entityGroupId').updateValueAndValidity({emitEvent});
  }

}
