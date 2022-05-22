///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { BaseData, HasId } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import {QueueId} from '@shared/models/id/queue-id';

export enum ServiceType {
  TB_CORE = 'TB_CORE',
  TB_RULE_ENGINE = 'TB_RULE_ENGINE',
  TB_TRANSPORT = 'TB_TRANSPORT',
  JS_EXECUTOR = 'JS_EXECUTOR'
}

export enum QueueSubmitStrategyTypes {
  SEQUENTIAL_BY_ORIGINATOR = 'SEQUENTIAL_BY_ORIGINATOR',
  SEQUENTIAL_BY_TENANT = 'SEQUENTIAL_BY_TENANT',
  SEQUENTIAL = 'SEQUENTIAL',
  BURST = 'BURST',
  BATCH = 'BATCH'
}

export enum QueueProcessingStrategyTypes {
  RETRY_FAILED_AND_TIMED_OUT = 'RETRY_FAILED_AND_TIMED_OUT',
  SKIP_ALL_FAILURES = 'SKIP_ALL_FAILURES',
  SKIP_ALL_FAILURES_AND_TIMED_OUT = 'SKIP_ALL_FAILURES_AND_TIMED_OUT',
  RETRY_ALL = 'RETRY_ALL',
  RETRY_FAILED = 'RETRY_FAILED',
  RETRY_TIMED_OUT = 'RETRY_TIMED_OUT'
}

export interface QueueInfo extends BaseData<QueueId> {
  name: string;
  packProcessingTimeout: number;
  partitions: number;
  consumerPerPartition: boolean;
  pollInterval: number;
  processingStrategy: {
    type: QueueProcessingStrategyTypes,
    retries: number,
    failurePercentage: number,
    pauseBetweenRetries: number,
    maxPauseBetweenRetries: number
  };
  submitStrategy: {
    type: QueueSubmitStrategyTypes,
    batchSize: number,
  };
  tenantId?: TenantId;
  topic: string;
  additionalInfo: {
    description?: string;
  };
}
