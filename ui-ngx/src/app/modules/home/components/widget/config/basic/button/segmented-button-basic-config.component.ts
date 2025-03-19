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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { BasicWidgetConfigComponent } from '@home/components/widget/config/widget-config.component.models';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { Datasource, TargetDevice, } from '@shared/models/widget.models';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
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
  SegmentedButtonWidgetSettings,
  WidgetButtonToggleState,
  widgetButtonToggleStatesTranslations
} from '@home/components/widget/lib/button/segmented-button-widget.models';

@Component({
  selector: 'tb-segmented-button-basic-config',
  templateUrl: './segmented-button-basic-config.component.html',
  styleUrls: ['../basic-config.scss']
})
export class SegmentedButtonBasicConfigComponent extends BasicWidgetConfigComponent {

  get targetDevice(): TargetDevice {
    const datasources: Datasource[] = this.segmentedButtonWidgetConfigForm.get('datasources').value;
    return getTargetDeviceFromDatasources(datasources);
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

  segmentedButtonWidgetConfigForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected widgetConfigComponent: WidgetConfigComponent,
              private fb: UntypedFormBuilder) {
    super(store, widgetConfigComponent);
  }

  protected configForm(): UntypedFormGroup {
    return this.segmentedButtonWidgetConfigForm;
  }

  protected onConfigSet(configData: WidgetConfigComponentData) {
    const settings: SegmentedButtonWidgetSettings = {...segmentedButtonDefaultSettings, ...(configData.config.settings || {})};

    this.segmentedButtonWidgetConfigForm = this.fb.group({
      datasources: [configData.config.datasources, []],

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

  protected prepareOutputConfig(config: any): WidgetConfigComponentData {
    this.widgetConfig.config.datasources = config.datasources;
    this.widgetConfig.config.settings = this.widgetConfig.config.settings || {};
    this.widgetConfig.config.settings.initialState = config.initialState;
    this.widgetConfig.config.settings.disabledState = config.disabledState;
    this.widgetConfig.config.settings.leftButtonClick = config.leftButtonClick;
    this.widgetConfig.config.settings.rightButtonClick = config.rightButtonClick;
    this.widgetConfig.config.settings.appearance = config.appearance;
    this.widgetConfig.config.borderRadius = this.segmentedButtonLayoutBorderMap.get(config.appearance.layout);
    return this.widgetConfig;
  }

}
