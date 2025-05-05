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
import { TargetDevice, WidgetSettings, WidgetSettingsComponent, widgetType } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ValueType } from '@shared/models/constants';
import { getTargetDeviceFromDatasources } from '@shared/models/widget-settings.models';
import {
  SegmentedButtonAppearanceType,
  SegmentedButtonColorStylesType,
  segmentedButtonDefaultSettings,
  segmentedButtonLayoutBorder,
  segmentedButtonLayoutImages,
  segmentedButtonLayouts,
  segmentedButtonLayoutTranslations,
  WidgetButtonToggleState,
  widgetButtonToggleStatesTranslations
} from '@home/components/widget/lib/button/segmented-button-widget.models';

@Component({
  selector: 'tb-segmented-button-widget-settings',
  templateUrl: './segmented-button-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class SegmentedButtonWidgetSettingsComponent extends WidgetSettingsComponent {

  get targetDevice(): TargetDevice {
    const datasources = this.widgetConfig?.config?.datasources;
    return getTargetDeviceFromDatasources(datasources);
  }

  get widgetType(): widgetType {
    return this.widgetConfig?.widgetType;
  }
  get borderRadius(): string {
    return this.segmentedButtonLayoutBorderMap.get(this.segmentedButtonWidgetSettingsForm.get('appearance.layout').value);
  }

  segmentedButtonAppearanceType: SegmentedButtonAppearanceType = 'first';
  segmentedButtonColorStylesType: SegmentedButtonColorStylesType = 'selected';

  widgetButtonToggleStates = Object.keys(WidgetButtonToggleState) as WidgetButtonToggleState[];
  widgetButtonToggleStatesTranslationsMap = widgetButtonToggleStatesTranslations;

  segmentedButtonLayouts = segmentedButtonLayouts;
  segmentedButtonLayoutTranslationMap = segmentedButtonLayoutTranslations;
  segmentedButtonLayoutImageMap = segmentedButtonLayoutImages;
  segmentedButtonLayoutBorderMap = segmentedButtonLayoutBorder;

  valueType = ValueType;

  segmentedButtonWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.segmentedButtonWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return segmentedButtonDefaultSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.segmentedButtonWidgetSettingsForm = this.fb.group({
      initialState: [settings.initialState, []],
      leftButtonClick: [settings.leftButtonClick, []],
      rightButtonClick: [settings.rightButtonClick, []],
      disabledState: [settings.disabledState, []],

      appearance: this.fb.group({
        layout: [settings.appearance.layout, []],
        autoScale: [settings.appearance.autoScale, []],
        cardBorder: [settings.appearance.cardBorder, []],
        cardBorderColor: [settings.appearance.cardBorderColor, []],
        leftAppearance: this.fb.group({
          showLabel: [settings.appearance.leftAppearance.showLabel, []],
          label: [settings.appearance.leftAppearance.label, []],
          labelFont: [settings.appearance.leftAppearance.labelFont, []],
          showIcon: [settings.appearance.leftAppearance.showIcon, []],
          icon: [settings.appearance.leftAppearance.icon, []],
          iconSize: [settings.appearance.leftAppearance.iconSize, []],
          iconSizeUnit: [settings.appearance.leftAppearance.iconSizeUnit, []],
        }),
        rightAppearance: this.fb.group({
          showLabel: [settings.appearance.rightAppearance.showLabel, []],
          label: [settings.appearance.rightAppearance.label, []],
          labelFont: [settings.appearance.rightAppearance.labelFont, []],
          showIcon: [settings.appearance.rightAppearance.showIcon, []],
          icon: [settings.appearance.rightAppearance.icon, []],
          iconSize: [settings.appearance.rightAppearance.iconSize, []],
          iconSizeUnit: [settings.appearance.rightAppearance.iconSizeUnit, []],
        }),
        selectedStyle: this.fb.group({
          mainColor: [settings.appearance.selectedStyle.mainColor, []],
          backgroundColor: [settings.appearance.selectedStyle.backgroundColor, []],
          customStyle: this.fb.group({
            enabled: [settings.appearance.selectedStyle.customStyle.enabled, []],
            hovered: [settings.appearance.selectedStyle.customStyle.hovered, []],
            disabled: [settings.appearance.selectedStyle.customStyle.disabled, []],
          })
        }),
        unselectedStyle: this.fb.group({
          mainColor: [settings.appearance.unselectedStyle.mainColor, []],
          backgroundColor: [settings.appearance.unselectedStyle.backgroundColor, []],
          customStyle: this.fb.group({
            enabled: [settings.appearance.unselectedStyle.customStyle.enabled, []],
            hovered: [settings.appearance.unselectedStyle.customStyle.hovered, []],
            disabled: [settings.appearance.unselectedStyle.customStyle.disabled, []],
          })
        }),
      })
    });
  }
}
