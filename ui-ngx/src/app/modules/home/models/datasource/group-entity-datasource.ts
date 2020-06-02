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

import { EntityBooleanFunction } from '@home/models/entity/entities-table-config.models';
import { EntityGroupColumn, EntityGroupColumnType, ShortEntityView } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntitiesDataSource } from '@home/models/datasource/entity-datasource';
import { deepClone } from '@core/utils';
import { PageLink } from '@shared/models/page/page-link';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import {
  AttributeScope,
  LatestTelemetry,
  SubscriptionUpdate,
  TelemetrySubscriber,
  TelemetryType
} from '@shared/models/telemetry/telemetry.models';
import { NgZone } from '@angular/core';
import { CollectionViewer } from '@angular/cdk/collections';

export class GroupEntitiesDataSource extends EntitiesDataSource<ShortEntityView> {

  private columnKeyToPropertyMap: {[columnKey: string]: string} = {};
  private propertyToColumnIndexMap: {[property: string]: number[]} = {};
  private telemetryKeysToPropertiesMap = new Map<TelemetryType, {[key: string]: string}>(
    [
      [AttributeScope.CLIENT_SCOPE, {}],
      [AttributeScope.SHARED_SCOPE, {}],
      [AttributeScope.SERVER_SCOPE, {}],
      [LatestTelemetry.LATEST_TELEMETRY, {}]
    ]
  );
  private telemetryToKeysMap = new Map<TelemetryType, string[]>();
  private telemetrySubscribers: TelemetrySubscriber[] = [];

  constructor(private columns: EntityGroupColumn[],
              private entityGroupId: string,
              private entityGroupService: EntityGroupService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone,
              protected selectionEnabledFunction: EntityBooleanFunction<ShortEntityView>,
              protected dataLoadedFunction: (col?: number, row?: number) => void) {
    super(
      (pageLink =>
        {
          if (pageLink.sortOrder && pageLink.sortOrder.property) {
            const property = this.columnKeyToPropertyMap[pageLink.sortOrder.property];
            let sortOrder = null;
            if (property) {
              sortOrder = deepClone(pageLink.sortOrder);
              sortOrder.property = property;
            }
            pageLink = new PageLink(pageLink.pageSize, pageLink.page, pageLink.textSearch, sortOrder);
          }
          return this.entityGroupService.getEntityGroupEntities<ShortEntityView>(this.entityGroupId, pageLink)
        }),
      selectionEnabledFunction,
      dataLoadedFunction
    );
    columns.forEach((column, index) => {
      this.columnKeyToPropertyMap[column.columnKey] = column.property;
      let columnIndexes = this.propertyToColumnIndexMap[column.property];
      if (!columnIndexes) {
        columnIndexes = [];
        this.propertyToColumnIndexMap[column.property] = columnIndexes;
      }
      columnIndexes.push(index);
      const telemetryType = this.entityGroupColumnTypeToTelemetryType(column.type);
      if (telemetryType !== null) {
        const keyToPropertiesMap = this.telemetryKeysToPropertiesMap.get(telemetryType);
        keyToPropertiesMap[column.key] = column.property;
      }
    });
    this.telemetryKeysToPropertiesMap.forEach((keyToPropertiesMap, telemetryType) => {
      const keys = Object.keys(keyToPropertiesMap);
      if (keys && keys.length) {
        this.telemetryToKeysMap.set(telemetryType, keys);
      }
    });
  }

  disconnect(collectionViewer: CollectionViewer): void {
    super.disconnect(collectionViewer);
    this.clearSubscribers();
  }

  protected onEntities(entities: ShortEntityView[]) {
    super.onEntities(entities);
    this.clearSubscribers(true);
    this.createSubscribers(entities);
    this.telemetryWsService.publishCommands();
  }

  private clearSubscribers(skipPublish?: boolean) {
    this.telemetryWsService.batchUnsubscribe(this.telemetrySubscribers);
    if (this.telemetrySubscribers.length) {
      this.telemetrySubscribers.length = 0;
      if (!skipPublish) {
        this.telemetryWsService.publishCommands();
      }
    }
  }

  private createSubscribers(entities: ShortEntityView[]) {
    entities.forEach((entity, row) => {
      this.telemetryToKeysMap.forEach((keys, telemetryType) => {
        const keyToPropertiesMap = this.telemetryKeysToPropertiesMap.get(telemetryType);
        const subscriber = this.createSubscriber(entity, row, telemetryType, keys, keyToPropertiesMap, entities);
        this.telemetrySubscribers.push(subscriber);
      });
    });
    this.telemetryWsService.batchSubscribe(this.telemetrySubscribers);
  }

  private createSubscriber(entity: ShortEntityView, row: number, telemetryType: TelemetryType, keys: string[],
                           keyToPropertiesMap: {[key: string]: string}, entities: ShortEntityView[]): TelemetrySubscriber {
    const subscriber = TelemetrySubscriber.createEntityAttributesSubscription(this.telemetryWsService,
      entity.id,
      telemetryType,
      this.zone,
      keys);
    subscriber.data$.subscribe((update) => {
      this.onData(entity, row, update, keyToPropertiesMap, entities);
    });
    return subscriber;
  }

  private onData(entity: ShortEntityView, row: number, update: SubscriptionUpdate,
                 keyToPropertiesMap: {[key: string]: string},
                 entities: ShortEntityView[]) {
    const data = update.data;
    const updatedColumns: number[] = [];
    for (const key of Object.keys(data)) {
      const keyData = data[key];
      if (keyData && keyData.length) {
        const value = keyData[0][1];
        const property = keyToPropertiesMap[key];
        if (property) {
          if (entity[property] !== value) {
            entity[property] = value;
            updatedColumns.push(...this.propertyToColumnIndexMap[property]);
          }
        }
      }
    }
    if (updatedColumns.length) {
      updatedColumns.forEach((col) => {
        this.dataLoadedFunction(col, row);
      });
      super.onEntities(entities);
    }
  }

  private entityGroupColumnTypeToTelemetryType(type: EntityGroupColumnType): TelemetryType {
    switch (type) {
      case EntityGroupColumnType.CLIENT_ATTRIBUTE:
        return AttributeScope.CLIENT_SCOPE;
      case EntityGroupColumnType.SHARED_ATTRIBUTE:
        return AttributeScope.SHARED_SCOPE;
      case EntityGroupColumnType.SERVER_ATTRIBUTE:
        return AttributeScope.SERVER_SCOPE;
      case EntityGroupColumnType.TIMESERIES:
        return LatestTelemetry.LATEST_TELEMETRY;
      case EntityGroupColumnType.ENTITY_FIELD:
        return null;
    }
  }

}
