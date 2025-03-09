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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import {
  PerimeterType,
  perimeterTypeTranslations,
  PresenceMonitoringStrategiesData,
  RangeUnit,
  rangeUnitTranslations,
  TimeUnit,
  timeUnitTranslations
} from '../rule-node-config.models';

@Component({
  selector: 'tb-action-node-gps-geofencing-config',
  templateUrl: './gps-geo-action-config.component.html',
  styleUrls: ['./gps-geo-action-config.component.scss']
})
export class GpsGeoActionConfigComponent extends RuleNodeConfigurationComponent {

  geoActionConfigForm: UntypedFormGroup;

  perimeterType = PerimeterType;
  perimeterTypes = Object.keys(PerimeterType);
  perimeterTypeTranslationMap = perimeterTypeTranslations;

  rangeUnits = Object.keys(RangeUnit);
  rangeUnitTranslationMap = rangeUnitTranslations;

  presenceMonitoringStrategies = PresenceMonitoringStrategiesData;
  presenceMonitoringStrategyKeys = Array.from(this.presenceMonitoringStrategies.keys());

  timeUnits = Object.keys(TimeUnit);
  timeUnitsTranslationMap = timeUnitTranslations;

  public defaultPaddingEnable = true;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.geoActionConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.geoActionConfigForm = this.fb.group({
      reportPresenceStatusOnEachMessage: [configuration ? configuration.reportPresenceStatusOnEachMessage : true,
        [Validators.required]],
      latitudeKeyName: [configuration ? configuration.latitudeKeyName : null, [Validators.required]],
      longitudeKeyName: [configuration ? configuration.longitudeKeyName : null, [Validators.required]],
      perimeterType: [configuration ? configuration.perimeterType : null, [Validators.required]],
      fetchPerimeterInfoFromMessageMetadata: [configuration ? configuration.fetchPerimeterInfoFromMessageMetadata : false, []],
      perimeterKeyName: [configuration ? configuration.perimeterKeyName : null, []],
      centerLatitude: [configuration ? configuration.centerLatitude : null, []],
      centerLongitude: [configuration ? configuration.centerLatitude : null, []],
      range: [configuration ? configuration.range : null, []],
      rangeUnit: [configuration ? configuration.rangeUnit : null, []],
      polygonsDefinition: [configuration ? configuration.polygonsDefinition : null, []],
      minInsideDuration: [configuration ? configuration.minInsideDuration : null,
        [Validators.required, Validators.min(1), Validators.max(2147483647)]],
      minInsideDurationTimeUnit: [configuration ? configuration.minInsideDurationTimeUnit : null, [Validators.required]],
      minOutsideDuration: [configuration ? configuration.minOutsideDuration : null,
        [Validators.required, Validators.min(1), Validators.max(2147483647)]],
      minOutsideDurationTimeUnit: [configuration ? configuration.minOutsideDurationTimeUnit : null, [Validators.required]],
    });
  }

  protected validatorTriggers(): string[] {
    return ['fetchPerimeterInfoFromMessageMetadata', 'perimeterType'];
  }

  protected updateValidators(emitEvent: boolean) {
    const fetchPerimeterInfoFromMessageMetadata: boolean = this.geoActionConfigForm.get('fetchPerimeterInfoFromMessageMetadata').value;
    const perimeterType: PerimeterType = this.geoActionConfigForm.get('perimeterType').value;
    if (fetchPerimeterInfoFromMessageMetadata) {
      this.geoActionConfigForm.get('perimeterKeyName').setValidators([Validators.required]);
    } else {
      this.geoActionConfigForm.get('perimeterKeyName').setValidators([]);
    }
    if (!fetchPerimeterInfoFromMessageMetadata && perimeterType === PerimeterType.CIRCLE) {
      this.geoActionConfigForm.get('centerLatitude').setValidators([Validators.required,
        Validators.min(-90), Validators.max(90)]);
      this.geoActionConfigForm.get('centerLongitude').setValidators([Validators.required,
        Validators.min(-180), Validators.max(180)]);
      this.geoActionConfigForm.get('range').setValidators([Validators.required, Validators.min(0)]);
      this.geoActionConfigForm.get('rangeUnit').setValidators([Validators.required]);

      this.defaultPaddingEnable = false;
    } else {
      this.geoActionConfigForm.get('centerLatitude').setValidators([]);
      this.geoActionConfigForm.get('centerLongitude').setValidators([]);
      this.geoActionConfigForm.get('range').setValidators([]);
      this.geoActionConfigForm.get('rangeUnit').setValidators([]);

      this.defaultPaddingEnable = true;
    }
    if (!fetchPerimeterInfoFromMessageMetadata && perimeterType === PerimeterType.POLYGON) {
      this.geoActionConfigForm.get('polygonsDefinition').setValidators([Validators.required]);
    } else {
      this.geoActionConfigForm.get('polygonsDefinition').setValidators([]);
    }
    this.geoActionConfigForm.get('perimeterKeyName').updateValueAndValidity({emitEvent});
    this.geoActionConfigForm.get('centerLatitude').updateValueAndValidity({emitEvent});
    this.geoActionConfigForm.get('centerLongitude').updateValueAndValidity({emitEvent});
    this.geoActionConfigForm.get('range').updateValueAndValidity({emitEvent});
    this.geoActionConfigForm.get('rangeUnit').updateValueAndValidity({emitEvent});
    this.geoActionConfigForm.get('polygonsDefinition').updateValueAndValidity({emitEvent});
  }
}
