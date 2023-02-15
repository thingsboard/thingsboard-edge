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

import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { NavTreeNode, NodesCallback } from '@shared/components/nav-tree.component';
import { Datasource } from '@shared/models/widget.models';
import { isDefined, isUndefined } from '@core/utils';
import {
  EntityRelationsQuery,
  EntitySearchDirection,
  RelationEntityTypeFilter,
  RelationTypeGroup
} from '@shared/models/relation.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { WidgetContext } from '@home/models/widget-component.models';

export interface EntitiesHierarchyWidgetSettings {
  nodeRelationQueryFunction: string;
  nodeHasChildrenFunction: string;
  nodeOpenedFunction: string;
  nodeDisabledFunction: string;
  nodeIconFunction: string;
  nodeTextFunction: string;
  nodesSortFunction: string;
}

export interface HierarchyNodeContext {
  parentNodeCtx?: HierarchyNodeContext;
  entity: BaseData<EntityId>;
  childrenNodesLoaded?: boolean;
  level?: number;
  data: {[key: string]: any};
}

export interface HierarchyNavTreeNode extends NavTreeNode {
  data?: {
    datasource: HierarchyNodeDatasource;
    nodeCtx: HierarchyNodeContext;
    searchText?: string;
  };
}

export interface HierarchyNodeDatasource extends Datasource {
  nodeId: string;
}

export interface HierarchyNodeIconInfo {
  iconUrl?: string;
  materialIcon?: string;
}

export type NodeRelationQueryFunction = (widgetCtx: WidgetContext, nodeCtx: HierarchyNodeContext) => EntityRelationsQuery | 'default';
export type NodeTextFunction = (widgetCtx: WidgetContext, nodeCtx: HierarchyNodeContext) => string;
export type NodeDisabledFunction = (widgetCtx: WidgetContext, nodeCtx: HierarchyNodeContext) => boolean;
export type NodeIconFunction = (widgetCtx: WidgetContext, nodeCtx: HierarchyNodeContext) => HierarchyNodeIconInfo | 'default';
export type NodeOpenedFunction = (widgetCtx: WidgetContext, nodeCtx: HierarchyNodeContext) => boolean;
export type NodeHasChildrenFunction = (widgetCtx: WidgetContext, nodeCtx: HierarchyNodeContext) => boolean;
export type NodesSortFunction = (widgetCtx: WidgetContext, nodeCtx1: HierarchyNodeContext, nodeCtx2: HierarchyNodeContext) => number;

export function loadNodeCtxFunction<F extends (...args: any[]) => any>(functionBody: string, argNames: string, ...args: any[]): F {
  let nodeCtxFunction: F = null;
  if (isDefined(functionBody) && functionBody.length) {
    try {
      nodeCtxFunction = new Function(argNames, functionBody) as F;
      const res = nodeCtxFunction.apply(null, args);
      if (isUndefined(res)) {
        nodeCtxFunction = null;
      }
    } catch (e) {
      nodeCtxFunction = null;
    }
  }
  return nodeCtxFunction;
}

export function materialIconHtml(materialIcon: string): string {
  return '<mat-icon class="node-icon material-icons" role="img" aria-hidden="false">' + materialIcon + '</mat-icon>';
}

export function iconUrlHtml(iconUrl: string): string {
  return '<div class="node-icon" style="background-image: url(' + iconUrl + ');">&nbsp;</div>';
}

export const defaultNodeRelationQueryFunction: NodeRelationQueryFunction = (widgetContext, nodeCtx) => {
  const entity = nodeCtx.entity;
  const entityType = entity.id.entityType;
  let filters: RelationEntityTypeFilter[] = [
    {
      relationType: 'Contains',
      entityTypes: []
    }
  ];
  if (entityType === EntityType.TENANT || entityType === EntityType.CUSTOMER){
    filters = [];
  }
  const query: EntityRelationsQuery = {
    parameters: {
      rootId: entity.id.id,
      rootType: entity.id.entityType as EntityType,
      direction: EntitySearchDirection.FROM,
      relationTypeGroup: RelationTypeGroup.COMMON,
      maxLevel: 1
    },
    filters
  };
  return query;
};

export const defaultNodeIconFunction: NodeIconFunction = (widgetContext, nodeCtx) => {
  let materialIcon = 'insert_drive_file';
  const entity = nodeCtx.entity;
  if (entity && entity.id && entity.id.entityType) {
    const entityType = entity.id.entityType;
    if (entityType === EntityType.ENTITY_GROUP) {
      materialIcon = materialIconByEntityType((entity as EntityGroupInfo).type);
    } else {
      materialIcon = materialIconByEntityType(entityType);
    }
  }
  return {
    materialIcon
  };
};

function materialIconByEntityType(entityType: EntityType | string): string {
  let materialIcon = 'insert_drive_file';
  switch (entityType) {
    case 'function':
      materialIcon = 'functions';
      break;
    case EntityType.DEVICE:
      materialIcon = 'devices_other';
      break;
    case EntityType.ASSET:
      materialIcon = 'domain';
      break;
    case EntityType.TENANT:
      materialIcon = 'supervisor_account';
      break;
    case EntityType.CUSTOMER:
      materialIcon = 'supervisor_account';
      break;
    case EntityType.USER:
      materialIcon = 'account_circle';
      break;
    case EntityType.DASHBOARD:
      materialIcon = 'dashboards';
      break;
    case EntityType.ALARM:
      materialIcon = 'notifications_active';
      break;
    case EntityType.ENTITY_VIEW:
      materialIcon = 'view_quilt';
      break;
  }
  return materialIcon;
}

export const defaultNodeOpenedFunction: NodeOpenedFunction = (widgetCtx, nodeCtx) => {
  return nodeCtx.level <= 4;
};

export const defaultNodesSortFunction: NodesSortFunction = (widgetCtx, nodeCtx1, nodeCtx2) => {
  let result = 0;
  if (!nodeCtx1.entity.id.entityType || !nodeCtx2.entity.id.entityType ) {
    if (nodeCtx1.entity.id.entityType) {
      result = 1;
    } else if (nodeCtx2.entity.id.entityType) {
      result = -1;
    }
  } else {
    result = nodeCtx1.entity.id.entityType.localeCompare(nodeCtx2.entity.id.entityType);
  }
  if (result === 0) {
    if (nodeCtx1.entity.id.entityType === EntityType.ENTITY_GROUP) {
      result = (nodeCtx1.entity as EntityGroupInfo).type.localeCompare((nodeCtx2.entity as EntityGroupInfo).type);
    }
    if (result === 0) {
      result = nodeCtx1.entity.name.localeCompare(nodeCtx2.entity.name);
    }
  }
  return result;
};
