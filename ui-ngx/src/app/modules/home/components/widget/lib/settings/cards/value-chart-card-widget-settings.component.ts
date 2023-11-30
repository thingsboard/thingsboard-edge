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

import { Component, Injector } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { formatValue } from '@core/utils';
import {
  valueCartCardLayouts,
  valueChartCardDefaultSettings,
  valueChartCardLayoutImages,
  valueChartCardLayoutTranslations
} from '@home/components/widget/lib/cards/value-chart-card-widget.models';

@Component({
  selector: 'tb-value-chart-card-widget-settings',
  templateUrl: './value-chart-card-widget-settings.component.html',
  styleUrls: []
})
export class ValueChartCardWidgetSettingsComponent extends WidgetSettingsComponent {

  valueChartCardLayouts = valueCartCardLayouts;

  valueChartCardLayoutTranslationMap = valueChartCardLayoutTranslations;
  valueChartCardLayoutImageMap = valueChartCardLayoutImages;

  valueChartCardWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.valueChartCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {...valueChartCardDefaultSettings};
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.valueChartCardWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showValue: [settings.showValue, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showValue'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showValue: boolean = this.valueChartCardWidgetSettingsForm.get('showValue').value;

    if (showValue) {
      this.valueChartCardWidgetSettingsForm.get('valueFont').enable();
      this.valueChartCardWidgetSettingsForm.get('valueColor').enable();
    } else {
      this.valueChartCardWidgetSettingsForm.get('valueFont').disable();
      this.valueChartCardWidgetSettingsForm.get('valueColor').disable();
    }

    this.valueChartCardWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.valueChartCardWidgetSettingsForm.get('valueColor').updateValueAndValidity({emitEvent});
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }

}
