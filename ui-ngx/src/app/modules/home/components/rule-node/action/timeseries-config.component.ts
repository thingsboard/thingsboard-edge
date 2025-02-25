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

import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import {
  defaultAdvancedProcessingStrategy,
  maxDeduplicateTimeSecs,
  ProcessingSettings,
  ProcessingSettingsForm,
  ProcessingType,
  ProcessingTypeTranslationMap,
  TimeseriesNodeConfiguration,
  TimeseriesNodeConfigurationForm
} from '@home/components/rule-node/action/timeseries-config.models';

@Component({
  selector: 'tb-action-node-timeseries-config',
  templateUrl: './timeseries-config.component.html',
  styleUrls: []
})
export class TimeseriesConfigComponent extends RuleNodeConfigurationComponent {

  timeseriesConfigForm: FormGroup;

  ProcessingType = ProcessingType;
  processingStrategies = [ProcessingType.ON_EVERY_MESSAGE, ProcessingType.DEDUPLICATE, ProcessingType.WEBSOCKETS_ONLY];
  ProcessingTypeTranslationMap = ProcessingTypeTranslationMap;

  maxDeduplicateTime = maxDeduplicateTimeSecs

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.timeseriesConfigForm;
  }

  protected validatorTriggers(): string[] {
    return ['processingSettings.isAdvanced', 'processingSettings.type'];
  }

  protected prepareInputConfig(config: TimeseriesNodeConfiguration): TimeseriesNodeConfigurationForm {
    let processingSettings: ProcessingSettingsForm;
    if (config?.processingSettings) {
      const isAdvanced = config?.processingSettings?.type === ProcessingType.ADVANCED;
      processingSettings = {
        type: isAdvanced ? ProcessingType.ON_EVERY_MESSAGE : config.processingSettings.type,
        isAdvanced: isAdvanced,
        deduplicationIntervalSecs: config.processingSettings?.deduplicationIntervalSecs ?? 60,
        advanced: isAdvanced ? config.processingSettings : defaultAdvancedProcessingStrategy
      }
    } else {
      processingSettings = {
        type: ProcessingType.ON_EVERY_MESSAGE,
        isAdvanced: false,
        deduplicationIntervalSecs: 60,
        advanced: defaultAdvancedProcessingStrategy
      };
    }
    return {
      ...config,
      processingSettings: processingSettings
    }
  }

  protected prepareOutputConfig(config: TimeseriesNodeConfigurationForm): TimeseriesNodeConfiguration {
    let processingSettings: ProcessingSettings;
    if (config.processingSettings.isAdvanced) {
      processingSettings = {
        ...config.processingSettings.advanced,
        type: ProcessingType.ADVANCED
      };
    } else {
      processingSettings = {
        type: config.processingSettings.type,
        deduplicationIntervalSecs: config.processingSettings?.deduplicationIntervalSecs
      };
    }
    return {
      ...config,
      processingSettings
    };
  }

  protected onConfigurationSet(config: TimeseriesNodeConfigurationForm) {
    this.timeseriesConfigForm = this.fb.group({
      processingSettings: this.fb.group({
        isAdvanced: [config?.processingSettings?.isAdvanced ?? false],
        type: [config?.processingSettings?.type ?? ProcessingType.ON_EVERY_MESSAGE],
        deduplicationIntervalSecs: [
          {value: config?.processingSettings?.deduplicationIntervalSecs ?? 60, disabled: true},
          [Validators.required, Validators.max(maxDeduplicateTimeSecs)]
        ],
        advanced: [{value: null, disabled: true}]
      }),
      defaultTTL: [config?.defaultTTL ?? null, [Validators.required, Validators.min(0)]],
      useServerTs: [config?.useServerTs ?? false]
    });
  }

  protected updateValidators(emitEvent: boolean, _trigger?: string) {
    const processingForm = this.timeseriesConfigForm.get('processingSettings') as FormGroup;
    const isAdvanced: boolean = processingForm.get('isAdvanced').value;
    const type: ProcessingType = processingForm.get('type').value;
    if (!isAdvanced && type === ProcessingType.DEDUPLICATE) {
      processingForm.get('deduplicationIntervalSecs').enable({emitEvent});
    } else {
      processingForm.get('deduplicationIntervalSecs').disable({emitEvent});
    }
    if (isAdvanced) {
      processingForm.get('advanced').enable({emitEvent});
    } else {
      processingForm.get('advanced').disable({emitEvent});
    }
  }
}
