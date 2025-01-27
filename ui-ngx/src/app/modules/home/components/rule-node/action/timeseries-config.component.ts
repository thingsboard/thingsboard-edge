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
  defaultAdvancedPersistenceStrategy,
  maxDeduplicateTimeSecs,
  PersistenceSettings,
  PersistenceSettingsForm,
  PersistenceType,
  PersistenceTypeTranslationMap,
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

  PersistenceType = PersistenceType;
  persistenceStrategies = [PersistenceType.ON_EVERY_MESSAGE, PersistenceType.DEDUPLICATE, PersistenceType.WEBSOCKETS_ONLY];
  PersistenceTypeTranslationMap = PersistenceTypeTranslationMap;

  maxDeduplicateTime = maxDeduplicateTimeSecs

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.timeseriesConfigForm;
  }

  protected validatorTriggers(): string[] {
    return ['persistenceSettings.isAdvanced', 'persistenceSettings.type'];
  }

  protected prepareInputConfig(config: TimeseriesNodeConfiguration): TimeseriesNodeConfigurationForm {
    let persistenceSettings: PersistenceSettingsForm;
    if (config?.persistenceSettings) {
      const isAdvanced = config?.persistenceSettings?.type === PersistenceType.ADVANCED;
      persistenceSettings = {
        type: isAdvanced ? PersistenceType.ON_EVERY_MESSAGE : config.persistenceSettings.type,
        isAdvanced: isAdvanced,
        deduplicationIntervalSecs: config.persistenceSettings?.deduplicationIntervalSecs ?? 60,
        advanced: isAdvanced ? config.persistenceSettings : defaultAdvancedPersistenceStrategy
      }
    } else {
      persistenceSettings = {
        type: PersistenceType.ON_EVERY_MESSAGE,
        isAdvanced: false,
        deduplicationIntervalSecs: 60,
        advanced: defaultAdvancedPersistenceStrategy
      };
    }
    return {
      ...config,
      persistenceSettings: persistenceSettings
    }
  }

  protected prepareOutputConfig(config: TimeseriesNodeConfigurationForm): TimeseriesNodeConfiguration {
    let persistenceSettings: PersistenceSettings;
    if (config.persistenceSettings.isAdvanced) {
      persistenceSettings = {
        ...config.persistenceSettings.advanced,
        type: PersistenceType.ADVANCED
      };
    } else {
      persistenceSettings = {
        type: config.persistenceSettings.type,
        deduplicationIntervalSecs: config.persistenceSettings?.deduplicationIntervalSecs
      };
    }
    return {
      ...config,
      persistenceSettings
    };
  }

  protected onConfigurationSet(config: TimeseriesNodeConfigurationForm) {
    this.timeseriesConfigForm = this.fb.group({
      persistenceSettings: this.fb.group({
        isAdvanced: [config?.persistenceSettings?.isAdvanced ?? false],
        type: [config?.persistenceSettings?.type ?? PersistenceType.ON_EVERY_MESSAGE],
        deduplicationIntervalSecs: [
          {value: config?.persistenceSettings?.deduplicationIntervalSecs ?? 60, disabled: true},
          [Validators.required, Validators.max(maxDeduplicateTimeSecs)]
        ],
        advanced: [{value: null, disabled: true}]
      }),
      defaultTTL: [config?.defaultTTL ?? null, [Validators.required, Validators.min(0)]],
      useServerTs: [config?.useServerTs ?? false]
    });
  }

  protected updateValidators(emitEvent: boolean, _trigger?: string) {
    const persistenceForm = this.timeseriesConfigForm.get('persistenceSettings') as FormGroup;
    const isAdvanced: boolean = persistenceForm.get('isAdvanced').value;
    const type: PersistenceType = persistenceForm.get('type').value;
    if (!isAdvanced && type === PersistenceType.DEDUPLICATE) {
      persistenceForm.get('deduplicationIntervalSecs').enable({emitEvent});
    } else {
      persistenceForm.get('deduplicationIntervalSecs').disable({emitEvent});
    }
    if (isAdvanced) {
      persistenceForm.get('advanced').enable({emitEvent});
    } else {
      persistenceForm.get('advanced').disable({emitEvent});
    }
  }
}
