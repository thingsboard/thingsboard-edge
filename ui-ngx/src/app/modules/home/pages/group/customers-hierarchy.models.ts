///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Customer } from '@shared/models/customer.model';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { NavTreeNode } from '@shared/components/nav-tree.component';

export type CustomersHierarchyViewMode = 'groups' | 'group';

export type CustomersHierarchyNodeType = 'group' | 'groups' | 'customer';

export interface BaseCustomersHierarchyNodeData {
  type: CustomersHierarchyNodeType;
  parentEntityGroupId: string;
  internalId: string;
}

export interface EntityGroupNodeData extends BaseCustomersHierarchyNodeData {
  type: 'group';
  entity: EntityGroupInfo;
}

export interface EntityGroupsNodeData extends BaseCustomersHierarchyNodeData {
  type: 'groups';
  groupsType: EntityType;
  customer: Customer;
}

export interface CustomerNodeData extends BaseCustomersHierarchyNodeData {
  type: 'customer';
  entity: Customer;
}

export type CustomersHierarchyNodeData = EntityGroupNodeData | EntityGroupsNodeData | CustomerNodeData;

export interface CustomersHierarchyNode extends NavTreeNode {
  data?: CustomersHierarchyNodeData;
}

export function entityGroupNodeText(entityGroup: EntityGroupInfo): string {
  const nodeIcon = materialIconByEntityType(entityGroup.type);
  return nodeIcon + entityGroup.name;
}

export function customerNodeText(customer: Customer): string {
  const nodeIcon = materialIconByEntityType(EntityType.CUSTOMER);
  return nodeIcon + customer.title;
}

export function entityGroupsNodeText(translate: TranslateService, groupType: EntityType) {
  const nodeIcon = materialIconByEntityType(groupType);
  const nodeText = textForGroupType(translate, groupType);
  return nodeIcon + nodeText;
}

function textForGroupType(translate: TranslateService, groupType: EntityType): string {
  switch (groupType) {
    case EntityType.USER:
      return translate.instant('entity-group.user-groups');
    case EntityType.CUSTOMER:
      return translate.instant('entity-group.customer-groups');
    case EntityType.ASSET:
      return translate.instant('entity-group.asset-groups');
    case EntityType.DEVICE:
      return translate.instant('entity-group.device-groups');
    case EntityType.ENTITY_VIEW:
      return translate.instant('entity-group.entity-view-groups');
    case EntityType.DASHBOARD:
      return translate.instant('entity-group.dashboard-groups');
  }
  return '';
}

function materialIconByEntityType (entityType: EntityType): string {
  let materialIcon = 'insert_drive_file';
  switch (entityType) {
    case EntityType.DEVICE:
      materialIcon = 'devices_other';
      break;
    case EntityType.ASSET:
      materialIcon = 'domain';
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
    case EntityType.ENTITY_VIEW:
      materialIcon = 'view_quilt';
      break;
  }
  return '<mat-icon class="node-icon material-icons" role="img" aria-hidden="false">' + materialIcon + '</mat-icon>';
}
