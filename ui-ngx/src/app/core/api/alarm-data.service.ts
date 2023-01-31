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

import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import { Datasource, DatasourceType } from '@shared/models/widget.models';
import { PageData } from '@shared/models/page/page-data';
import { AlarmData, AlarmDataPageLink, KeyFilter } from '@shared/models/query/query.models';
import { Injectable } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import {
  AlarmDataSubscription,
  AlarmDataSubscriptionOptions,
  AlarmSubscriptionDataKey
} from '@core/api/alarm-data-subscription';
import { deepClone } from '@core/utils';

export interface AlarmDataListener {
  subscriptionTimewindow?: SubscriptionTimewindow;
  alarmSource: Datasource;
  alarmsLoaded: (pageData: PageData<AlarmData>, allowedEntities: number, totalEntities: number) => void;
  alarmsUpdated: (update: Array<AlarmData>, pageData: PageData<AlarmData>) => void;
  alarmDataSubscriptionOptions?: AlarmDataSubscriptionOptions;
  subscription?: AlarmDataSubscription;
}

@Injectable({
  providedIn: 'root'
})
export class AlarmDataService {

  constructor(private telemetryService: TelemetryWebsocketService) {}


  public subscribeForAlarms(listener: AlarmDataListener,
                            pageLink: AlarmDataPageLink,
                            keyFilters: KeyFilter[]) {
    const alarmSource = listener.alarmSource;
    listener.alarmDataSubscriptionOptions = this.createAlarmSubscriptionOptions(listener, pageLink, keyFilters);
    if (alarmSource.type === DatasourceType.entity && (!alarmSource.entityFilter || !pageLink)) {
      return;
    }
    listener.subscription = new AlarmDataSubscription(listener, this.telemetryService);
    return listener.subscription.subscribe();
  }

  public stopSubscription(listener: AlarmDataListener) {
    if (listener.subscription) {
      listener.subscription.unsubscribe();
    }
  }

  private createAlarmSubscriptionOptions(listener: AlarmDataListener,
                                         pageLink: AlarmDataPageLink,
                                         additionalKeyFilters: KeyFilter[]): AlarmDataSubscriptionOptions {
    const alarmSource = listener.alarmSource;
    const alarmSubscriptionDataKeys: Array<AlarmSubscriptionDataKey> = [];
    alarmSource.dataKeys.forEach((dataKey) => {
      const alarmSubscriptionDataKey: AlarmSubscriptionDataKey = {
        name: dataKey.name,
        type: dataKey.type
      };
      alarmSubscriptionDataKeys.push(alarmSubscriptionDataKey);
    });
    const alarmDataSubscriptionOptions: AlarmDataSubscriptionOptions = {
      datasourceType: alarmSource.type,
      dataKeys: alarmSubscriptionDataKeys,
      subscriptionTimewindow: deepClone(listener.subscriptionTimewindow)
    };
    if (alarmDataSubscriptionOptions.datasourceType === DatasourceType.entity) {
      alarmDataSubscriptionOptions.entityFilter = alarmSource.entityFilter;
      alarmDataSubscriptionOptions.pageLink = pageLink;
      alarmDataSubscriptionOptions.keyFilters = alarmSource.keyFilters;
      alarmDataSubscriptionOptions.additionalKeyFilters = additionalKeyFilters;
    }
    return alarmDataSubscriptionOptions;
  }

}
