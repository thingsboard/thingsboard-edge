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

import { Component, Input, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetContext } from '@home/models/widget-component.models';
import { Datasource, DatasourceType, WidgetConfig } from '@shared/models/widget.models';
import { IWidgetSubscription } from '@core/api/widget-api.models';
import { UtilsService } from '@core/services/utils.service';
import { LoadNodesCallback } from '@shared/components/nav-tree.component';
import { EntityType } from '@shared/models/entity-type.models';
import {
  EdgeGroupNodeData,
  edgeGroupsNodeText,
  EdgeOverviewNode,
  entityGroupNodeText,
  EntityNodeData,
  EntityNodeDatasource,
  entityNodeText
} from '@home/components/widget/lib/edges-overview-widget.models';
import { EdgeService } from '@core/http/edge.service';
import { EntityService } from '@core/http/entity.service';
import { TranslateService } from '@ngx-translate/core';
import { PageLink } from '@shared/models/page/page-link';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { edgeEntityGroupTypes, edgeEntityTypes } from "@shared/models/edge.models";
import { EntityGroupNodeData } from "@home/pages/group/customers-hierarchy.models";
import { groupResourceByGroupType, Operation, resourceByEntityType } from "@shared/models/security.models";
import { UserPermissionsService } from "@core/http/user-permissions.service";
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { Observable } from 'rxjs';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { RuleChainService } from '@core/http/rule-chain.service';
import { map } from 'rxjs/operators';

@Component({
  selector: 'tb-edges-overview-widget',
  templateUrl: './edges-overview-widget.component.html',
  styleUrls: ['./edges-overview-widget.component.scss']
})
export class EdgesOverviewWidgetComponent extends PageComponent implements OnInit {

  @Input()
  ctx: WidgetContext;

  public toastTargetId = 'edges-overview-' + this.utils.guid();
  public edgeIsDatasource: boolean = true;

  private widgetConfig: WidgetConfig;
  private subscription: IWidgetSubscription;
  private datasources: Array<EntityNodeDatasource>;

  private nodeIdCounter = 0;

  private entityNodesMap: {[parentNodeId: string]: {[edgeId: string]: string}} = {};
  private entityGroupNodesMap: {[edgeNodeId: string]: {[groupType: string]: string}} = {};

  private allowedGroupTypes = edgeEntityGroupTypes.filter((groupType) =>
    this.userPermissionsService.hasGenericPermission(groupResourceByGroupType.get(groupType), Operation.READ));

  private allowedEntityTypes = edgeEntityTypes.filter((entityType) =>
    this.userPermissionsService.hasGenericPermission(resourceByEntityType.get(entityType), Operation.READ));

  constructor(protected store: Store<AppState>,
              private edgeService: EdgeService,
              private entityService: EntityService,
              private entityGroupService: EntityGroupService,
              private schedulerEventService: SchedulerEventService,
              private ruleChainService: RuleChainService,
              private translateService: TranslateService,
              private utils: UtilsService,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit(): void {
    this.widgetConfig = this.ctx.widgetConfig;
    this.subscription = this.ctx.defaultSubscription;
    this.datasources = this.subscription.datasources as Array<EntityNodeDatasource>;
    this.ctx.updateWidgetParams();
  }

  public loadNodes: LoadNodesCallback = (node, cb) => {
    const datasource: Datasource = this.datasources[0];
    if (datasource) {
      const pageLink = new PageLink(datasource.pageLink.pageSize);
      if (node.id === '#') {
        if (datasource.type === DatasourceType.entity && datasource.entity.id.entityType === EntityType.EDGE) {
          var selectedEdge: BaseData<EntityId> = datasource.entity;
          cb(this.loadNodesForEdge(selectedEdge.id.id, selectedEdge));
        } else if (datasource.type === DatasourceType.function) {
          cb(this.loadNodesForEdge(datasource.entityId, datasource.entity));
        } else {
          this.edgeIsDatasource = false;
          cb([]);
        }
      } else if (node.data && node.data.type === 'edgeGroup') {
        let entitiesObservable: Observable<any>;
        const edgeId = node.data.entity.id.id;
        const entityType = node.data.entityType;
        switch (entityType) {
          case (EntityType.SCHEDULER_EVENT):
            entitiesObservable = this.schedulerEventService.getEdgeSchedulerEvents(edgeId);
            break;
          case (EntityType.RULE_CHAIN):
            entitiesObservable = this.ruleChainService.getEdgeRuleChains(edgeId, pageLink).pipe(map(entities => entities.data));
            break;
          default:
            entitiesObservable = this.entityGroupService.getEdgeEntityGroups(edgeId, entityType, {ignoreLoading: true});
        }
        entitiesObservable.subscribe((entityGroups) => {
          cb(this.entityGroupsToNodes(node.id, entityGroups));
        });
      } else if (node.data && node.data.type === 'group') {
        const entityId = node.data.entity.id.id;
        const entityType = node.data.entity.type;
        this.entityGroupService.getEntityGroupEntities(entityId, pageLink, entityType).subscribe(
          (entities) => {
            cb(this.entitiesToNodes(node.id, entities.data, entityType));
          }
        );
      } else {
        cb([]);
      }
    } else {
      cb([]);
    }
  }

  private loadNodesForEdge(parentNodeId: string, entity: BaseData<HasId>): EdgeOverviewNode[] {
    const nodes: EdgeOverviewNode[] = [];
    const nodesMap = {};
    this.entityGroupNodesMap[parentNodeId] = nodesMap;
    this.allowedGroupTypes.forEach((entityType) => {
      const node: EdgeOverviewNode = this.createEdgeGroupNode(entityType, entity);
      nodes.push(node);
      nodesMap[entityType] = node.id;
    });
    this.allowedEntityTypes.forEach((entityType) => {
      const node: EdgeOverviewNode = this.createEdgeGroupNode(entityType, entity);
      nodes.push(node);
      nodesMap[entityType] = node.id;
    });
    return nodes;
  }

  private createEdgeGroupNode(entityType: EntityType, entity: BaseData<HasId>): EdgeOverviewNode {
    return {
      id: (++this.nodeIdCounter)+'',
      icon: false,
      text: edgeGroupsNodeText(this.translateService, entityType),
      children: true,
      data: {
        type: 'edgeGroup',
        entityType,
        entity,
        internalId: entity.id.id + '_' + entityType
      } as EdgeGroupNodeData
    };
  }

  private entityGroupsToNodes(parentNodeId: string, entityGroups: EntityGroupInfo[]): EdgeOverviewNode[] {
    const nodes: EdgeOverviewNode[] = [];
    this.entityGroupNodesMap[parentNodeId] = {};
    if (entityGroups) {
      entityGroups.forEach((entityGroup) => {
        const node = this.createEntityGroupNode(parentNodeId, entityGroup);
        nodes.push(node);
      });
    }
    return nodes;
  }

  private createEntityGroupNode(parentNodeId: string, entityGroup: EntityGroupInfo): EdgeOverviewNode {
    let nodesMap = this.entityGroupNodesMap[parentNodeId];
    if (!nodesMap) {
      nodesMap = {};
      this.entityGroupNodesMap[parentNodeId] = nodesMap;
    }
    const node: EdgeOverviewNode = {
      id: (++this.nodeIdCounter)+'',
      icon: false,
      text: entityGroup.id.entityType === EntityType.ENTITY_GROUP ? entityGroupNodeText(entityGroup) : entityNodeText(entityGroup),
      children: entityGroup.id.entityType === EntityType.ENTITY_GROUP,
      data: {
        type: 'group',
        entity: entityGroup,
        internalId: entityGroup.id.id
      } as EntityGroupNodeData
    };
    nodesMap[entityGroup.id.id] = node.id;
    return node;
  }

  private entitiesToNodes(parentNodeId: string, entities: BaseData<HasId>[], entityType: EntityType): EdgeOverviewNode[] {
    const nodes: EdgeOverviewNode[] = [];
    this.entityNodesMap[parentNodeId] = {};
    if (entities) {
      entities.forEach((entity) => {
        const node = this.createEntityNode(parentNodeId, entity, entityType);
        nodes.push(node);
      });
    }
    return nodes;
  }

  private createEntityNode(parentNodeId: string, entity: BaseData<HasId>, entityType: EntityType): EdgeOverviewNode {
    let nodesMap = this.entityNodesMap[parentNodeId];
    if (!nodesMap) {
      nodesMap = {};
      this.entityNodesMap[parentNodeId] = nodesMap;
    }
    const node: EdgeOverviewNode = {
      id: (++this.nodeIdCounter)+'',
      icon: false,
      text: entityNodeText(entity),
      children: false,
      data: {
        entityType,
        entity,
        internalId: entity.id.id
      } as EntityNodeData
    };
    nodesMap[entity.id.id] = node.id;
    return node;
  }

}
