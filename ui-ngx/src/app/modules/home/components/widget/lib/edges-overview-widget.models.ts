///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

export function edgeGroupsNodeText(translate: TranslateService, entityType: EntityType): string {
  const nodeIcon = materialIconByEntityType(entityType);
  const nodeText = textForEdgeGroupsType(translate, entityType);
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
    case EntityType.DASHBOARD:
      materialIcon = 'dashboards';
      break;
    case EntityType.ENTITY_VIEW:
      materialIcon = 'view_quilt';
      break;
    case EntityType.RULE_CHAIN:
      materialIcon = 'settings_ethernet';
      break;
  }
  return '<mat-icon class="node-icon material-icons" role="img" aria-hidden="false">' + materialIcon + '</mat-icon>';
}

export function textForEdgeGroupsType(translate: TranslateService, entityType: EntityType): string {
  let textForEdgeGroupsType: string = '';
  switch (entityType) {
    case EntityType.DEVICE:
      textForEdgeGroupsType = 'device.devices';
      break;
    case EntityType.ASSET:
      textForEdgeGroupsType = 'asset.assets';
      break;
    case EntityType.DASHBOARD:
      textForEdgeGroupsType = 'dashboard.dashboards';
      break;
    case EntityType.ENTITY_VIEW:
      textForEdgeGroupsType = 'entity-view.entity-views';
      break;
    case EntityType.RULE_CHAIN:
      textForEdgeGroupsType = 'rulechain.rulechains';
      break;
  }
  return translate.instant(textForEdgeGroupsType);
}

export const edgeGroupsTypes: EntityType[] = [
  EntityType.ASSET,
  EntityType.DEVICE,
  EntityType.ENTITY_VIEW,
  EntityType.DASHBOARD,
  EntityType.RULE_CHAIN
];

export interface EdgeOverviewNode extends NavTreeNode {
  data?: EdgeOverviewNodeData;
}

export type EdgeOverviewNodeData = EdgeGroupNodeData | EntityNodeData;

export interface EdgeGroupNodeData extends BaseEdgeOverviewNodeData {
  type: 'edgeGroups';
  entityType: EntityType;
  entity: BaseData<HasId>;
}

export interface EntityNodeData extends BaseEdgeOverviewNodeData {
  entity: BaseData<HasId>;
}

export interface BaseEdgeOverviewNodeData {
  internalId: string;
}
