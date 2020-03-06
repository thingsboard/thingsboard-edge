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

import { Injectable } from '@angular/core';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { UtilsService } from '@core/services/utils.service';
import { EntityType } from '@app/shared/models/entity-type.models';
import { DataSetHolder, Datasource, DatasourceType, widgetType } from '@shared/models/widget.models';
import { SubscriptionTimewindow } from '@shared/models/time/time.models';
import {
  DatasourceSubscription,
  DatasourceSubscriptionOptions,
  SubscriptionDataKey
} from '@core/api/datasource-subcription';
import { deepClone } from '@core/utils';

export interface DatasourceListener {
  subscriptionType: widgetType;
  subscriptionTimewindow: SubscriptionTimewindow;
  datasource: Datasource;
  entityType: EntityType;
  entityId: string;
  datasourceIndex: number;
  dataUpdated: (data: DataSetHolder, datasourceIndex: number, dataKeyIndex: number, detectChanges: boolean) => void;
  updateRealtimeSubscription: () => SubscriptionTimewindow;
  setRealtimeSubscription: (subscriptionTimewindow: SubscriptionTimewindow) => void;
  datasourceSubscriptionKey?: number;
}

@Injectable({
  providedIn: 'root'
})
export class DatasourceService {

  private subscriptions: {[datasourceSubscriptionKey: string]: DatasourceSubscription} = {};

  constructor(private telemetryService: TelemetryWebsocketService,
              private utils: UtilsService) {}

  public subscribeToDatasource(listener: DatasourceListener) {
    const datasource = listener.datasource;
    if (datasource.type === DatasourceType.entity && (!listener.entityId || !listener.entityType)) {
      return;
    }
    const subscriptionDataKeys: Array<SubscriptionDataKey> = [];
    datasource.dataKeys.forEach((dataKey) => {
      const subscriptionDataKey: SubscriptionDataKey = {
        name: dataKey.name,
        type: dataKey.type,
        funcBody: dataKey.funcBody,
        postFuncBody: dataKey.postFuncBody
      };
      subscriptionDataKeys.push(subscriptionDataKey);
    });

    const datasourceSubscriptionOptions: DatasourceSubscriptionOptions = {
      datasourceType: datasource.type,
      dataKeys: subscriptionDataKeys,
      type: listener.subscriptionType
    };

    if (listener.subscriptionType === widgetType.timeseries) {
      datasourceSubscriptionOptions.subscriptionTimewindow = deepClone(listener.subscriptionTimewindow);
    }
    if (datasourceSubscriptionOptions.datasourceType === DatasourceType.entity) {
      datasourceSubscriptionOptions.entityType = listener.entityType;
      datasourceSubscriptionOptions.entityId = listener.entityId;
    }
    listener.datasourceSubscriptionKey = this.utils.objectHashCode(datasourceSubscriptionOptions);
    let subscription: DatasourceSubscription;
    if (this.subscriptions[listener.datasourceSubscriptionKey]) {
      subscription = this.subscriptions[listener.datasourceSubscriptionKey];
      subscription.syncListener(listener);
    } else {
      subscription = new DatasourceSubscription(datasourceSubscriptionOptions,
                                                this.telemetryService, this.utils);
      this.subscriptions[listener.datasourceSubscriptionKey] = subscription;
      subscription.start();
    }
    subscription.addListener(listener);
  }

  public unsubscribeFromDatasource(listener: DatasourceListener) {
    if (listener.datasourceSubscriptionKey) {
      const subscription = this.subscriptions[listener.datasourceSubscriptionKey];
      if (subscription) {
        subscription.removeListener(listener);
        if (!subscription.hasListeners()) {
          subscription.unsubscribe();
          delete this.subscriptions[listener.datasourceSubscriptionKey];
        }
      }
      listener.datasourceSubscriptionKey = null;
    }
  }
}
