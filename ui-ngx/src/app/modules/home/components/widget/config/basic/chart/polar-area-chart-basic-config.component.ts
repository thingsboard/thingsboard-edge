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

import { Component, TemplateRef, ViewChild } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DataKey } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  polarAreaChartWidgetDefaultSettings,
  PolarAreaChartWidgetSettings
} from '@home/components/widget/lib/chart/polar-area-widget.models';
import {
  LatestChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/latest-chart-basic-config.component';

@Component({
  selector: 'tb-polar-area-chart-basic-config',
  templateUrl: './latest-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class PolarAreaChartBasicConfigComponent extends LatestChartBasicConfigComponent<PolarAreaChartWidgetSettings> {

  @ViewChild('polarAreaChart')
  polarAreaChartConfigTemplate: TemplateRef<any>;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'windPower', label: 'Wind', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B' },
      { name: 'solarPower', label: 'Solar', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A' },
      { name: 'hydroelectricPower', label: 'Hydroelectric', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FFDE30' }];
  }

  protected defaultSettings() {
    return polarAreaChartWidgetDefaultSettings;
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.polarAreaChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetConfigForm: UntypedFormGroup, settings: PolarAreaChartWidgetSettings) {
    latestChartWidgetConfigForm.addControl('barSettings', this.fb.control(settings.barSettings, []));

    latestChartWidgetConfigForm.addControl('axisMin', this.fb.control(settings.axisMin, []));
    latestChartWidgetConfigForm.addControl('axisMax', this.fb.control(settings.axisMax, []));
    latestChartWidgetConfigForm.addControl('axisTickLabelFont', this.fb.control(settings.axisTickLabelFont, []));
    latestChartWidgetConfigForm.addControl('axisTickLabelColor', this.fb.control(settings.axisTickLabelColor, []));
    latestChartWidgetConfigForm.addControl('angleAxisStartAngle', this.fb.control(settings.angleAxisStartAngle, []));
  }

  protected prepareOutputLatestChartConfig(config: any) {
    this.widgetConfig.config.settings.barSettings = config.barSettings;

    this.widgetConfig.config.settings.axisMin = config.axisMin;
    this.widgetConfig.config.settings.axisMax = config.axisMax;
    this.widgetConfig.config.settings.axisTickLabelFont = config.axisTickLabelFont;
    this.widgetConfig.config.settings.axisTickLabelColor = config.axisTickLabelColor;
    this.widgetConfig.config.settings.angleAxisStartAngle = config.angleAxisStartAngle;
  }
}
