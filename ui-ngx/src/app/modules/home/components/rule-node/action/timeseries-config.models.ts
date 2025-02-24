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

import { DAY, SECOND } from '@shared/models/time/time.models';

export const maxDeduplicateTimeSecs = DAY / SECOND;

export interface TimeseriesNodeConfiguration {
  processingSettings: ProcessingSettings;
  defaultTTL: number;
  useServerTs: boolean;
}

export interface TimeseriesNodeConfigurationForm extends Omit<TimeseriesNodeConfiguration, 'processingSettings'> {
  processingSettings: ProcessingSettingsForm
}

export type ProcessingSettings = BasicProcessingSettings & Partial<DeduplicateProcessingStrategy> & Partial<AdvancedProcessingStrategy>;

export type ProcessingSettingsForm = Omit<ProcessingSettings, keyof AdvancedProcessingStrategy> & {
  isAdvanced: boolean;
  advanced?: Partial<AdvancedProcessingStrategy>;
  type: ProcessingType;
};

export enum ProcessingType {
  ON_EVERY_MESSAGE = 'ON_EVERY_MESSAGE',
  DEDUPLICATE = 'DEDUPLICATE',
  WEBSOCKETS_ONLY = 'WEBSOCKETS_ONLY',
  ADVANCED = 'ADVANCED',
  SKIP = 'SKIP'
}

export const ProcessingTypeTranslationMap = new Map<ProcessingType, string>([
  [ProcessingType.ON_EVERY_MESSAGE, 'rule-node-config.save-time-series.strategy-type.every-message'],
  [ProcessingType.DEDUPLICATE, 'rule-node-config.save-time-series.strategy-type.deduplicate'],
  [ProcessingType.WEBSOCKETS_ONLY, 'rule-node-config.save-time-series.strategy-type.web-sockets-only'],
  [ProcessingType.SKIP, 'rule-node-config.save-time-series.strategy-type.skip'],
])

export interface BasicProcessingSettings {
  type: ProcessingType;
}

export interface DeduplicateProcessingStrategy extends BasicProcessingSettings{
  deduplicationIntervalSecs: number;
}

export interface AdvancedProcessingStrategy extends BasicProcessingSettings{
  timeseries: AdvancedProcessingConfig;
  latest: AdvancedProcessingConfig;
  webSockets: AdvancedProcessingConfig;
}

export type AdvancedProcessingConfig = WithOptional<DeduplicateProcessingStrategy, 'deduplicationIntervalSecs'>;

export const defaultAdvancedProcessingConfig: AdvancedProcessingConfig = {
  type: ProcessingType.ON_EVERY_MESSAGE
}

export const defaultAdvancedPersistenceStrategy: Omit<AdvancedProcessingStrategy, 'type'> = {
  timeseries: defaultAdvancedProcessingConfig,
  latest: defaultAdvancedProcessingConfig,
  webSockets: defaultAdvancedProcessingConfig,
}
