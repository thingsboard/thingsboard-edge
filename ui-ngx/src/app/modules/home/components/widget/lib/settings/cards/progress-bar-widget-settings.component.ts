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
  progressBarDefaultSettings,
  ProgressBarLayout,
  progressBarLayoutImages,
  progressBarLayouts,
  progressBarLayoutTranslations
} from '@home/components/widget/lib/cards/progress-bar-widget.models';

@Component({
  selector: 'tb-progress-bar-widget-settings',
  templateUrl: './progress-bar-widget-settings.component.html',
  styleUrls: []
})
export class ProgressBarWidgetSettingsComponent extends WidgetSettingsComponent {

  progressBarLayout = ProgressBarLayout;

  progressBarLayouts = progressBarLayouts;

  progressBarLayoutTranslationMap = progressBarLayoutTranslations;
  progressBarLayoutImageMap = progressBarLayoutImages;

  progressBarWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private $injector: Injector,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.progressBarWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {...progressBarDefaultSettings};
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.progressBarWidgetSettingsForm = this.fb.group({
      layout: [settings.layout, []],
      autoScale: [settings.autoScale, []],

      showValue: [settings.showValue, []],
      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      tickMin: [settings.tickMin, []],
      tickMax: [settings.tickMax, []],

      showTicks: [settings.showTicks, []],
      ticksFont: [settings.ticksFont, []],
      ticksColor: [settings.ticksColor, []],

      barColor: [settings.barColor, []],
      barBackground: [settings.barBackground, []],

      background: [settings.background, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showValue', 'showTicks', 'layout'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showValue: boolean = this.progressBarWidgetSettingsForm.get('showValue').value;
    const showTicks: boolean = this.progressBarWidgetSettingsForm.get('showTicks').value;
    const layout: ProgressBarLayout = this.progressBarWidgetSettingsForm.get('layout').value;

    const ticksEnabled = layout === ProgressBarLayout.default;

    if (showValue) {
      this.progressBarWidgetSettingsForm.get('valueFont').enable();
      this.progressBarWidgetSettingsForm.get('valueColor').enable();
    } else {
      this.progressBarWidgetSettingsForm.get('valueFont').disable();
      this.progressBarWidgetSettingsForm.get('valueColor').disable();
    }

    if (ticksEnabled) {
      this.progressBarWidgetSettingsForm.get('showTicks').enable({emitEvent: false});
      if (showTicks) {
        this.progressBarWidgetSettingsForm.get('ticksFont').enable();
        this.progressBarWidgetSettingsForm.get('ticksColor').enable();
      } else {
        this.progressBarWidgetSettingsForm.get('ticksFont').disable();
        this.progressBarWidgetSettingsForm.get('ticksColor').disable();
      }
    } else {
      this.progressBarWidgetSettingsForm.get('showTicks').disable({emitEvent: false});
      this.progressBarWidgetSettingsForm.get('ticksFont').disable();
      this.progressBarWidgetSettingsForm.get('ticksColor').disable();
    }

    this.progressBarWidgetSettingsForm.get('valueFont').updateValueAndValidity({emitEvent});
    this.progressBarWidgetSettingsForm.get('valueColor').updateValueAndValidity({emitEvent});
    this.progressBarWidgetSettingsForm.get('showTicks').updateValueAndValidity({emitEvent: false});
    this.progressBarWidgetSettingsForm.get('ticksFont').updateValueAndValidity({emitEvent});
    this.progressBarWidgetSettingsForm.get('ticksColor').updateValueAndValidity({emitEvent});
  }

  private _valuePreviewFn(): string {
    const units: string = this.widgetConfig.config.units;
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(78, decimals, units, true);
  }

}
