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

import { Component } from '@angular/core';
import { FormGroup, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { AttributeScope, telemetryTypeTranslations } from '@app/shared/models/telemetry/telemetry.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  maxDeduplicateTimeSecs,
  ProcessingSettings,
  ProcessingSettingsForm,
  ProcessingType,
  ProcessingTypeTranslationMap
} from '@home/components/rule-node/action/timeseries-config.models';
import {
  AttributeNodeConfiguration,
  AttributeNodeConfigurationForm,
  defaultAttributeAdvancedProcessingStrategy
} from '@home/components/rule-node/action/attributes-config.model';

@Component({
  selector: 'tb-action-node-attributes-config',
  templateUrl: './attributes-config.component.html',
  styleUrls: []
})
export class AttributesConfigComponent extends RuleNodeConfigurationComponent {

  attributeScopeMap = AttributeScope;
  attributeScopes = Object.keys(AttributeScope);
  telemetryTypeTranslationsMap = telemetryTypeTranslations;

  ProcessingType = ProcessingType;
  processingStrategies = [ProcessingType.ON_EVERY_MESSAGE, ProcessingType.DEDUPLICATE, ProcessingType.WEBSOCKETS_ONLY];
  ProcessingTypeTranslationMap = ProcessingTypeTranslationMap;

  maxDeduplicateTime = maxDeduplicateTimeSecs;

  attributesConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.attributesConfigForm;
  }

  protected validatorTriggers(): string[] {
    return ['processingSettings.isAdvanced', 'processingSettings.type'];
  }

  protected prepareInputConfig(config: AttributeNodeConfiguration): AttributeNodeConfigurationForm {
    let processingSettings: ProcessingSettingsForm;
    if (config?.processingSettings) {
      const isAdvanced = config?.processingSettings?.type === ProcessingType.ADVANCED;
      processingSettings = {
        type: isAdvanced ? ProcessingType.ON_EVERY_MESSAGE : config.processingSettings.type,
        isAdvanced: isAdvanced,
        deduplicationIntervalSecs: config.processingSettings?.deduplicationIntervalSecs ?? 60,
        advanced: isAdvanced ? config.processingSettings : defaultAttributeAdvancedProcessingStrategy
      }
    } else {
      processingSettings = {
        type: ProcessingType.ON_EVERY_MESSAGE,
        isAdvanced: false,
        deduplicationIntervalSecs: 60,
        advanced: defaultAttributeAdvancedProcessingStrategy
      };
    }
    return {
      ...config,
      processingSettings: processingSettings
    }
  }

  protected prepareOutputConfig(config: AttributeNodeConfigurationForm): AttributeNodeConfiguration {
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

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.attributesConfigForm = this.fb.group({
      processingSettings: this.fb.group({
        isAdvanced: [configuration?.processingSettings?.isAdvanced ?? false],
        type: [configuration?.processingSettings?.type ?? ProcessingType.ON_EVERY_MESSAGE],
        deduplicationIntervalSecs: [
          {value: configuration?.processingSettings?.deduplicationIntervalSecs ?? 60, disabled: true},
          [Validators.required, Validators.max(maxDeduplicateTimeSecs)]
        ],
        advanced: [{value: null, disabled: true}]
      }),
      scope: [configuration ? configuration.scope : null, [Validators.required]],
      notifyDevice: [configuration ? configuration.notifyDevice : true, []],
      sendAttributesUpdatedNotification: [configuration ? configuration.sendAttributesUpdatedNotification : false, []],
      updateAttributesOnlyOnValueChange: [configuration ? configuration.updateAttributesOnlyOnValueChange : false, []]
    });

    this.attributesConfigForm.get('scope').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value) => {
      if (value !== AttributeScope.SHARED_SCOPE) {
        this.attributesConfigForm.get('notifyDevice').patchValue(false, {emitEvent: false});
      }
      if (value === AttributeScope.CLIENT_SCOPE) {
        this.attributesConfigForm.get('sendAttributesUpdatedNotification').patchValue(false, {emitEvent: false});
      }
      this.attributesConfigForm.get('updateAttributesOnlyOnValueChange').patchValue(false, {emitEvent: false});
    });
  }

  protected updateValidators(emitEvent: boolean, _trigger?: string) {
    const processingForm = this.attributesConfigForm.get('processingSettings') as FormGroup;
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
