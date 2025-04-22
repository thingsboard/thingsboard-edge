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
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { isDefinedAndNotNull } from '@core/utils';
import {
  timeSeriesChartKeyDefaultSettings,
  TimeSeriesChartKeySettings,
  TimeSeriesChartSeriesType,
  timeSeriesChartSeriesTypes,
  timeSeriesChartSeriesTypeTranslations,
  TimeSeriesChartType,
  timeSeriesChartTypeTranslations,
  TimeSeriesChartYAxisId
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { TimeSeriesChartWidgetSettings } from '@home/components/widget/lib/chart/time-series-chart-widget.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-time-series-chart-key-settings',
  templateUrl: './time-series-chart-key-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TimeSeriesChartKeySettingsComponent extends WidgetSettingsComponent {

  TimeSeriesChartType = TimeSeriesChartType;

  timeSeriesChartTypeTranslations = timeSeriesChartTypeTranslations;

  TimeSeriesChartSeriesType = TimeSeriesChartSeriesType;

  timeSeriesChartSeriesTypes = timeSeriesChartSeriesTypes;

  timeSeriesChartSeriesTypeTranslations = timeSeriesChartSeriesTypeTranslations;

  timeSeriesChartKeySettingsForm: UntypedFormGroup;

  chartType = TimeSeriesChartType.default;

  yAxisIds: TimeSeriesChartYAxisId[];

  comparisonEnabled: boolean;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(protected store: Store<AppState>,
              private widgetService: WidgetService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.timeSeriesChartKeySettingsForm;
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
    const params = widgetConfig.typeParameters as any;
    if (isDefinedAndNotNull(params.chartType)) {
      this.chartType = params.chartType;
    }
    const widgetSettings = (widgetConfig.config?.settings || {}) as TimeSeriesChartWidgetSettings;
    this.yAxisIds = widgetSettings.yAxes ? Object.keys(widgetSettings.yAxes) : ['default'];
    this.comparisonEnabled = !!widgetSettings.comparisonEnabled;
  }

  protected defaultSettings(): WidgetSettings {
    return timeSeriesChartKeyDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    const seriesSettings = settings as TimeSeriesChartKeySettings;
    let yAxisId = seriesSettings.yAxisId;
    if (!this.yAxisIds.includes(yAxisId)) {
      yAxisId = 'default';
    }
    this.timeSeriesChartKeySettingsForm = this.fb.group({
      yAxisId: [yAxisId, []],
      showInLegend: [seriesSettings.showInLegend, []],
      dataHiddenByDefault: [seriesSettings.dataHiddenByDefault, []],
      type: [seriesSettings.type, []],
      lineSettings: [seriesSettings.lineSettings, []],
      barSettings: [seriesSettings.barSettings, []],
      tooltipValueFormatter: [seriesSettings.tooltipValueFormatter, []],
      comparisonSettings: this.fb.group({
        showValuesForComparison: [seriesSettings.comparisonSettings?.showValuesForComparison, []],
        comparisonValuesLabel: [seriesSettings.comparisonSettings?.comparisonValuesLabel, []],
        color: [seriesSettings.comparisonSettings?.color, []]
      })
    });
  }

  protected validatorTriggers(): string[] {
    return ['showInLegend', 'type', 'comparisonSettings.showValuesForComparison'];
  }

  protected updateValidators(_emitEvent: boolean) {
    const showInLegend: boolean = this.timeSeriesChartKeySettingsForm.get('showInLegend').value;
    const type: TimeSeriesChartSeriesType = this.timeSeriesChartKeySettingsForm.get('type').value;
    const showValuesForComparison: boolean =
      this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('showValuesForComparison').value;
    if (showInLegend) {
      this.timeSeriesChartKeySettingsForm.get('dataHiddenByDefault').enable();
    } else {
      this.timeSeriesChartKeySettingsForm.get('dataHiddenByDefault').patchValue(false, {emitEvent: false});
      this.timeSeriesChartKeySettingsForm.get('dataHiddenByDefault').disable();
    }
    if (type === TimeSeriesChartSeriesType.line) {
      this.timeSeriesChartKeySettingsForm.get('lineSettings').enable();
      this.timeSeriesChartKeySettingsForm.get('barSettings').disable();
    } else if (type === TimeSeriesChartSeriesType.bar) {
      this.timeSeriesChartKeySettingsForm.get('lineSettings').disable();
      this.timeSeriesChartKeySettingsForm.get('barSettings').enable();
    }
    if (this.comparisonEnabled) {
      this.timeSeriesChartKeySettingsForm.get('comparisonSettings').enable({emitEvent: false});
      if (showValuesForComparison) {
        this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('comparisonValuesLabel').enable();
        this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('color').enable();
      } else {
        this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('comparisonValuesLabel').disable();
        this.timeSeriesChartKeySettingsForm.get('comparisonSettings').get('color').disable();
      }
    } else {
      this.timeSeriesChartKeySettingsForm.get('comparisonSettings').disable({emitEvent: false});
    }
  }
}
