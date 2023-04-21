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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { RuleNodeId } from '@shared/models/id/rule-node-id';
import { RuleNode, RuleNodeComponentDescriptor, RuleNodeType } from '@shared/models/rule-node.models';
import { ComponentSingletonSupport, ComponentType } from '@shared/models/component-descriptor.models';
import { EntityGroupParams } from '@shared/models/entity-group.models';

export interface RuleChain extends BaseData<RuleChainId>, ExportableEntity<RuleChainId> {
  tenantId: TenantId;
  name: string;
  firstRuleNodeId: RuleNodeId;
  root: boolean;
  debugMode: boolean;
  type: string;
  configuration?: any;
  additionalInfo?: any;
  isDefault?: boolean;
}

export interface RuleChainMetaData {
  ruleChainId: RuleChainId;
  firstNodeIndex?: number;
  nodes: Array<RuleNode>;
  connections: Array<NodeConnectionInfo>;
}

export interface RuleChainImport {
  ruleChain: RuleChain;
  metadata: RuleChainMetaData;
}

export interface NodeConnectionInfo {
  fromIndex: number;
  toIndex: number;
  type: string;
}

export interface RuleChainParams extends EntityGroupParams {
  ruleChainScope: string;
}

export const ruleNodeTypeComponentTypes: ComponentType[] =
  [
    ComponentType.FILTER,
    ComponentType.ENRICHMENT,
    ComponentType.TRANSFORMATION,
    ComponentType.ACTION,
    ComponentType.ANALYTICS,
    ComponentType.EXTERNAL,
    ComponentType.FLOW
  ];

export const unknownNodeComponent: RuleNodeComponentDescriptor = {
  type: RuleNodeType.UNKNOWN,
  name: 'unknown',
  singleton: ComponentSingletonSupport.NOT_SUPPORTED,
  clazz: 'tb.internal.Unknown',
  configurationDescriptor: {
    nodeDefinition: {
      description: '',
      details: '',
      inEnabled: true,
      outEnabled: true,
      relationTypes: [],
      customRelations: false,
      defaultConfiguration: {}
    }
  }
};

export const inputNodeComponent: RuleNodeComponentDescriptor = {
  type: RuleNodeType.INPUT,
  singleton: ComponentSingletonSupport.NOT_SUPPORTED,
  name: 'Input',
  clazz: 'tb.internal.Input'
};

export enum RuleChainType {
  CORE = 'CORE',
  EDGE = 'EDGE'
}
