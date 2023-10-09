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
import { formatValue, isDefinedAndNotNull } from '@core/utils';
import {
  centerValueLabel,
  windSpeedDirectionDefaultSettings,
  WindSpeedDirectionLayout,
  windSpeedDirectionLayoutImages,
  windSpeedDirectionLayouts,
  windSpeedDirectionLayoutTranslations
} from '@home/components/widget/lib/weather/wind-speed-direction-widget.models';
import { getDataKeyByLabel } from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-wind-speed-direction-widget-settings',
  templateUrl: './wind-speed-direction-widget-settings.component.html',
  styleUrls: []
})
export class WindSpeedDirectionWidgetSettingsComponent extends WidgetSettingsComponent {

  get hasCenterValue(): boolean {
    return !!getDataKeyByLabel(this.widgetConfig.config.datasources, centerValueLabel);
  }

  get majorTicksFontEnabled(): boolean {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetSettingsForm.get('layout').value;
    return [ WindSpeedDirectionLayout.default, WindSpeedDirectionLayout.advanced ].includes(layout);
  }

  get minorTicksFontEnabled(): boolean {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetSettingsForm.get('layout').value;
    return layout === WindSpeedDirectionLayout.advanced;
  }

  windSpeedDirectionLayouts = windSpeedDirectionLayouts;

  windSpeedDirectionLayoutTranslationMap = windSpeedDirectionLayoutTranslations;
  windSpeedDirectionLayoutImageMap = windSpeedDirectionLayoutImages;

  windSpeedDirectionWidgetSettingsForm: UntypedFormGroup;

  centerValuePreviewFn = this._centerValuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.windSpeedDirectionWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {...windSpeedDirectionDefaultSettings};
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.windSpeedDirectionWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],

      centerValueFont: [settings.centerValueFont, []],
      centerValueColor: [settings.centerValueColor, []],

      ticksColor: [settings.ticksColor, []],
      directionalNamesElseDegrees: [settings.directionalNamesElseDegrees, []],

      majorTicksFont: [settings.majorTicksFont, []],
      majorTicksColor: [settings.majorTicksColor, []],

      minorTicksFont: [settings.minorTicksFont, []],
      minorTicksColor: [settings.minorTicksColor, []],

      arrowColor: [settings.arrowColor, []],

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['layout'];
  }

  protected updateValidators(emitEvent: boolean) {
    const layout: WindSpeedDirectionLayout = this.windSpeedDirectionWidgetSettingsForm.get('layout').value;

    const majorTicksFontEnabled = [ WindSpeedDirectionLayout.default, WindSpeedDirectionLayout.advanced ].includes(layout);
    const minorTicksFontEnabled = layout === WindSpeedDirectionLayout.advanced;

    if (majorTicksFontEnabled) {
      this.windSpeedDirectionWidgetSettingsForm.get('majorTicksFont').enable();
    } else {
      this.windSpeedDirectionWidgetSettingsForm.get('majorTicksFont').disable();
    }

    if (minorTicksFontEnabled) {
      this.windSpeedDirectionWidgetSettingsForm.get('minorTicksFont').enable();
    } else {
      this.windSpeedDirectionWidgetSettingsForm.get('minorTicksFont').disable();
    }
  }

  private _centerValuePreviewFn(): string {
    const centerValueDataKey = getDataKeyByLabel(this.widgetConfig.config.datasources, centerValueLabel);
    if (centerValueDataKey) {
      let units: string = this.widgetConfig.config.units;
      let decimals: number = this.widgetConfig.config.decimals;
      if (isDefinedAndNotNull(centerValueDataKey?.decimals)) {
        decimals = centerValueDataKey.decimals;
      }
      if (centerValueDataKey?.units) {
        units = centerValueDataKey.units;
      }
      return formatValue(25, decimals, units, true);
    } else {
      return '225°';
    }
  }

}
