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
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityId } from '@shared/models/id/entity-id';
import { EventId } from './id/event-id';
import { ContentType } from '@shared/models/constants';
import { EntityType } from '@shared/models/entity-type.models';

export enum EventType {
  ERROR = 'ERROR',
  LC_EVENT = 'LC_EVENT',
  STATS = 'STATS'
}

export enum DebugEventType {
  DEBUG_RULE_NODE = 'DEBUG_RULE_NODE',
  DEBUG_RULE_CHAIN = 'DEBUG_RULE_CHAIN',
  DEBUG_CONVERTER = 'DEBUG_CONVERTER',
  DEBUG_INTEGRATION = 'DEBUG_INTEGRATION'
}

export const eventTypeTranslations = new Map<EventType | DebugEventType, string>(
  [
    [EventType.ERROR, 'event.type-error'],
    [EventType.LC_EVENT, 'event.type-lc-event'],
    [EventType.STATS, 'event.type-stats'],
    [DebugEventType.DEBUG_RULE_NODE, 'event.type-debug-rule-node'],
    [DebugEventType.DEBUG_RULE_CHAIN, 'event.type-debug-rule-chain'],
    [DebugEventType.DEBUG_CONVERTER, 'event.type-debug-converter'],
    [DebugEventType.DEBUG_INTEGRATION, 'event.type-debug-integration']
  ]
);

export interface BaseEventBody {
  server: string;
}

export interface ErrorEventBody extends BaseEventBody {
  method: string;
  error: string;
}

export interface LcEventEventBody extends BaseEventBody {
  event: string;
  success: boolean;
  error: string;
}

export interface StatsEventBody extends BaseEventBody {
  messagesProcessed: number;
  errorsOccurred: number;
}

export interface RawDataEventBody extends BaseEventBody {
  message: string;
  messageType: ContentType;
  uuid: string;
}

export interface DebugRuleNodeEventBody extends BaseEventBody {
  type: string;
  entityId: string;
  entityType: string;
  msgId: string;
  msgType: string;
  relationType: string;
  dataType: ContentType;
  data: string;
  metadata: string;
  error: string;
}

export interface DebugRuleChainEventBody extends BaseEventBody {
  message: string;
  error?: string;
}

export interface DebugConverterEventBody extends BaseEventBody {
  type: string;
  in: string;
  inMessageType: ContentType;
  out: string;
  outMessageType: ContentType;
  metadata: string;
  error: string;
}

export interface DebugIntegrationEventBody extends BaseEventBody {
  type: string;
  message: string;
  messageType: ContentType;
  status: string;
  error: string;
}

export type EventBody = ErrorEventBody & LcEventEventBody & StatsEventBody & RawDataEventBody
                        & DebugRuleNodeEventBody & DebugRuleChainEventBody & DebugConverterEventBody & DebugIntegrationEventBody;

export interface Event extends BaseData<EventId> {
  tenantId: TenantId;
  entityId: EntityId;
  type: string;
  uid: string;
  body: EventBody;
}

export interface BaseFilterEventBody {
  server?: string;
}

export interface ErrorFilterEventBody extends BaseFilterEventBody {
  method?: string;
  errorStr?: string;
}

export interface LcFilterEventEventBody extends BaseFilterEventBody {
  event?: string;
  status?: string;
  errorStr?: string;
}

export interface StatsFilterEventBody extends BaseFilterEventBody {
  messagesProcessed?: number;
  errorsOccurred?: number;
}

export interface DebugFilterRuleNodeEventBody extends BaseFilterEventBody {
  msgDirectionType?: string;
  entityId?: string;
  entityName?: EntityType;
  msgId?: string;
  msgType?: string;
  relationType?: string;
  dataSearch?: string;
  metadataSearch?: string;
  isError?: boolean;
  errorStr?: string;
}

export type FilterEventBody = ErrorFilterEventBody & LcFilterEventEventBody & StatsFilterEventBody & DebugFilterRuleNodeEventBody;
