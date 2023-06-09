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

import { DataKey, DataSetHolder, Datasource, DatasourceType, widgetType } from '@shared/models/widget.models';
import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import { EntityData, EntityDataPageLink, KeyFilter } from '@shared/models/query/query.models';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { Injectable } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { UtilsService } from '@core/services/utils.service';
import { deepClone } from '@core/utils';
import {
  EntityDataSubscription,
  EntityDataSubscriptionOptions,
  SubscriptionDataKey
} from '@core/api/entity-data-subscription';
import { Observable, of } from 'rxjs';

export interface EntityDataListener {
  subscriptionType: widgetType;
  useTimewindow?: boolean;
  subscriptionTimewindow?: SubscriptionTimewindow;
  latestTsOffset?: number;
  configDatasource: Datasource;
  configDatasourceIndex: number;
  dataLoaded: (pageData: PageData<EntityData>,
               data: Array<Array<DataSetHolder>>,
               datasourceIndex: number, pageLink: EntityDataPageLink) => void;
  dataUpdated: (data: DataSetHolder, datasourceIndex: number, dataIndex: number, dataKeyIndex: number,
                detectChanges: boolean, isLatest: boolean) => void;
  initialPageDataChanged?: (nextPageData: PageData<EntityData>) => void;
  forceReInit?: () => void;
  updateRealtimeSubscription?: () => SubscriptionTimewindow;
  setRealtimeSubscription?: (subscriptionTimewindow: SubscriptionTimewindow) => void;
  subscriptionOptions?: EntityDataSubscriptionOptions;
  subscription?: EntityDataSubscription;
}

export interface EntityDataLoadResult {
  pageData: PageData<EntityData>;
  data: Array<Array<DataSetHolder>>;
  datasourceIndex: number;
  pageLink: EntityDataPageLink;
}

@Injectable({
  providedIn: 'root'
})
export class EntityDataService {

  constructor(private telemetryService: TelemetryWebsocketService,
              private utils: UtilsService) {}

  private static isUnresolvedDatasource(datasource: Datasource, pageLink: EntityDataPageLink): boolean {
    if (datasource.type === DatasourceType.entity) {
      return !datasource.entityFilter || !pageLink;
    } else if (datasource.type === DatasourceType.entityCount) {
      return !datasource.entityFilter;
    } else {
      return false;
    }
  }

  private static toSubscriptionDataKey(dataKey: DataKey, latest: boolean): SubscriptionDataKey {
    return {
      name: dataKey.name,
      type: dataKey.type,
      aggregationType: dataKey.aggregationType,
      comparisonEnabled: dataKey.comparisonEnabled,
      timeForComparison: dataKey.timeForComparison,
      comparisonCustomIntervalValue: dataKey.comparisonCustomIntervalValue,
      comparisonResultType: dataKey.comparisonResultType,
      funcBody: dataKey.funcBody,
      postFuncBody: dataKey.postFuncBody,
      latest
    };
  }

  public prepareSubscription(listener: EntityDataListener,
                             ignoreDataUpdateOnIntervalTick = false): Observable<EntityDataLoadResult> {
    const datasource = listener.configDatasource;
    listener.subscriptionOptions = this.createSubscriptionOptions(
      datasource,
      listener.subscriptionType,
      datasource.pageLink,
      datasource.keyFilters,
      null,
      false,
      ignoreDataUpdateOnIntervalTick);
    if (EntityDataService.isUnresolvedDatasource(datasource, datasource.pageLink)) {
      return of(null);
    }
    listener.subscription = new EntityDataSubscription(listener, this.telemetryService, this.utils);
    return listener.subscription.subscribe();
  }

  public startSubscription(listener: EntityDataListener) {
    if (listener.subscription) {
      if (listener.useTimewindow) {
        listener.subscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
      }
      if (listener.subscriptionType === widgetType.timeseries || listener.subscriptionType === widgetType.latest) {
        listener.subscriptionOptions.latestTsOffset = listener.latestTsOffset;
      }
      listener.subscription.start();
    }
  }

  public subscribeForPaginatedData(listener: EntityDataListener,
                                   pageLink: EntityDataPageLink,
                                   keyFilters: KeyFilter[],
                                   ignoreDataUpdateOnIntervalTick = false): Observable<EntityDataLoadResult> {
    const datasource = listener.configDatasource;
    listener.subscriptionOptions = this.createSubscriptionOptions(
      datasource,
      listener.subscriptionType,
      pageLink,
      datasource.keyFilters,
      keyFilters,
      true,
      ignoreDataUpdateOnIntervalTick);
    if (EntityDataService.isUnresolvedDatasource(datasource, pageLink)) {
      listener.dataLoaded(emptyPageData<EntityData>(), [],
        listener.configDatasourceIndex, listener.subscriptionOptions.pageLink);
      return of(null);
    }
    listener.subscription = new EntityDataSubscription(listener, this.telemetryService, this.utils);
    if (listener.useTimewindow) {
      listener.subscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
    }
    if (listener.subscriptionType === widgetType.timeseries || listener.subscriptionType === widgetType.latest) {
      listener.subscriptionOptions.latestTsOffset = listener.latestTsOffset;
    }
    return listener.subscription.subscribe();
  }

  public stopSubscription(listener: EntityDataListener) {
    if (listener.subscription) {
      listener.subscription.unsubscribe();
    }
  }

  private createSubscriptionOptions(datasource: Datasource,
                                    subscriptionType: widgetType,
                                    pageLink: EntityDataPageLink,
                                    keyFilters: KeyFilter[],
                                    additionalKeyFilters: KeyFilter[],
                                    isPaginatedDataSubscription: boolean,
                                    ignoreDataUpdateOnIntervalTick: boolean): EntityDataSubscriptionOptions {
    const subscriptionDataKeys: Array<SubscriptionDataKey> = [];
    datasource.dataKeys.forEach((dataKey) => {
      subscriptionDataKeys.push(EntityDataService.toSubscriptionDataKey(dataKey, false));
    });
    if (datasource.latestDataKeys) {
      datasource.latestDataKeys.forEach((dataKey) => {
        subscriptionDataKeys.push(EntityDataService.toSubscriptionDataKey(dataKey, true));
      });
    }
    const entityDataSubscriptionOptions: EntityDataSubscriptionOptions = {
      datasourceType: datasource.type,
      dataKeys: subscriptionDataKeys,
      type: subscriptionType
    };
    if (entityDataSubscriptionOptions.datasourceType === DatasourceType.entity ||
      entityDataSubscriptionOptions.datasourceType === DatasourceType.entityCount ||
      entityDataSubscriptionOptions.datasourceType === DatasourceType.alarmCount) {
      entityDataSubscriptionOptions.entityFilter = datasource.entityFilter;
      entityDataSubscriptionOptions.alarmFilter = datasource.alarmFilter;
      entityDataSubscriptionOptions.keyFilters = keyFilters;
      entityDataSubscriptionOptions.additionalKeyFilters = additionalKeyFilters;
      if (entityDataSubscriptionOptions.datasourceType === DatasourceType.entity) {
        entityDataSubscriptionOptions.pageLink = pageLink;
      }
    }
    entityDataSubscriptionOptions.isPaginatedDataSubscription = isPaginatedDataSubscription;
    entityDataSubscriptionOptions.ignoreDataUpdateOnIntervalTick = ignoreDataUpdateOnIntervalTick;
    return entityDataSubscriptionOptions;
  }
}
