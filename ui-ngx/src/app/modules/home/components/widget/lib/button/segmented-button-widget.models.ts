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

import { DataToValueType, GetValueAction, GetValueSettings } from '@shared/models/action-widget-settings.models';
import { cssUnit, Font } from '@shared/models/widget-settings.models';
import { defaultWidgetAction, WidgetAction } from '@shared/models/widget.models';

const defaultMainColor = '#00695C';
const defaultBackgroundColor = '#E8E8E8';

export const defaultMainColorDisabled = 'rgba(0, 0, 0, 0.38)';
export const defaultBackgroundColorDisabled = 'rgba(0, 0, 0, 0.03)';
export const defaultBorderColorDisabled = defaultBackgroundColorDisabled;

const defaultBorderColor = defaultBackgroundColor;

export enum SegmentedButtonLayout {
  squared = 'squared',
  rounded = 'rounded'
}

export type SegmentedButtonAppearanceType = 'first' | 'second';
export type SegmentedButtonColorStylesType = 'selected' | 'unselected';

export const segmentedButtonLayouts = Object.keys(SegmentedButtonLayout) as SegmentedButtonLayout[];

export const segmentedButtonLayoutTranslations = new Map<SegmentedButtonLayout, string>(
  [
    [SegmentedButtonLayout.squared, 'widgets.segmented-button.layout-squared'],
    [SegmentedButtonLayout.rounded, 'widgets.segmented-button.layout-rounded']
  ]
);

export const segmentedButtonLayoutImages = new Map<SegmentedButtonLayout, string>(
  [
    [SegmentedButtonLayout.squared, 'assets/widget/segmented-button/squared-layout.svg'],
    [SegmentedButtonLayout.rounded, 'assets/widget/segmented-button/rounded-layout.svg']
  ]
);

export const segmentedButtonLayoutBorder = new Map<SegmentedButtonLayout, string>(
  [
    [SegmentedButtonLayout.squared, '4px'],
    [SegmentedButtonLayout.rounded, '40px']
  ]
);

export interface SegmentedButtonAppearance {
  showLabel: boolean;
  label: string;
  labelFont: Font;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
}

export interface SegmentedButtonStyles {
  mainColor: string;
  backgroundColor: string;
  customStyle: WidgetButtonToggleCustomStyles;
}

export interface ButtonToggleAppearance {
  layout: SegmentedButtonLayout;
  autoScale: boolean;
  cardBorder: number;
  cardBorderColor: string;
  leftAppearance: SegmentedButtonAppearance;
  rightAppearance: SegmentedButtonAppearance;
  selectedStyle: SegmentedButtonStyles;
  unselectedStyle: SegmentedButtonStyles;
}

export interface SegmentedButtonWidgetSettings {
  initialState: GetValueSettings<boolean>;
  disabledState: GetValueSettings<boolean>;
  leftButtonClick: WidgetAction;
  rightButtonClick: WidgetAction;

  appearance: ButtonToggleAppearance;
}

export const segmentedButtonDefaultAppearance: ButtonToggleAppearance = {
  layout: SegmentedButtonLayout.squared,
  autoScale: true,
  cardBorder: 1,
  cardBorderColor: '#305680',
  leftAppearance: {
    showLabel: true,
    label: 'Traditional',
    labelFont: {
      family: 'Roboto',
      weight: '500',
      style: 'normal',
      size: 14,
      sizeUnit: 'px',
      lineHeight: '18px'
    },
    showIcon: true,
    icon: 'home',
    iconSize: 24,
    iconSizeUnit: 'px',
  },
  rightAppearance: {
    showLabel: true,
    label: 'Hi-Perf',
    labelFont: {
      family: 'Roboto',
      weight: '500',
      style: 'normal',
      size: 14,
      sizeUnit: 'px',
      lineHeight: '18px'
    },
    showIcon: true,
    icon: 'home',
    iconSize: 24,
    iconSizeUnit: 'px',
  },
  selectedStyle: {
    mainColor: '#FFFFFF',
    backgroundColor: '#00695C',
    customStyle: {
      enabled: null,
      hovered: null,
      disabled: null
    }
  },
  unselectedStyle: {
    mainColor: '#000000C2',
    backgroundColor: '#E8E8E8',
    customStyle: {
      enabled: null,
      hovered: null,
      disabled: null
    }
  }
}

export const segmentedButtonDefaultSettings: SegmentedButtonWidgetSettings = {
  initialState: {
    action: GetValueAction.DO_NOTHING,
    defaultValue: true,
    getAttribute: {
      key: 'state',
      scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return boolean value */\nreturn data;'
    }
  },
  disabledState: {
    action: GetValueAction.DO_NOTHING,
    defaultValue: false,
    getAttribute: {
      key: 'state',
      scope: null
    },
    getTimeSeries: {
      key: 'state'
    },
    getAlarmStatus: {
      severityList: null,
      typeList: null
    },
    dataToValue: {
      type: DataToValueType.NONE,
      compareToValue: true,
      dataToValueFunction: '/* Should return boolean value */\nreturn data;'
    }
  },
  leftButtonClick:  defaultWidgetAction(),
  rightButtonClick:  defaultWidgetAction(),
  appearance: segmentedButtonDefaultAppearance
};


const mainCheckedColorVarPrefix = '--tb-widget-button-toggle-main-checked-color-';
const backgroundCheckedColorVarPrefix = '--tb-widget-button-toggle-background-checked-color-';
const borderCheckedColorVarPrefix = '--tb-widget-button-toggle-border-checked-color-';

const mainUncheckedColorVarPrefix = '--tb-widget-button-toggle-main-unchecked-color-';
const backgroundUncheckedColorVarPrefix = '--tb-widget-button-toggle-background-unchecked-color-';
const borderUncheckedColorVarPrefix = '--tb-widget-button-toggle-border-unchecked-color-';

export enum WidgetButtonToggleState {
  enabled = 'enabled',
  hovered = 'hovered',
  disabled = 'disabled'
}

export const widgetButtonToggleStates = Object.keys(WidgetButtonToggleState) as WidgetButtonToggleState[];

export const widgetButtonToggleStatesTranslations = new Map<WidgetButtonToggleState, string>(
  [
    [WidgetButtonToggleState.enabled, 'widgets.button-state.enabled'],
    [WidgetButtonToggleState.hovered, 'widgets.button-state.hovered'],
    [WidgetButtonToggleState.disabled, 'widgets.button-state.disabled']
  ]
);

export interface WidgetButtonToggleCustomStyle {
  overrideMainColor?: boolean;
  mainColor?: string;
  overrideBackgroundColor?: boolean;
  backgroundColor?: string;
  overrideBorderColor?: boolean;
  borderColor?: string;
}

export type WidgetButtonToggleCustomStyles = Record<WidgetButtonToggleState, WidgetButtonToggleCustomStyle>;

export abstract class ButtonToggleStateCssGenerator {

  constructor() {}

  public generateStateCss(selectedAppearance: SegmentedButtonStyles, unselectedAppearance: SegmentedButtonStyles): string {
    const selectedColor = this.getColors(selectedAppearance);
    const unselectedColor = this.getColors(unselectedAppearance);

    return `${mainCheckedColorVarPrefix}${this.state}: ${selectedColor.mainColor};\n`+
      `${backgroundCheckedColorVarPrefix}${this.state}: ${selectedColor.backgroundColor};\n`+
      `${borderCheckedColorVarPrefix}${this.state}: ${selectedColor.borderColor};\n`+
        `${mainUncheckedColorVarPrefix}${this.state}: ${unselectedColor.mainColor};\n`+
        `${backgroundUncheckedColorVarPrefix}${this.state}: ${unselectedColor.backgroundColor};\n`+
        `${borderUncheckedColorVarPrefix}${this.state}: ${unselectedColor.borderColor};`;
  }

  private getColors(appearance: SegmentedButtonStyles) {
    let mainColor = this.getMainColor(appearance);
    let backgroundColor = this.getBackgroundColor(appearance);
    let borderColor = this.getBorderColor();
    const stateCustomStyle = appearance.customStyle[this.state];
    if (stateCustomStyle?.overrideMainColor && stateCustomStyle?.mainColor) {
      mainColor = stateCustomStyle.mainColor;
    }
    if (stateCustomStyle?.overrideBackgroundColor && stateCustomStyle?.backgroundColor) {
      backgroundColor = stateCustomStyle.backgroundColor;
    }
    if (stateCustomStyle?.overrideBorderColor && stateCustomStyle?.borderColor) {
      borderColor = stateCustomStyle.borderColor;
    }
    return {
      mainColor,
      backgroundColor,
      borderColor
    }
  }

  protected abstract get state(): WidgetButtonToggleState;

  protected getMainColor(appearance: SegmentedButtonStyles): string {
    return appearance.mainColor || defaultMainColor;
  }

  protected getBackgroundColor(appearance: SegmentedButtonStyles): string {
    return appearance.backgroundColor || defaultBackgroundColor;
  }

  protected getBorderColor(): string {
    return defaultBorderColor;
  }
}

class EnabledButtonStateCssGenerator extends ButtonToggleStateCssGenerator {

  protected get state(): WidgetButtonToggleState {
    return WidgetButtonToggleState.enabled;
  }
}

class HoveredButtonStateCssGenerator extends ButtonToggleStateCssGenerator {

  protected get state(): WidgetButtonToggleState {
    return WidgetButtonToggleState.hovered;
  }
}

class DisabledButtonStateCssGenerator extends ButtonToggleStateCssGenerator {

  protected get state(): WidgetButtonToggleState {
    return WidgetButtonToggleState.disabled;
  }

  protected getMainColor(): string {
    return defaultMainColorDisabled;
  }

  protected getBackgroundColor(): string {
    return defaultBackgroundColorDisabled;
  }

  protected getBorderColor(): string {
    return defaultBorderColorDisabled;
  }
}

const buttonToggleStateCssGeneratorsMap = new Map<WidgetButtonToggleState, ButtonToggleStateCssGenerator>(
  [
    [WidgetButtonToggleState.enabled, new EnabledButtonStateCssGenerator()],
    [WidgetButtonToggleState.hovered, new HoveredButtonStateCssGenerator()],
    [WidgetButtonToggleState.disabled, new DisabledButtonStateCssGenerator()]
  ]
);

const widgetButtonCssSelector = '.mat-button-toggle-group.mat-button-toggle-group-appearance-standard.tb-toggle-header';

export const generateWidgetButtonToggleAppearanceCss = (selectedAppearance: SegmentedButtonStyles, unselectedAppearance: SegmentedButtonStyles): string => {
  let statesCss = '';
  for (const state of widgetButtonToggleStates) {
    const generator = buttonToggleStateCssGeneratorsMap.get(state);
    statesCss += `\n${generator.generateStateCss(selectedAppearance, unselectedAppearance)}`;
  }
  return `${widgetButtonCssSelector} {\n`+
    `${statesCss}\n`+
    `}`;
};

export const generateWidgetButtonToggleBorderLayout = (layout: SegmentedButtonLayout): string => {
  return `${widgetButtonCssSelector} {\n`+
    `--tb-widget-button-toggle-border-radius: ${segmentedButtonLayoutBorder.get(layout)}\n`+
    `}`;
};
