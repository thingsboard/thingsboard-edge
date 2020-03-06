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


import { EntityType } from '@shared/models/entity-type.models';
import { AggregationType } from '../time/time.models';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { map } from 'rxjs/operators';
import { NgZone } from '@angular/core';

export enum DataKeyType {
  timeseries = 'timeseries',
  attribute = 'attribute',
  function = 'function',
  alarm = 'alarm',
  entityField = 'entityField'
}

export enum LatestTelemetry {
  LATEST_TELEMETRY = 'LATEST_TELEMETRY'
}

export enum AttributeScope {
  CLIENT_SCOPE = 'CLIENT_SCOPE',
  SERVER_SCOPE = 'SERVER_SCOPE',
  SHARED_SCOPE = 'SHARED_SCOPE'
}

export enum TelemetryFeature {
  ATTRIBUTES = 'ATTRIBUTES',
  TIMESERIES = 'TIMESERIES'
}

export type TelemetryType = LatestTelemetry | AttributeScope;

export function toTelemetryType(val: string): TelemetryType {
  if (LatestTelemetry[val]) {
    return LatestTelemetry[val];
  } else {
    return AttributeScope[val];
  }
}

export const telemetryTypeTranslations = new Map<TelemetryType, string>(
  [
    [LatestTelemetry.LATEST_TELEMETRY, 'attribute.scope-latest-telemetry'],
    [AttributeScope.CLIENT_SCOPE, 'attribute.scope-client'],
    [AttributeScope.SERVER_SCOPE, 'attribute.scope-server'],
    [AttributeScope.SHARED_SCOPE, 'attribute.scope-shared']
  ]
);

export const isClientSideTelemetryType = new Map<TelemetryType, boolean>(
  [
    [LatestTelemetry.LATEST_TELEMETRY, true],
    [AttributeScope.CLIENT_SCOPE, true],
    [AttributeScope.SERVER_SCOPE, false],
    [AttributeScope.SHARED_SCOPE, false]
  ]
);

export interface AttributeData {
  lastUpdateTs?: number;
  key: string;
  value: any;
}

export interface TelemetryPluginCmd {
  cmdId: number;
  keys: string;
}

export abstract class SubscriptionCmd implements TelemetryPluginCmd {
  cmdId: number;
  keys: string;
  entityType: EntityType;
  entityId: string;
  scope?: AttributeScope;
  unsubscribe: boolean;
  abstract getType(): TelemetryFeature;
}

export class AttributesSubscriptionCmd extends SubscriptionCmd {
  getType() {
    return TelemetryFeature.ATTRIBUTES;
  }
}

export class TimeseriesSubscriptionCmd extends SubscriptionCmd {
  startTs: number;
  timeWindow: number;
  interval: number;
  limit: number;
  agg: AggregationType;

  getType() {
    return TelemetryFeature.TIMESERIES;
  }
}

export class GetHistoryCmd implements TelemetryPluginCmd {
  cmdId: number;
  keys: string;
  entityType: EntityType;
  entityId: string;
  startTs: number;
  endTs: number;
  interval: number;
  limit: number;
  agg: AggregationType;
}

export class TelemetryPluginCmdsWrapper {
  attrSubCmds: Array<AttributesSubscriptionCmd>;
  tsSubCmds: Array<TimeseriesSubscriptionCmd>;
  historyCmds: Array<GetHistoryCmd>;

  constructor() {
    this.attrSubCmds = [];
    this.tsSubCmds = [];
    this.historyCmds = [];
  }

  public hasCommands(): boolean {
    return this.tsSubCmds.length > 0 ||
      this.historyCmds.length > 0 ||
      this.attrSubCmds.length > 0;
  }

  public clear() {
    this.attrSubCmds.length = 0;
    this.tsSubCmds.length = 0;
    this.historyCmds.length = 0;
  }

  public preparePublishCommands(maxCommands: number): TelemetryPluginCmdsWrapper {
    const preparedWrapper = new TelemetryPluginCmdsWrapper();
    let leftCount = maxCommands;
    preparedWrapper.tsSubCmds = this.popCmds(this.tsSubCmds, leftCount);
    leftCount -= preparedWrapper.tsSubCmds.length;
    preparedWrapper.historyCmds = this.popCmds(this.historyCmds, leftCount);
    leftCount -= preparedWrapper.historyCmds.length;
    preparedWrapper.attrSubCmds = this.popCmds(this.attrSubCmds, leftCount);
    return preparedWrapper;
  }

  private popCmds<T extends TelemetryPluginCmd>(cmds: Array<T>, leftCount: number): Array<T> {
    const toPublish = Math.min(cmds.length, leftCount);
    if (toPublish > 0) {
      return cmds.splice(0, toPublish);
    } else {
      return [];
    }
  }
}

export interface SubscriptionData {
  [key: string]: [number, any][];
}

export interface SubscriptionDataHolder {
  data: SubscriptionData;
}

export interface SubscriptionUpdateMsg extends SubscriptionDataHolder {
  subscriptionId: number;
  errorCode: number;
  errorMsg: string;
}

export class SubscriptionUpdate implements SubscriptionUpdateMsg {
  subscriptionId: number;
  errorCode: number;
  errorMsg: string;
  data: SubscriptionData;

  constructor(msg: SubscriptionUpdateMsg) {
    this.subscriptionId = msg.subscriptionId;
    this.errorCode = msg.errorCode;
    this.errorMsg = msg.errorMsg;
    this.data = msg.data;
  }

  public prepareData(keys: string[]) {
    if (!this.data) {
      this.data = {};
    }
    if (keys) {
      keys.forEach((key) => {
        if (!this.data[key]) {
          this.data[key] = [];
        }
      });
    }
  }

  public updateAttributeData(origData: Array<AttributeData>): Array<AttributeData> {
    for (const key of Object.keys(this.data)) {
      const keyData = this.data[key];
      if (keyData.length) {
        const existing = origData.find((data) => data.key === key);
        if (existing) {
          existing.lastUpdateTs = keyData[0][0];
          existing.value = keyData[0][1];
        } else {
          origData.push(
            {
              key,
              lastUpdateTs: keyData[0][0],
              value: keyData[0][1]
            }
          );
        }
      }
    }
    return origData;
  }
}

export interface TelemetryService {
  subscribe(subscriber: TelemetrySubscriber);
  unsubscribe(subscriber: TelemetrySubscriber);
}

export class TelemetrySubscriber {

  private dataSubject = new ReplaySubject<SubscriptionUpdate>(1);
  private reconnectSubject = new Subject();

  private zone: NgZone;

  public subscriptionCommands: Array<TelemetryPluginCmd>;

  public data$ = this.dataSubject.asObservable();
  public reconnect$ = this.reconnectSubject.asObservable();

  public static createEntityAttributesSubscription(telemetryService: TelemetryService,
                                                   entityId: EntityId, attributeScope: TelemetryType,
                                                   zone: NgZone, keys: string[] = null): TelemetrySubscriber {
    let subscriptionCommand: SubscriptionCmd;
    if (attributeScope === LatestTelemetry.LATEST_TELEMETRY) {
      subscriptionCommand = new TimeseriesSubscriptionCmd();
    } else {
      subscriptionCommand = new AttributesSubscriptionCmd();
    }
    subscriptionCommand.entityType = entityId.entityType as EntityType;
    subscriptionCommand.entityId = entityId.id;
    subscriptionCommand.scope = attributeScope as AttributeScope;
    if (keys) {
      subscriptionCommand.keys = keys.join(',');
    }
    const subscriber = new TelemetrySubscriber(telemetryService);
    subscriber.zone = zone;
    subscriber.subscriptionCommands.push(subscriptionCommand);
    return subscriber;
  }

  constructor(private telemetryService: TelemetryService) {
    this.subscriptionCommands = [];
  }

  public subscribe() {
    this.telemetryService.subscribe(this);
  }

  public unsubscribe() {
    this.telemetryService.unsubscribe(this);
    this.dataSubject.complete();
    this.reconnectSubject.complete();
  }

  public onData(message: SubscriptionUpdate) {
    const cmdId = message.subscriptionId;
    let keys: string[];
    const cmd = this.subscriptionCommands.find((command) => command.cmdId === cmdId);
    if (cmd) {
      if (cmd.keys && cmd.keys.length) {
        keys = cmd.keys.split(',');
      }
    }
    message.prepareData(keys);
    if (this.zone) {
     this.zone.run(
       () => {
         this.dataSubject.next(message);
       }
     );
    } else {
      this.dataSubject.next(message);
    }
  }

  public onReconnected() {
    this.reconnectSubject.next();
  }

  public attributeData$(): Observable<Array<AttributeData>> {
    const attributeData = new Array<AttributeData>();
    return this.data$.pipe(
      map((message) => message.updateAttributeData(attributeData))
    );
  }
}
