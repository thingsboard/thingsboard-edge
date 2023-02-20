///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export const CONTAINS_TYPE = 'Contains';
export const MANAGES_TYPE = 'Manages';

export const RelationTypes = [
  CONTAINS_TYPE,
  MANAGES_TYPE
];

export enum RelationTypeGroup {
  COMMON = 'COMMON',
  ALARM = 'ALARM',
  DASHBOARD = 'DASHBOARD',
  RULE_CHAIN = 'RULE_CHAIN',
  RULE_NODE = 'RULE_NODE',
  FROM_ENTITY_GROUP = 'FROM_ENTITY_GROUP'
}

export enum EntitySearchDirection {
  FROM = 'FROM',
  TO = 'TO'
}

export const entitySearchDirectionTranslations = new Map<EntitySearchDirection, string>(
  [
    [EntitySearchDirection.FROM, 'relation.search-direction.FROM'],
    [EntitySearchDirection.TO, 'relation.search-direction.TO'],
  ]
);

export const directionTypeTranslations = new Map<EntitySearchDirection, string>(
  [
    [EntitySearchDirection.FROM, 'relation.direction-type.FROM'],
    [EntitySearchDirection.TO, 'relation.direction-type.TO'],
  ]
);

export interface RelationEntityTypeFilter {
  relationType: string;
  entityTypes: Array<EntityType>;
}

export interface RelationsSearchParameters {
  rootId: string;
  rootType: EntityType;
  direction: EntitySearchDirection;
  relationTypeGroup?: RelationTypeGroup;
  maxLevel?: number;
  fetchLastLevelOnly?: boolean;
}

export interface EntityRelationsQuery {
  parameters: RelationsSearchParameters;
  filters: Array<RelationEntityTypeFilter>;
}

export interface EntitySearchQuery {
  parameters: RelationsSearchParameters;
  relationType: string;
}

export interface EntityRelation {
  from: EntityId;
  to: EntityId;
  type: string;
  typeGroup: RelationTypeGroup;
  additionalInfo?: any;
}

export interface EntityRelationInfo extends EntityRelation {
  fromName: string;
  toEntityTypeName?: string;
  toName: string;
  fromEntityTypeName?: string;
  entityURL?: string;
}
