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
  doughnutDefaultSettings,
  DoughnutLayout,
  DoughnutWidgetSettings
} from '@home/components/widget/lib/chart/doughnut-widget.models';
import {
  LatestChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/latest-chart-basic-config.component';

@Component({
  selector: 'tb-doughnut-basic-config',
  templateUrl: './latest-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class DoughnutBasicConfigComponent extends LatestChartBasicConfigComponent<DoughnutWidgetSettings> {

  @ViewChild('doughnutChart')
  doughnutChartConfigTemplate: TemplateRef<any>;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{ name: 'windPower', label: 'Wind power', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B' },
            { name: 'solarPower', label: 'Solar power', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A' }];
  }

  protected defaultSettings() {
    return doughnutDefaultSettings(this.doughnutHorizontal);
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.doughnutChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetConfigForm: UntypedFormGroup, settings: DoughnutWidgetSettings) {
    latestChartWidgetConfigForm.addControl('layout', this.fb.control(settings.layout, []));
    latestChartWidgetConfigForm.addControl('autoScale', this.fb.control(settings.autoScale, []));
    latestChartWidgetConfigForm.addControl('clockwise', this.fb.control(settings.clockwise, []));
    latestChartWidgetConfigForm.addControl('totalValueFont', this.fb.control(settings.totalValueFont, []));
    latestChartWidgetConfigForm.addControl('totalValueColor', this.fb.control(settings.totalValueColor, []));
  }

  protected prepareOutputLatestChartConfig(config: any) {
    this.widgetConfig.config.settings.layout = config.layout;
    this.widgetConfig.config.settings.autoScale = config.autoScale;
    this.widgetConfig.config.settings.clockwise = config.clockwise;
    this.widgetConfig.config.settings.totalValueFont = config.totalValueFont;
    this.widgetConfig.config.settings.totalValueColor = config.totalValueColor;
  }

  protected latestChartValidatorTriggers(): string[] {
    return ['layout'];
  }

  protected updateLatestChartValidators(latestChartWidgetConfigForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
    const layout: DoughnutLayout = latestChartWidgetConfigForm.get('layout').value;
    const totalEnabled = layout === DoughnutLayout.with_total;
    if (totalEnabled) {
      latestChartWidgetConfigForm.get('totalValueFont').enable();
      latestChartWidgetConfigForm.get('totalValueColor').enable();
    } else {
      latestChartWidgetConfigForm.get('totalValueFont').disable();
      latestChartWidgetConfigForm.get('totalValueColor').disable();
    }
  }
}
