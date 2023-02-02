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

import { NavTreeNode } from '@shared/components/nav-tree.component';
import { Datasource } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { BaseData, HasId } from '@shared/models/base-data';

export interface EntityNodeDatasource extends Datasource {
  nodeId: string;
}

export interface EdgeOverviewNode extends NavTreeNode {
  data?: BaseEdgeNodeData;
}

export interface BaseEdgeNodeData {
  type: EdgeNodeType;
  group: BaseData<HasId>;
  groupType: EntityType;
}

export type EdgeNodeType = 'group' | 'groups';

export interface EntityGroupNodeData extends BaseEdgeNodeData {
  type: 'group';
}

export interface EntityGroupsNodeData extends BaseEdgeNodeData {
  type: 'groups';
  edge?: BaseData<HasId>;
}

export function edgeGroupsNodeText(translate: TranslateService, entityType: EntityType): string {
  const nodeIcon = materialIconByEntityType(entityType);
  const nodeText = textForEntityGroupsType(translate, entityType);
  return nodeIcon + nodeText;
}

export function entityGroupNodeText(entity: any): string {
  const nodeIcon = materialIconByEntityType(entity.type);
  const nodeText = entity.name;
  return nodeIcon + nodeText;
}

export function entityNodeText(entity: any): string {
  const nodeIcon = materialIconByEntityType(entity.id.entityType);
  const nodeText = entity.name;
  return nodeIcon + nodeText;
}

export function materialIconByEntityType(entityType: EntityType): string {
  let materialIcon = 'insert_drive_file';
  switch (entityType) {
    case EntityType.DEVICE:
      materialIcon = 'devices_other';
      break;
    case EntityType.ASSET:
      materialIcon = 'domain';
      break;
    case EntityType.USER:
      materialIcon = 'account_circle';
      break;
    case EntityType.DASHBOARD:
      materialIcon = 'dashboards';
      break;
    case EntityType.ENTITY_VIEW:
      materialIcon = 'view_quilt';
      break;
    case EntityType.SCHEDULER_EVENT:
      materialIcon = 'schedule';
      break;
    case EntityType.RULE_CHAIN:
      materialIcon = 'settings_ethernet';
      break;
    case EntityType.INTEGRATION:
      materialIcon = 'input';
      break;
  }
  return '<mat-icon class="node-icon material-icons" role="img" aria-hidden="false">' + materialIcon + '</mat-icon>';
}

export function textForEntityGroupsType(translate: TranslateService, entityType: EntityType): string {
  let textForEntityGroupsType: string = '';
  switch (entityType) {
    case EntityType.USER:
      return translate.instant('entity-group.user-groups');
    case EntityType.ASSET:
      return translate.instant('entity-group.asset-groups');
    case EntityType.DEVICE:
      return translate.instant('entity-group.device-groups');
    case EntityType.ENTITY_VIEW:
      return translate.instant('entity-group.entity-view-groups');
    case EntityType.DASHBOARD:
      return translate.instant('entity-group.dashboard-groups');
    case EntityType.SCHEDULER_EVENT:
      return translate.instant('entity.type-scheduler-events');
    case EntityType.RULE_CHAIN:
      return translate.instant('entity.type-rulechains');
    case EntityType.INTEGRATION:
      return translate.instant('entity.type-integrations');
  }
  return translate.instant(textForEntityGroupsType);
}
