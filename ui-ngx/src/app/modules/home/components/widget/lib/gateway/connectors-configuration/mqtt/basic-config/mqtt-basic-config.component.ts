///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, forwardRef, ChangeDetectionStrategy } from '@angular/core';
import { NG_VALUE_ACCESSOR, NG_VALIDATORS } from '@angular/forms';
import {
  BrokerConfig,
  MQTTBasicConfig_v3_5_2,
  RequestMappingData,
  RequestMappingValue,
  RequestType, WorkersConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import {
  MqttBasicConfigDirective
} from '@home/components/widget/lib/gateway/connectors-configuration/mqtt/basic-config/mqtt-basic-config.abstract';
import { isDefinedAndNotNull } from '@core/utils';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  SecurityConfigComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/security-config/security-config.component';
import {
  WorkersConfigControlComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mqtt/workers-config-control/workers-config-control.component';
import {
  BrokerConfigControlComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mqtt/broker-config-control/broker-config-control.component';
import {
  MappingTableComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mapping-table/mapping-table.component';

@Component({
  selector: 'tb-mqtt-basic-config',
  templateUrl: './mqtt-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MqttBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MqttBasicConfigComponent),
      multi: true
    }
  ],
  styleUrls: ['./mqtt-basic-config.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
    WorkersConfigControlComponent,
    BrokerConfigControlComponent,
    MappingTableComponent,
  ],
})
export class MqttBasicConfigComponent extends MqttBasicConfigDirective<MQTTBasicConfig_v3_5_2> {

  protected override mapConfigToFormValue(basicConfig: MQTTBasicConfig_v3_5_2): MQTTBasicConfig_v3_5_2 {
    const { broker, mapping = [], requestsMapping } = basicConfig;
    return{
      workers: broker && (broker.maxNumberOfWorkers || broker.maxMessageNumberPerWorker) ? {
        maxNumberOfWorkers: broker.maxNumberOfWorkers,
        maxMessageNumberPerWorker: broker.maxMessageNumberPerWorker,
      } : {} as WorkersConfig,
      mapping: mapping ?? [],
      broker: broker ?? {} as BrokerConfig,
      requestsMapping: this.getRequestDataArray(requestsMapping as Record<RequestType, RequestMappingData[]>),
    };
  }

  protected override getMappedValue(basicConfig: MQTTBasicConfig_v3_5_2): MQTTBasicConfig_v3_5_2 {
    let { broker, workers, mapping, requestsMapping  } = basicConfig || {};

    if (isDefinedAndNotNull(workers.maxNumberOfWorkers) || isDefinedAndNotNull(workers.maxMessageNumberPerWorker)) {
      broker = {
        ...broker,
        ...workers,
      };
    }

    if ((requestsMapping as RequestMappingData[])?.length) {
      requestsMapping = this.getRequestDataObject(requestsMapping as RequestMappingValue[]);
    }

    return { broker, mapping, requestsMapping };
  }
}
