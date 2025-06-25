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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { DataKey } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import {
  radarChartWidgetDefaultSettings,
  RadarChartWidgetSettings
} from '@home/components/widget/lib/chart/radar-chart-widget.models';
import {
  LatestChartBasicConfigComponent
} from '@home/components/widget/config/basic/chart/latest-chart-basic-config.component';

@Component({
  selector: 'tb-radar-chart-basic-config',
  templateUrl: './latest-chart-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class RadarChartBasicConfigComponent extends LatestChartBasicConfigComponent<RadarChartWidgetSettings> {

  @ViewChild('radarChart')
  radarChartConfigTemplate: TemplateRef<any>;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected defaultDataKeys(configData: WidgetConfigComponentData): DataKey[] {
    return [{name: 'windPower', label: 'Wind', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#08872B'},
      {name: 'solarPower', label: 'Solar', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FF4D5A'},
      {name: 'hydroelectricPower', label: 'Hydroelectric', type: DataKeyType.timeseries, units: '', decimals: 0, color: '#FFDE30'}];
  }

  protected defaultSettings() {
    return radarChartWidgetDefaultSettings;
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.radarChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetConfigForm: UntypedFormGroup, settings: RadarChartWidgetSettings) {
    latestChartWidgetConfigForm.addControl('shape', this.fb.control(settings.shape, []));
    latestChartWidgetConfigForm.addControl('color', this.fb.control(settings.color, []));
    latestChartWidgetConfigForm.addControl('showLine', this.fb.control(settings.showLine, []));
    latestChartWidgetConfigForm.addControl('lineType', this.fb.control(settings.lineType, []));
    latestChartWidgetConfigForm.addControl('lineWidth', this.fb.control(settings.lineWidth, [Validators.min(0)]));
    latestChartWidgetConfigForm.addControl('showPoints', this.fb.control(settings.showPoints, []));
    latestChartWidgetConfigForm.addControl('pointShape', this.fb.control(settings.pointShape, []));
    latestChartWidgetConfigForm.addControl('pointSize', this.fb.control(settings.pointSize, [Validators.min(0)]));
    latestChartWidgetConfigForm.addControl('showLabel', this.fb.control(settings.showLabel, []));
    latestChartWidgetConfigForm.addControl('labelPosition', this.fb.control(settings.labelPosition, []));
    latestChartWidgetConfigForm.addControl('labelFont', this.fb.control(settings.labelFont, []));
    latestChartWidgetConfigForm.addControl('labelColor', this.fb.control(settings.labelColor, []));
    latestChartWidgetConfigForm.addControl('fillAreaSettings', this.fb.control(settings.fillAreaSettings, []));

    latestChartWidgetConfigForm.addControl('normalizeAxes', this.fb.control(settings.normalizeAxes, []));
    latestChartWidgetConfigForm.addControl('axisShowLabel', this.fb.control(settings.axisShowLabel, []));
    latestChartWidgetConfigForm.addControl('axisLabelFont', this.fb.control(settings.axisLabelFont, []));
    latestChartWidgetConfigForm.addControl('axisShowTickLabels', this.fb.control(settings.axisShowTickLabels, []));
    latestChartWidgetConfigForm.addControl('axisTickLabelFont', this.fb.control(settings.axisTickLabelFont, []));
    latestChartWidgetConfigForm.addControl('axisTickLabelColor', this.fb.control(settings.axisTickLabelColor, []));
  }

  protected prepareOutputLatestChartConfig(config: any) {
    this.widgetConfig.config.settings.shape = config.shape;
    this.widgetConfig.config.settings.color = config.color;
    this.widgetConfig.config.settings.showLine = config.showLine;
    this.widgetConfig.config.settings.lineType = config.lineType;
    this.widgetConfig.config.settings.lineWidth = config.lineWidth;
    this.widgetConfig.config.settings.showPoints = config.showPoints;
    this.widgetConfig.config.settings.pointShape = config.pointShape;
    this.widgetConfig.config.settings.pointSize = config.pointSize;
    this.widgetConfig.config.settings.showLabel = config.showLabel;
    this.widgetConfig.config.settings.labelPosition = config.labelPosition;
    this.widgetConfig.config.settings.labelFont = config.labelFont;
    this.widgetConfig.config.settings.labelColor = config.labelColor;
    this.widgetConfig.config.settings.fillAreaSettings = config.fillAreaSettings;

    this.widgetConfig.config.settings.normalizeAxes = config.normalizeAxes;
    this.widgetConfig.config.settings.axisShowLabel = config.axisShowLabel;
    this.widgetConfig.config.settings.axisLabelFont = config.axisLabelFont;
    this.widgetConfig.config.settings.axisShowTickLabels = config.axisShowTickLabels;
    this.widgetConfig.config.settings.axisTickLabelFont = config.axisTickLabelFont;
    this.widgetConfig.config.settings.axisTickLabelColor = config.axisTickLabelColor;
  }

  protected latestChartValidatorTriggers(): string[] {
    return ['showLine', 'showPoints', 'showLabel', 'axisShowLabel',
      'axisShowTickLabels'];
  }

  protected updateLatestChartValidators(latestChartWidgetConfigForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
    const showLine: boolean = latestChartWidgetConfigForm.get('showLine').value;
    const showPoints: boolean = latestChartWidgetConfigForm.get('showPoints').value;
    const showLabel: boolean = latestChartWidgetConfigForm.get('showLabel').value;
    const axisShowLabel: boolean = latestChartWidgetConfigForm.get('axisShowLabel').value;
    const axisShowTickLabels: boolean = latestChartWidgetConfigForm.get('axisShowTickLabels').value;

    if (showLine) {
      latestChartWidgetConfigForm.get('lineType').enable();
      latestChartWidgetConfigForm.get('lineWidth').enable();
    } else {
      latestChartWidgetConfigForm.get('lineType').disable();
      latestChartWidgetConfigForm.get('lineWidth').disable();
    }

    if (showPoints) {
      latestChartWidgetConfigForm.get('pointShape').enable();
      latestChartWidgetConfigForm.get('pointSize').enable();
    } else {
      latestChartWidgetConfigForm.get('pointShape').disable();
      latestChartWidgetConfigForm.get('pointSize').disable();
    }

    if (showLabel) {
      latestChartWidgetConfigForm.get('labelPosition').enable();
      latestChartWidgetConfigForm.get('labelFont').enable();
      latestChartWidgetConfigForm.get('labelColor').enable();
    } else {
      latestChartWidgetConfigForm.get('labelPosition').disable();
      latestChartWidgetConfigForm.get('labelFont').disable();
      latestChartWidgetConfigForm.get('labelColor').disable();
    }

    if (axisShowLabel) {
      latestChartWidgetConfigForm.get('axisLabelFont').enable();
    } else {
      latestChartWidgetConfigForm.get('axisLabelFont').disable();
    }

    if (axisShowTickLabels) {
      latestChartWidgetConfigForm.get('axisTickLabelFont').enable();
      latestChartWidgetConfigForm.get('axisTickLabelColor').enable();
    } else {
      latestChartWidgetConfigForm.get('axisTickLabelFont').disable();
      latestChartWidgetConfigForm.get('axisTickLabelColor').disable();
    }
  }
}
