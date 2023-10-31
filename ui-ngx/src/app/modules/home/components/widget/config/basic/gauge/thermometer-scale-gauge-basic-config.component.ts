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
import { UntypedFormBuilder } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  GaugeBasicConfigComponent
} from '@home/components/widget/config/basic/gauge/analog-gauge-basic-config.component';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';

@Component({
  selector: 'tb-thermometer-scale-gauge-basic-config',
  templateUrl: './analog-gauge-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class ThermometerScaleGaugeBasicConfigComponent extends GaugeBasicConfigComponent {

  gaugeType = 'linear';

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              protected fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent, fb);
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    super.onConfigSet(configData);
    this.radialGaugeWidgetConfigForm.addControl('colorBarStroke',
      this.fb.control(configData.config.settings?.colorBarStroke, []));
    this.radialGaugeWidgetConfigForm.addControl('colorBarProgress',
      this.fb.control(configData.config.settings?.colorBarProgress, []));
    this.radialGaugeWidgetConfigForm.addControl('colorBarProgressEnd',
      this.fb.control(configData.config.settings?.colorBarProgressEnd, []));
  }

  protected prepareOutputConfig(config): WidgetConfigComponentData {
    const outputConfig = super.prepareOutputConfig(config);
    this.widgetConfig.config.settings.colorBarStroke = config.colorBarStroke;
    this.widgetConfig.config.settings.colorBarProgress = config.colorBarProgress;
    this.widgetConfig.config.settings.colorBarProgressEnd = config.colorBarProgressEnd;
    return outputConfig;
  }
}
