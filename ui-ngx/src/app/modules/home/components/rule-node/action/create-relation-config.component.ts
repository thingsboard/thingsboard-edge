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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';
import { EntitySearchDirection } from '@app/shared/models/relation.models';
import { EntityType } from '@app/shared/models/entity-type.models';

@Component({
  selector: 'tb-action-node-create-relation-config',
  templateUrl: './create-relation-config.component.html',
  styleUrls: []
})
export class CreateRelationConfigComponent extends RuleNodeConfigurationComponent {

  directionTypes = Object.keys(EntitySearchDirection);
  directionTypeTranslations  = new Map<EntitySearchDirection, string>(
    [
      [EntitySearchDirection.FROM, 'rule-node-config.search-direction-from'],
      [EntitySearchDirection.TO, 'rule-node-config.search-direction-to'],
    ]
  );

  entityType = EntityType;

  entityTypeNamePatternTranslation = new Map<EntityType, string>(
    [
      [EntityType.DEVICE, 'rule-node-config.device-name-pattern'],
      [EntityType.ASSET, 'rule-node-config.asset-name-pattern'],
      [EntityType.ENTITY_VIEW, 'rule-node-config.entity-view-name-pattern'],
      [EntityType.CUSTOMER, 'rule-node-config.customer-title-pattern'],
      [EntityType.USER, 'rule-node-config.user-name-pattern'],
      [EntityType.DASHBOARD, 'rule-node-config.dashboard-name-pattern'],
      [EntityType.EDGE, 'rule-node-config.edge-name-pattern']
    ]
  );

  allowedEntityTypes = [EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.TENANT,
    EntityType.CUSTOMER, EntityType.USER, EntityType.DASHBOARD, EntityType.EDGE];

  createRelationConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.createRelationConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.createRelationConfigForm = this.fb.group({
      direction: [configuration ? configuration.direction : null, [Validators.required]],
      entityType: [configuration ? configuration.entityType : null, [Validators.required]],
      entityNamePattern: [configuration ? configuration.entityNamePattern : null, []],
      entityTypePattern: [configuration ? configuration.entityTypePattern : null, []],
      relationType: [configuration ? configuration.relationType : null, [Validators.required]],
      createEntityIfNotExists: [configuration ? configuration.createEntityIfNotExists : false, []],
      removeCurrentRelations: [configuration ? configuration.removeCurrentRelations : false, []],
      changeOriginatorToRelatedEntity: [configuration ? configuration.changeOriginatorToRelatedEntity : false, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['entityType', 'createEntityIfNotExists'];
  }

  protected updateValidators(emitEvent: boolean) {
    const entityType: EntityType = this.createRelationConfigForm.get('entityType').value;
    if (entityType) {
      this.createRelationConfigForm.get('entityNamePattern').setValidators([Validators.required, Validators.pattern(/.*\S.*/)]);
    } else {
      this.createRelationConfigForm.get('entityNamePattern').setValidators([]);
    }
    if (entityType && (entityType === EntityType.DEVICE || entityType === EntityType.ASSET)) {
      const validators = [Validators.pattern(/.*\S.*/)]
      if (this.createRelationConfigForm.get('createEntityIfNotExists').value) {
        validators.push(Validators.required);
      }
      this.createRelationConfigForm.get('entityTypePattern').setValidators(validators);
    } else {
      this.createRelationConfigForm.get('entityTypePattern').setValidators([]);
    }
    this.createRelationConfigForm.get('entityNamePattern').updateValueAndValidity({emitEvent});
    this.createRelationConfigForm.get('entityTypePattern').updateValueAndValidity({emitEvent});
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.entityNamePattern = configuration.entityNamePattern ? configuration.entityNamePattern.trim() : null;
    configuration.entityTypePattern = configuration.entityTypePattern ? configuration.entityTypePattern.trim() : null;
    return configuration;
  }
}
