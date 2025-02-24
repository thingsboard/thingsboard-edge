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
import { isDefinedAndNotNull } from '@core/public-api';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { OwnerType, ownerTypeTranslations } from '@home/components/rule-node/rule-node-config.models';

@Component({
  selector: 'tb-action-node-change-owner-config',
  templateUrl: './change-owner-config.component.html',
  styleUrls: []
})
export class ChangeOwnerConfigComponent extends RuleNodeConfigurationComponent {

  changeOwnerConfigForm: UntypedFormGroup;

  ownerType = OwnerType;
  ownerTypes = Object.keys(OwnerType);
  ownerTypeTranslationsMap = ownerTypeTranslations;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.changeOwnerConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      ownerType: isDefinedAndNotNull(configuration?.ownerType) ? configuration.ownerType : OwnerType.TENANT,
      ownerNamePattern: isDefinedAndNotNull(configuration?.ownerNamePattern) ? configuration.ownerNamePattern : null,
      createOwnerIfNotExists: isDefinedAndNotNull(configuration?.createOwnerIfNotExists) ? configuration.createOwnerIfNotExists : false,
      createOwnerOnOriginatorLevel: isDefinedAndNotNull(configuration?.createOwnerOnOriginatorLevel) ? configuration.createOwnerOnOriginatorLevel : false
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.changeOwnerConfigForm = this.fb.group({
      ownerType: [configuration ? configuration.ownerType : null, [Validators.required]],
      ownerNamePattern: [configuration ? configuration.ownerNamePattern : null, []],
      createOwnerIfNotExists: [configuration ? configuration.createOwnerIfNotExists : false, []],
      createOwnerOnOriginatorLevel: [configuration ? configuration.createOwnerOnOriginatorLevel : false, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['ownerType', 'createOwnerIfNotExists'];
  }

  protected updateValidators(emitEvent: boolean) {
    const ownerType: OwnerType = this.changeOwnerConfigForm.get('ownerType').value;
    const createOwnerIfNotExists: boolean = this.changeOwnerConfigForm.get('createOwnerIfNotExists').value;
    if (ownerType === OwnerType.CUSTOMER) {
      this.changeOwnerConfigForm.get('ownerNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      this.changeOwnerConfigForm.get('ownerNamePattern').setValidators([]);
    }
    if (createOwnerIfNotExists) {
      this.changeOwnerConfigForm.get('createOwnerOnOriginatorLevel').enable({emitEvent});
    } else {
      this.changeOwnerConfigForm.get('createOwnerOnOriginatorLevel').disable({emitEvent});
    }
    this.changeOwnerConfigForm.get('ownerNamePattern').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.ownerNamePattern = configuration.ownerNamePattern ? configuration.ownerNamePattern.trim() : null;
    return configuration;
  }
}
