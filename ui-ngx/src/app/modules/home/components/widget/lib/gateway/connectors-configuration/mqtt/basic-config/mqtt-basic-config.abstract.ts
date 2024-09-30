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

import { Directive } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  BrokerConfig,
  MappingType,
  MQTTBasicConfig,
  MQTTBasicConfig_v3_5_2,
  RequestMappingData,
  RequestMappingValue,
  RequestType,
  WorkersConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isObject } from '@core/utils';
import {
  GatewayConnectorBasicConfigDirective
} from '@home/components/widget/lib/gateway/abstract/gateway-connector-basic-config.abstract';

@Directive()
export abstract class MqttBasicConfigDirective<BasicConfig>
  extends GatewayConnectorBasicConfigDirective<MQTTBasicConfig_v3_5_2, BasicConfig> {

  MappingType = MappingType;

  protected override initBasicFormGroup(): FormGroup {
    return this.fb.group({
      mapping: [],
      requestsMapping: [],
      broker: [],
      workers: [],
    });
  }

  protected getRequestDataArray(value: Record<RequestType, RequestMappingData[]>): RequestMappingData[] {
    const mappingConfigs = [];

    if (isObject(value)) {
      Object.keys(value).forEach((configKey: string) => {
        for (const mapping of value[configKey]) {
          mappingConfigs.push({
            requestType: configKey,
            requestValue: mapping
          });
        }
      });
    }

    return mappingConfigs;
  }

  protected getRequestDataObject(array: RequestMappingValue[]): Record<RequestType, RequestMappingValue[]> {
    return array.reduce((result, { requestType, requestValue }) => {
      result[requestType].push(requestValue);
      return result;
    }, {
      connectRequests: [],
      disconnectRequests: [],
      attributeRequests: [],
      attributeUpdates: [],
      serverSideRpc: [],
    });
  }

  protected getBrokerMappedValue(broker: BrokerConfig, workers: WorkersConfig): BrokerConfig {
    return {
      ...broker,
      maxNumberOfWorkers: workers.maxNumberOfWorkers ?? 100,
      maxMessageNumberPerWorker: workers.maxMessageNumberPerWorker ?? 10,
    };
  }

  writeValue(basicConfig: BasicConfig): void {
    this.basicFormGroup.setValue(this.mapConfigToFormValue(basicConfig), { emitEvent: false });
  }

  protected abstract override mapConfigToFormValue(config: BasicConfig): MQTTBasicConfig_v3_5_2;
  protected abstract override getMappedValue(config: MQTTBasicConfig): BasicConfig;
}
