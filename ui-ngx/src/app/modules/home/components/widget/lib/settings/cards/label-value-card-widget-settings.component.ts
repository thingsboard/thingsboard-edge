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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { labelValueCardWidgetDefaultSettings } from '@home/components/widget/lib/cards/label-value-card-widget.models';
import { formatValue } from '@core/utils';
import { getSourceTbUnitSymbol } from '@shared/models/unit.models';

@Component({
  selector: 'tb-label-value-card-widget-settings',
  templateUrl: './label-value-card-widget-settings.component.html',
  styleUrls: []
})
export class LabelValueCardWidgetSettingsComponent extends WidgetSettingsComponent {

  labelValueCardWidgetSettingsForm: UntypedFormGroup;

  valuePreviewFn = this._valuePreviewFn.bind(this);

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.labelValueCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return labelValueCardWidgetDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.labelValueCardWidgetSettingsForm = this.fb.group({
      autoScale: [settings.autoScale, []],

      showLabel: [settings.showLabel, []],
      label: [settings.label, []],
      labelFont: [settings.labelFont, []],
      labelColor: [settings.labelColor, []],

      showIcon: [settings.showIcon, []],
      iconSize: [settings.iconSize, [Validators.min(0)]],
      iconSizeUnit: [settings.iconSizeUnit, []],
      icon: [settings.icon, []],
      iconColor: [settings.iconColor, []],

      valueFont: [settings.valueFont, []],
      valueColor: [settings.valueColor, []],

      background: [settings.background, []],
      padding: [settings.padding, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['showLabel', 'showIcon'];
  }

  protected updateValidators(emitEvent: boolean) {
    const showLabel: boolean = this.labelValueCardWidgetSettingsForm.get('showLabel').value;
    const showIcon: boolean = this.labelValueCardWidgetSettingsForm.get('showIcon').value;

    if (showLabel) {
      this.labelValueCardWidgetSettingsForm.get('label').enable();
      this.labelValueCardWidgetSettingsForm.get('labelFont').enable();
      this.labelValueCardWidgetSettingsForm.get('labelColor').enable();
    } else {
      this.labelValueCardWidgetSettingsForm.get('label').disable();
      this.labelValueCardWidgetSettingsForm.get('labelFont').disable();
      this.labelValueCardWidgetSettingsForm.get('labelColor').disable();
    }

    if (showIcon) {
      this.labelValueCardWidgetSettingsForm.get('iconSize').enable();
      this.labelValueCardWidgetSettingsForm.get('iconSizeUnit').enable();
      this.labelValueCardWidgetSettingsForm.get('icon').enable();
      this.labelValueCardWidgetSettingsForm.get('iconColor').enable();
    } else {
      this.labelValueCardWidgetSettingsForm.get('iconSize').disable();
      this.labelValueCardWidgetSettingsForm.get('iconSizeUnit').disable();
      this.labelValueCardWidgetSettingsForm.get('icon').disable();
      this.labelValueCardWidgetSettingsForm.get('iconColor').disable();
    }
  }

  private _valuePreviewFn(): string {
    const units = getSourceTbUnitSymbol(this.widgetConfig.config.units);
    const decimals: number = this.widgetConfig.config.decimals;
    return formatValue(22, decimals, units, true);
  }
}
