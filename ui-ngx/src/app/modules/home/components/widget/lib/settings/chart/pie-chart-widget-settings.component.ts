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
import { WidgetSettings } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  pieChartWidgetDefaultSettings,
  PieChartWidgetSettings
} from '@home/components/widget/lib/chart/pie-chart-widget.models';
import {
  LatestChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/latest-chart-widget-settings.component';

@Component({
  selector: 'tb-pie-chart-widget-settings',
  templateUrl: './latest-chart-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class PieChartWidgetSettingsComponent extends LatestChartWidgetSettingsComponent<PieChartWidgetSettings> {

  @ViewChild('pieChart')
  pieChartConfigTemplate: TemplateRef<any>;

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store, fb);
  }

  protected defaultLatestChartSettings() {
    return pieChartWidgetDefaultSettings;
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.pieChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetSettingsForm: UntypedFormGroup, settings: WidgetSettings) {
    latestChartWidgetSettingsForm.addControl('showLabel', this.fb.control(settings.showLabel, []));
    latestChartWidgetSettingsForm.addControl('labelPosition', this.fb.control(settings.labelPosition, []));
    latestChartWidgetSettingsForm.addControl('labelFont', this.fb.control(settings.labelFont, []));
    latestChartWidgetSettingsForm.addControl('labelColor', this.fb.control(settings.labelColor, []));
    latestChartWidgetSettingsForm.addControl('borderWidth', this.fb.control(settings.borderWidth, [Validators.min(0)]));
    latestChartWidgetSettingsForm.addControl('borderColor', this.fb.control(settings.borderColor, []));
    latestChartWidgetSettingsForm.addControl('radius', this.fb.control(settings.radius, []));
    latestChartWidgetSettingsForm.addControl('clockwise', this.fb.control(settings.clockwise, []));
  }

  protected latestChartValidatorTriggers(): string[] {
    return ['showLabel'];
  }

  protected updateLatestChartValidators(latestChartWidgetSettingsForm: UntypedFormGroup, emitEvent: boolean, trigger?: string) {
    const showLabel: boolean = latestChartWidgetSettingsForm.get('showLabel').value;
    if (showLabel) {
      latestChartWidgetSettingsForm.get('labelPosition').enable();
      latestChartWidgetSettingsForm.get('labelFont').enable();
      latestChartWidgetSettingsForm.get('labelColor').enable();
    } else {
      latestChartWidgetSettingsForm.get('labelPosition').disable();
      latestChartWidgetSettingsForm.get('labelFont').disable();
      latestChartWidgetSettingsForm.get('labelColor').disable();
    }
  }
}
