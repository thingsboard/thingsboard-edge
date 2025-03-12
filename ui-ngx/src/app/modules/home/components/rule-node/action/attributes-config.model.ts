///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { BasicProcessingSettings, ProcessingType } from '@home/components/rule-node/action/timeseries-config.models';

export interface AttributeNodeConfiguration {
  processingSettings: AttributeProcessingSettings;
  scope: AttributeScope;
  notifyDevice: boolean;
  sendAttributesUpdatedNotification: boolean;
  updateAttributesOnlyOnValueChange: boolean;
}

export interface AttributeNodeConfigurationForm extends Omit<AttributeNodeConfiguration, 'processingSettings'> {
  processingSettings: AttributeProcessingSettingsForm
}

export type AttributeProcessingSettings = BasicProcessingSettings & Partial<AttributeDeduplicateProcessingStrategy> & Partial<AttributeAdvancedProcessingStrategy>;

export type AttributeProcessingSettingsForm = Omit<AttributeProcessingSettings, keyof AttributeAdvancedProcessingStrategy> & {
  isAdvanced: boolean;
  advanced?: Partial<AttributeAdvancedProcessingStrategy>;
  type: ProcessingType;
};

export interface AttributeDeduplicateProcessingStrategy extends BasicProcessingSettings {
  deduplicationIntervalSecs: number;
}

export interface AttributeAdvancedProcessingStrategy extends BasicProcessingSettings {
  attributes: AttributeAdvancedProcessingConfig;
  webSockets: AttributeAdvancedProcessingConfig;
  calculatedFields: AttributeAdvancedProcessingConfig;
}

export type AttributeAdvancedProcessingConfig = WithOptional<AttributeDeduplicateProcessingStrategy, 'deduplicationIntervalSecs'>;

export const defaultAdvancedProcessingConfig: AttributeAdvancedProcessingConfig = {
  type: ProcessingType.ON_EVERY_MESSAGE
}

export const defaultAttributeAdvancedProcessingStrategy: Omit<AttributeAdvancedProcessingStrategy, 'type'> = {
  attributes: defaultAdvancedProcessingConfig,
  webSockets: defaultAdvancedProcessingConfig,
  calculatedFields: defaultAdvancedProcessingConfig,
}
