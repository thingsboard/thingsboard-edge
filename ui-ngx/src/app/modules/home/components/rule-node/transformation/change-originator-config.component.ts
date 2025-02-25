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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  OriginatorSource,
  originatorSourceDescTranslations,
  originatorSourceTranslations
} from '@home/components/rule-node/rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { EntityType } from '@app/shared/models/entity-type.models';

@Component({
  selector: 'tb-transformation-node-change-originator-config',
  templateUrl: './change-originator-config.component.html'
})
export class ChangeOriginatorConfigComponent extends RuleNodeConfigurationComponent {

  originatorSource = OriginatorSource;
  originatorSources = Object.keys(OriginatorSource) as OriginatorSource[];
  originatorSourceTranslationMap = originatorSourceTranslations;
  originatorSourceDescTranslationMap = originatorSourceDescTranslations;

  changeOriginatorConfigForm: FormGroup;

  allowedEntityTypes = [EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.USER, EntityType.EDGE];

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.changeOriginatorConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.changeOriginatorConfigForm = this.fb.group({
      originatorSource: [configuration ? configuration.originatorSource : null, [Validators.required]],
      preserveOriginatorIfCustomer: [configuration ? configuration?.preserveOriginatorIfCustomer : false, []],
      entityType: [configuration ? configuration.entityType : null, []],
      entityNamePattern: [configuration ? configuration.entityNamePattern : null, []],
      relationsQuery: [configuration ? configuration.relationsQuery : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['originatorSource'];
  }

  protected updateValidators(emitEvent: boolean) {
    const originatorSource: OriginatorSource = this.changeOriginatorConfigForm.get('originatorSource').value;
    if (originatorSource === OriginatorSource.RELATED) {
      this.changeOriginatorConfigForm.get('relationsQuery').setValidators([Validators.required]);
    } else {
      this.changeOriginatorConfigForm.get('relationsQuery').setValidators([]);
    }
    if (originatorSource === OriginatorSource.ENTITY) {
      this.changeOriginatorConfigForm.get('entityType').setValidators([Validators.required]);
      this.changeOriginatorConfigForm.get('entityNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      this.changeOriginatorConfigForm.get('entityType').patchValue(null, {emitEvent});
      this.changeOriginatorConfigForm.get('entityNamePattern').patchValue(null, {emitEvent});
      this.changeOriginatorConfigForm.get('entityType').setValidators([]);
      this.changeOriginatorConfigForm.get('entityNamePattern').setValidators([]);
    }
    this.changeOriginatorConfigForm.get('relationsQuery').updateValueAndValidity({emitEvent});
    this.changeOriginatorConfigForm.get('entityType').updateValueAndValidity({emitEvent});
    this.changeOriginatorConfigForm.get('entityNamePattern').updateValueAndValidity({emitEvent});
  }
}
