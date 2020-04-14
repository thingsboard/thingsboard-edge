///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LoadNodesCallback, NavTreeEditCallbacks, NodeSelectedCallback } from '@shared/components/nav-tree.component';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityGroupInfo, EntityGroupParams } from '@shared/models/entity-group.models';
import {
  CustomerNodeData,
  customerNodeText,
  CustomersHierarchyNode,
  CustomersHierarchyViewMode,
  EntityGroupNodeData,
  entityGroupNodeText,
  EntityGroupsNodeData,
  entityGroupsNodeText
} from '@home/pages/group/customers-hierarchy.models';
import { EntityService } from '@core/http/entity.service';
import { Customer } from '@shared/models/customer.model';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { groupResourceByGroupType, Operation } from '@shared/models/security.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityGroupsTableConfigResolver } from '@home/pages/group/entity-groups-table-config.resolver';
import { EntityGroupConfigResolver } from '@home/pages/group/entity-group-config.resolver';
import { EntityGroupStateInfo } from '@home/models/group/group-entities-table-config.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityGroupsTableConfig } from '@home/pages/group/entity-groups-table-config';

const groupTypes: EntityType[] = [
  EntityType.USER,
  EntityType.CUSTOMER,
  EntityType.ASSET,
  EntityType.DEVICE,
  EntityType.ENTITY_VIEW,
  EntityType.DASHBOARD
];

@Component({
  selector: 'tb-customers-hierarchy',
  templateUrl: './customers-hierarchy.component.html',
  styleUrls: ['./customers-hierarchy.component.scss']
})
export class CustomersHierarchyComponent extends PageComponent implements OnInit {

  isNavTreeOpen = true;

  nodeIdCounter = 0;
  selectedNodeId = -1;

  parentIdToGroupAllNodeId: {[parentNodeId: string]: string} = {};

  entityGroupNodesMap: {[parentNodeId: string]: {[entityGroupId: string]: string}} = {};
  customerNodesMap: {[parentNodeId: string]: {[customerId: string]: string}} = {};
  customerGroupsNodesMap: {[customerNodeId: string]: {[groupsType: string]: string}} = {};

  internalIdToNodeIds: {[internalId: string]: string[]} = {};
  internalIdToParentNodeIds: {[internalId: string]: string[]} = {};
  nodeIdToInternalId: {[nodeId: string]: string} = {};

  viewMode: CustomersHierarchyViewMode = 'groups';
  entityGroupParams: EntityGroupParams = {
    groupType: EntityType.CUSTOMER
  };
  entityGroupsTableConfig: EntityGroupsTableConfig = this.resolveEntityGroupTableConfig(this.entityGroupParams);
  entityGroupStateInfo: EntityGroupStateInfo<BaseData<HasId>> = null;

  public nodeEditCallbacks: NavTreeEditCallbacks = {};

  private allowedGroupTypes = groupTypes.filter((groupType) =>
    this.userPermissionsService.hasGenericPermission(groupResourceByGroupType.get(groupType), Operation.READ));

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef,
              private ngZone: NgZone,
              private entityGroupService: EntityGroupService,
              private entityService: EntityService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private entityGroupsTableConfigResolver: EntityGroupsTableConfigResolver,
              private entityGroupConfigResolver: EntityGroupConfigResolver) {
    super(store);
  }

  ngOnInit() {
  }

  public loadNodes: LoadNodesCallback = (node: CustomersHierarchyNode, cb) => {
    if (node.id === '#') {
      this.entityGroupService.getEntityGroups(EntityType.CUSTOMER, {ignoreLoading: true}).subscribe((entityGroups) => {
        cb(this.entityGroupsToNodes(node.id, null, entityGroups));
        // TODO: load root view
      });
    } else if (node.data && node.data.type) {
      if (node.data.type === 'group') {
        const entityGroup = node.data.entity;
        if (entityGroup.type === EntityType.CUSTOMER) {
          this.entityService.getEntityGroupEntities(entityGroup.id.id,
            EntityType.CUSTOMER, -1, {ignoreLoading: true}).subscribe(
              (customers: Customer[]) => {
              cb(this.customersToNodes(node.id, entityGroup.id.id, customers));
            });
        } else {
          cb([]);
        }
      } else if (node.data.type === 'customer') {
        const customer = node.data.entity;
        const parentEntityGroupId = node.data.parentEntityGroupId;
        cb(this.loadNodesForCustomer(node.id, parentEntityGroupId, customer));
      } else if (node.data.type === 'groups') {
        const owner = node.data.customer;
        const parentEntityGroupId = node.data.parentEntityGroupId;
        this.entityGroupService.getEntityGroupsByOwnerId(owner.id.entityType,
          owner.id.id, node.data.groupsType, {ignoreLoading: true}).subscribe((entityGroups) => {
          cb(this.entityGroupsToNodes(node.id, parentEntityGroupId, entityGroups));
        });
      }
    }
  }

  public onNodeSelected: NodeSelectedCallback = (node: CustomersHierarchyNode, event) => {
    let nodeId;
    if (!node) {
      nodeId = -1;
    } else {
      nodeId = node.id;
    }
    if (this.selectedNodeId !== nodeId) {
      this.selectedNodeId = nodeId;
      const entityGroupParams: EntityGroupParams = {};
      if (nodeId === -1) {
        entityGroupParams.groupType = EntityType.CUSTOMER;
        this.updateGroupsView(entityGroupParams);
      } else if (node.data.type === 'groups' || node.data.type === 'group') {
        if (node.data.type === 'groups') {
          const parentEntityGroupId = node.data.parentEntityGroupId;
          if (parentEntityGroupId) {
            entityGroupParams.entityGroupId = parentEntityGroupId;
            entityGroupParams.groupType = EntityType.CUSTOMER;
            entityGroupParams.childGroupType = node.data.groupsType;
          } else {
            entityGroupParams.groupType = node.data.groupsType;
          }
          entityGroupParams.customerId = node.data.customer.id.id;
          this.updateGroupsView(entityGroupParams, node.data.customer);
        } else {
          const entityGroup = node.data.entity;
          const parentEntityGroupId = node.data.parentEntityGroupId;
          if (parentEntityGroupId) {
            entityGroupParams.entityGroupId = parentEntityGroupId;
            entityGroupParams.groupType = EntityType.CUSTOMER;
            entityGroupParams.childEntityGroupId = entityGroup.id.id;
            entityGroupParams.childGroupType = entityGroup.type;
          } else {
            entityGroupParams.entityGroupId = entityGroup.id.id;
            entityGroupParams.groupType = entityGroup.type;
          }
          if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
            entityGroupParams.customerId = entityGroup.ownerId.id;
          } else {
            entityGroupParams.customerId = null;
          }
          this.updateGroupView(entityGroupParams, entityGroup);
        }
      }
    }
  }

  private updateGroupsView(entityGroupParams: EntityGroupParams, customer?: Customer) {
    const entityGroupsTableConfig = this.resolveEntityGroupTableConfig(entityGroupParams, customer?.title);
    this.updateView('groups', entityGroupParams, entityGroupsTableConfig, null);
  }

  private updateGroupView(entityGroupParams: EntityGroupParams, entityGroup: EntityGroupInfo) {
    this.entityGroupConfigResolver.constructGroupConfig(this.entityGroupParams, entityGroup).subscribe(
      (entityGroupStateInfo) => {
        this.updateView('group', entityGroupParams, null, entityGroupStateInfo);
      }
    );
  }

  private updateView(viewMode: CustomersHierarchyViewMode,
                     entityGroupParams: EntityGroupParams,
                     entityGroupsTableConfig: EntityGroupsTableConfig,
                     entityGroupStateInfo: EntityGroupStateInfo<BaseData<HasId>>) {
    setTimeout(() => {
      this.viewMode = viewMode;
      this.entityGroupParams = entityGroupParams;
      this.entityGroupsTableConfig = entityGroupsTableConfig;
      this.entityGroupStateInfo = entityGroupStateInfo;
    }, 0);
  }

  private resolveEntityGroupTableConfig(entityGroupParams: EntityGroupParams, customerTitle?: string): EntityGroupsTableConfig {
    return this.entityGroupsTableConfigResolver.resolveEntityGroupTableConfig(entityGroupParams,
        false, customerTitle) as EntityGroupsTableConfig;
  }

  selectRootNode() {
    setTimeout(() => {
      this.nodeEditCallbacks.deselectAll();
    }, 0);
  }

  private entityGroupsToNodes(parentNodeId: string, parentEntityGroupId: string,
                              entityGroups: EntityGroupInfo[]): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    this.entityGroupNodesMap[parentNodeId] = {};
    if (entityGroups) {
      entityGroups.forEach((entityGroup) => {
        const node = this.createEntityGroupNode(parentNodeId, entityGroup, parentEntityGroupId);
        nodes.push(node);
        if (entityGroup.groupAll) {
          this.parentIdToGroupAllNodeId[parentNodeId] = node.id;
        }
      });
    }
    return nodes;
  }

  private createEntityGroupNode(parentNodeId: string, entityGroup: EntityGroupInfo, parentEntityGroupId: string): CustomersHierarchyNode {
    let nodesMap = this.entityGroupNodesMap[parentNodeId];
    if (!nodesMap) {
      nodesMap = {};
      this.entityGroupNodesMap[parentNodeId] = nodesMap;
    }
    const node: CustomersHierarchyNode = {
      id: (++this.nodeIdCounter)+'',
      icon: false,
      text: entityGroupNodeText(entityGroup),
      children: entityGroup.type === EntityType.CUSTOMER,
      data: {
        type: 'group',
        entity: entityGroup,
        parentEntityGroupId,
        internalId: entityGroup.id.id
      } as EntityGroupNodeData
    };
    nodesMap[entityGroup.id.id] = node.id;
    this.registerNode(node, parentNodeId);
    return node;
  }

  private customersToNodes(parentNodeId: string, groupId: string, customers: Customer[]): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    this.customerNodesMap[parentNodeId] = {};
    if (customers) {
      customers.forEach((customer) => {
        const node = this.createCustomerNode(parentNodeId, customer, groupId);
        nodes.push(node);
      });
    }
    return nodes;
  }

  private createCustomerNode(parentNodeId: string, customer: Customer, groupId: string): CustomersHierarchyNode {
    let nodesMap = this.customerNodesMap[parentNodeId];
    if (!nodesMap) {
      nodesMap = {};
      this.customerNodesMap[parentNodeId] = nodesMap;
    }
    const node: CustomersHierarchyNode = {
      id: (++this.nodeIdCounter)+'',
      icon: false,
      text: customerNodeText(customer),
      children: true,
      state: {
        disabled: true
      },
      data: {
        type: 'customer',
        entity: customer,
        parentEntityGroupId: groupId,
        internalId: customer.id.id
      } as CustomerNodeData
    };
    nodesMap[customer.id.id] = node.id;
    this.registerNode(node, parentNodeId);
    return node;
  }

  private loadNodesForCustomer(parentNodeId: string, parentEntityGroupId: string,
                               customer: Customer): CustomersHierarchyNode[] {
    const nodes: CustomersHierarchyNode[] = [];
    const nodesMap = {};
    this.customerGroupsNodesMap[parentNodeId] = nodesMap;
    this.allowedGroupTypes.forEach((groupType) => {
      const node: CustomersHierarchyNode = {
        id: (++this.nodeIdCounter)+'',
        icon: false,
        text: entityGroupsNodeText(this.translate, groupType),
        children: true,
        data: {
          type: 'groups',
          groupsType: groupType,
          customer,
          parentEntityGroupId,
          internalId: customer.id.id + '_' + groupType
        } as EntityGroupsNodeData
      };
      nodes.push(node);
      nodesMap[groupType] = node.id;
      this.registerNode(node, parentNodeId);
    });
    return nodes;
  }

  private registerNode(node: CustomersHierarchyNode, parentNodeId: string) {
    let nodeIds = this.internalIdToNodeIds[node.data.internalId];
    if (!nodeIds) {
      nodeIds = [];
      this.internalIdToNodeIds[node.data.internalId] = nodeIds;
    }
    if (nodeIds.indexOf(node.id) === -1) {
      nodeIds.push(node.id);
    }
    let parentNodeIds = this.internalIdToParentNodeIds[node.data.internalId];
    if (!parentNodeIds) {
      parentNodeIds = [];
      this.internalIdToParentNodeIds[node.data.internalId] = parentNodeIds;
    }
    if (parentNodeIds.indexOf(parentNodeId) === -1) {
      parentNodeIds.push(parentNodeId);
    }
    this.nodeIdToInternalId[node.id] = node.data.internalId;
  }

}
