///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import { WidgetSettings } from '@shared/models/widget.models';
import { FormBuilder, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  AnalogueGaugeWidgetSettingsComponent
} from '@home/components/widget/lib/settings/gauge/analogue-gauge-widget-settings.component';

@Component({
  selector: 'tb-analogue-linear-gauge-widget-settings',
  templateUrl: './analogue-gauge-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class AnalogueLinearGaugeWidgetSettingsComponent extends AnalogueGaugeWidgetSettingsComponent {

  gaugeType = 'linear';

  constructor(protected store: Store<AppState>,
              protected fb: FormBuilder) {
    super(store, fb);
  }

  protected defaultSettings(): WidgetSettings {
    const settings = super.defaultSettings();
    settings.barStrokeWidth = 2.5;
    settings.colorBarStroke = null;
    settings.colorBar = '#fff';
    settings.colorBarEnd = '#ddd';
    settings.colorBarProgress = null;
    settings.colorBarProgressEnd = null;
    return settings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    super.onSettingsSet(settings);
    this.analogueGaugeWidgetSettingsForm.addControl('barStrokeWidth',
      this.fb.control(settings.barStrokeWidth, [Validators.min(0)]));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBarStroke',
      this.fb.control(settings.colorBarStroke, []));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBar',
      this.fb.control(settings.colorBar, []));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBarEnd',
      this.fb.control(settings.colorBarEnd, []));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBarProgress',
      this.fb.control(settings.colorBarProgress, []));
    this.analogueGaugeWidgetSettingsForm.addControl('colorBarProgressEnd',
      this.fb.control(settings.colorBarProgressEnd, []));
  }
}
