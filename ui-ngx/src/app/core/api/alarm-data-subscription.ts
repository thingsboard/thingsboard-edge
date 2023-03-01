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

import {
  AlarmDataCmd,
  DataKeyType,
  TelemetryService,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import { DatasourceType } from '@shared/models/widget.models';
import {
  AlarmData,
  AlarmDataPageLink,
  EntityFilter,
  EntityKey,
  EntityKeyType,
  KeyFilter
} from '@shared/models/query/query.models';
import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import { AlarmDataListener } from '@core/api/alarm-data.service';
import { PageData } from '@shared/models/page/page-data';
import { deepClone, isDefined, isDefinedAndNotNull, isObject } from '@core/utils';
import { simulatedAlarm } from '@shared/models/alarm.models';

export interface AlarmSubscriptionDataKey {
  name: string;
  type: DataKeyType;
}

export interface AlarmDataSubscriptionOptions {
  datasourceType: DatasourceType;
  dataKeys: Array<AlarmSubscriptionDataKey>;
  entityFilter?: EntityFilter;
  pageLink?: AlarmDataPageLink;
  keyFilters?: Array<KeyFilter>;
  additionalKeyFilters?: Array<KeyFilter>;
  subscriptionTimewindow?: SubscriptionTimewindow;
}

export class AlarmDataSubscription {

  private alarmDataSubscriptionOptions = this.listener.alarmDataSubscriptionOptions;
  private datasourceType: DatasourceType = this.alarmDataSubscriptionOptions.datasourceType;

  private history: boolean;
  private realtime: boolean;

  private subscriber: TelemetrySubscriber;
  private alarmDataCommand: AlarmDataCmd;

  private pageData: PageData<AlarmData>;
  private prematureUpdates: Array<Array<AlarmData>>;
  private alarmIdToDataIndex: {[id: string]: number};

  private subsTw: SubscriptionTimewindow;

  constructor(private listener: AlarmDataListener,
              private telemetryService: TelemetryService) {
  }

  public unsubscribe() {
    if (this.datasourceType === DatasourceType.entity) {
      if (this.subscriber) {
        this.subscriber.unsubscribe();
        this.subscriber = null;
      }
    }
  }

  public subscribe() {
    this.subsTw = this.alarmDataSubscriptionOptions.subscriptionTimewindow;
    this.history = this.alarmDataSubscriptionOptions.subscriptionTimewindow &&
      isObject(this.alarmDataSubscriptionOptions.subscriptionTimewindow.fixedWindow);
    this.realtime = this.alarmDataSubscriptionOptions.subscriptionTimewindow &&
      isDefinedAndNotNull(this.alarmDataSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs);
    if (this.datasourceType === DatasourceType.entity) {
      this.subscriber = new TelemetrySubscriber(this.telemetryService);
      this.alarmDataCommand = new AlarmDataCmd();

      const alarmFields: Array<EntityKey> =
        this.alarmDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.alarm).map(
          dataKey => ({ type: EntityKeyType.ALARM_FIELD, key: dataKey.name })
        );

      const entityFields: Array<EntityKey> =
        this.alarmDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.entityField).map(
          dataKey => ({ type: EntityKeyType.ENTITY_FIELD, key: dataKey.name })
        );

      const attrFields = this.alarmDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.attribute).map(
        dataKey => ({ type: EntityKeyType.ATTRIBUTE, key: dataKey.name })
      );
      const tsFields = this.alarmDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.timeseries).map(
        dataKey => ({ type: EntityKeyType.TIME_SERIES, key: dataKey.name })
      );
      const latestValues = attrFields.concat(tsFields);

      let keyFilters = this.alarmDataSubscriptionOptions.keyFilters;
      if (this.alarmDataSubscriptionOptions.additionalKeyFilters) {
        if (keyFilters) {
          keyFilters = keyFilters.concat(this.alarmDataSubscriptionOptions.additionalKeyFilters);
        } else {
          keyFilters = this.alarmDataSubscriptionOptions.additionalKeyFilters;
        }
      }
      this.alarmDataCommand.query = {
        entityFilter: this.alarmDataSubscriptionOptions.entityFilter,
        pageLink: deepClone(this.alarmDataSubscriptionOptions.pageLink),
        keyFilters,
        alarmFields,
        entityFields,
        latestValues
      };
      if (this.history) {
        this.alarmDataCommand.query.pageLink.startTs = this.subsTw.fixedWindow.startTimeMs;
        this.alarmDataCommand.query.pageLink.endTs = this.subsTw.fixedWindow.endTimeMs;
      } else {
        this.alarmDataCommand.query.pageLink.timeWindow = this.subsTw.realtimeWindowMs;
      }

      this.subscriber.setTsOffset(this.subsTw.tsOffset);
      this.subscriber.subscriptionCommands.push(this.alarmDataCommand);

      this.subscriber.alarmData$.subscribe((alarmDataUpdate) => {
        if (alarmDataUpdate.data) {
          this.onPageData(alarmDataUpdate.data, alarmDataUpdate.allowedEntities, alarmDataUpdate.totalEntities);
          if (this.prematureUpdates) {
            for (const update of this.prematureUpdates) {
              this.onDataUpdate(update);
            }
            this.prematureUpdates = null;
          }
        } else if (alarmDataUpdate.update) {
          if (!this.pageData) {
            if (!this.prematureUpdates) {
              this.prematureUpdates = [];
            }
            this.prematureUpdates.push(alarmDataUpdate.update);
          } else {
            this.onDataUpdate(alarmDataUpdate.update);
          }
        }
      });

      this.subscriber.subscribe();

    } else if (this.datasourceType === DatasourceType.function) {
      const alarm = deepClone(simulatedAlarm);
      alarm.createdTime += this.subsTw.tsOffset;
      alarm.startTs += this.subsTw.tsOffset;
      const pageData: PageData<AlarmData> = {
        data: [{...alarm, entityId: '1', latest: {}}],
        hasNext: false,
        totalElements: 1,
        totalPages: 1
      };
      this.onPageData(pageData, 1024, 1);
    }
  }

  private resetData() {
    this.alarmIdToDataIndex = {};
    for (let dataIndex = 0; dataIndex < this.pageData.data.length; dataIndex++) {
      const alarmData = this.pageData.data[dataIndex];
      this.alarmIdToDataIndex[alarmData.id.id] = dataIndex;
    }
  }

  private onPageData(pageData: PageData<AlarmData>, allowedEntities: number, totalEntities: number) {
    this.pageData = pageData;
    this.resetData();
    this.listener.alarmsLoaded(pageData, allowedEntities, totalEntities);
  }

  private onDataUpdate(update: Array<AlarmData>) {
    for (const alarmData of update) {
      const dataIndex = this.alarmIdToDataIndex[alarmData.id.id];
      if (isDefined(dataIndex) && dataIndex >= 0) {
        this.pageData.data[dataIndex] = alarmData;
      }
    }
    this.listener.alarmsUpdated(update, this.pageData);
  }

}
