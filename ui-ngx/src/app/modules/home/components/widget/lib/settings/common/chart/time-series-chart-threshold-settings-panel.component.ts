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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import {
  TimeSeriesChartShape,
  timeSeriesChartShapes,
  timeSeriesChartShapeTranslations,
  TimeSeriesChartThreshold,
  timeSeriesLineTypes,
  timeSeriesLineTypeTranslations,
  timeSeriesThresholdLabelPositions,
  timeSeriesThresholdLabelPositionTranslations
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { merge } from 'rxjs';
import { WidgetConfig } from '@shared/models/widget.models';
import { formatValue, isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-time-series-chart-threshold-settings-panel',
  templateUrl: './time-series-chart-threshold-settings-panel.component.html',
  providers: [],
  styleUrls: ['./time-series-chart-threshold-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimeSeriesChartThresholdSettingsPanelComponent implements OnInit {

  timeSeriesLineTypes = timeSeriesLineTypes;

  timeSeriesLineTypeTranslations = timeSeriesLineTypeTranslations;

  timeSeriesChartShapes = timeSeriesChartShapes;

  timeSeriesChartShapeTranslations = timeSeriesChartShapeTranslations;

  timeSeriesThresholdLabelPositions = timeSeriesThresholdLabelPositions;

  timeSeriesThresholdLabelPositionTranslations = timeSeriesThresholdLabelPositionTranslations;

  labelPreviewFn = this._labelPreviewFn.bind(this);

  @Input()
  thresholdSettings: Partial<TimeSeriesChartThreshold>;

  @Input()
  widgetConfig: WidgetConfig;

  @Input()
  popover: TbPopoverComponent<TimeSeriesChartThresholdSettingsPanelComponent>;

  @Output()
  thresholdSettingsApplied = new EventEmitter<Partial<TimeSeriesChartThreshold>>();

  thresholdSettingsFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.thresholdSettingsFormGroup = this.fb.group(
      {
        units: [this.thresholdSettings.units, []],
        decimals: [this.thresholdSettings.decimals, [Validators.min(0)]],
        lineColor: [this.thresholdSettings.lineColor, []],
        lineType: [this.thresholdSettings.lineType, []],
        lineWidth: [this.thresholdSettings.lineWidth, [Validators.min(0)]],
        startSymbol: [this.thresholdSettings.startSymbol, []],
        startSymbolSize: [this.thresholdSettings.startSymbolSize, [Validators.min(0)]],
        endSymbol: [this.thresholdSettings.endSymbol, []],
        endSymbolSize: [this.thresholdSettings.endSymbolSize, [Validators.min(0)]],
        showLabel: [this.thresholdSettings.showLabel, []],
        labelPosition: [this.thresholdSettings.labelPosition, []],
        labelFont: [this.thresholdSettings.labelFont, []],
        labelColor: [this.thresholdSettings.labelColor, []]
      }
    );
    merge(this.thresholdSettingsFormGroup.get('showLabel').valueChanges,
          this.thresholdSettingsFormGroup.get('startSymbol').valueChanges,
          this.thresholdSettingsFormGroup.get('endSymbol').valueChanges).subscribe(() => {
      this.updateValidators();
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyThresholdSettings() {
    const thresholdSettings = this.thresholdSettingsFormGroup.getRawValue();
    this.thresholdSettingsApplied.emit(thresholdSettings);
  }

  private updateValidators() {
    const showLabel: boolean = this.thresholdSettingsFormGroup.get('showLabel').value;
    const startSymbol: TimeSeriesChartShape = this.thresholdSettingsFormGroup.get('startSymbol').value;
    const endSymbol: TimeSeriesChartShape = this.thresholdSettingsFormGroup.get('endSymbol').value;
    if (showLabel) {
      this.thresholdSettingsFormGroup.get('labelPosition').enable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelFont').enable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelColor').enable({emitEvent: false});
    } else {
      this.thresholdSettingsFormGroup.get('labelPosition').disable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelFont').disable({emitEvent: false});
      this.thresholdSettingsFormGroup.get('labelColor').disable({emitEvent: false});
    }
    if (startSymbol === TimeSeriesChartShape.none) {
      this.thresholdSettingsFormGroup.get('startSymbolSize').disable({emitEvent: false});
    } else {
      this.thresholdSettingsFormGroup.get('startSymbolSize').enable({emitEvent: false});
    }
    if (endSymbol === TimeSeriesChartShape.none) {
      this.thresholdSettingsFormGroup.get('endSymbolSize').disable({emitEvent: false});
    } else {
      this.thresholdSettingsFormGroup.get('endSymbolSize').enable({emitEvent: false});
    }
  }

  private _labelPreviewFn(): string {
    let units: string = this.thresholdSettingsFormGroup.get('units').value;
    units = units && units.length ? units : this.widgetConfig.units;
    let decimals: number = this.thresholdSettingsFormGroup.get('decimals').value;
    decimals = isDefinedAndNotNull(decimals) ? decimals :
      (isDefinedAndNotNull(this.widgetConfig.decimals) ? this.widgetConfig.decimals : 2);
    return formatValue(22, decimals, units, false);
  }
}
