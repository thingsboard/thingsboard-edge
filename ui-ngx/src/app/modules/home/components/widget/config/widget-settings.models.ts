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

import { isDefinedAndNotNull, isNumber, isNumeric, parseFunction } from '@core/utils';
import { DataKey, Datasource, DatasourceData } from '@shared/models/widget.models';

export type ComponentStyle = {[klass: string]: any};

export const cssUnits = ['px', 'em', '%', 'rem', 'pt', 'pc', 'in', 'cm', 'mm', 'ex', 'ch', 'vw', 'vh', 'vmin', 'vmax'] as const;
type cssUnitTuple = typeof cssUnits;
export type cssUnit = cssUnitTuple[number];

export const fontWeights = ['normal', 'bold', 'bolder', 'lighter', '100', '200', '300', '400', '500', '600', '700', '800', '900'] as const;
type fontWeightTuple = typeof fontWeights;
export type fontWeight = fontWeightTuple[number];

export const fontWeightTranslations = new Map<fontWeight, string>(
  [
    ['normal', 'widgets.widget-font.font-weight-normal'],
    ['bold', 'widgets.widget-font.font-weight-bold'],
    ['bolder', 'widgets.widget-font.font-weight-bolder'],
    ['lighter', 'widgets.widget-font.font-weight-lighter']
  ]
);

export const fontStyles = ['normal', 'italic', 'oblique'] as const;
type fontStyleTuple = typeof fontStyles;
export type fontStyle = fontStyleTuple[number];

export const fontStyleTranslations = new Map<fontStyle, string>(
  [
    ['normal', 'widgets.widget-font.font-style-normal'],
    ['italic', 'widgets.widget-font.font-style-italic'],
    ['oblique', 'widgets.widget-font.font-style-oblique']
  ]
);

export const commonFonts = ['Roboto', 'monospace', 'sans-serif', 'serif'];

export interface Font {
  size: number;
  sizeUnit: cssUnit;
  family: string;
  weight: fontWeight;
  style: fontStyle;
}

export enum ColorType {
  constant = 'constant',
  range = 'range',
  function = 'function'
}

export const colorTypeTranslations = new Map<ColorType, string>(
  [
    [ColorType.constant, 'widgets.color.color-type-constant'],
    [ColorType.range, 'widgets.color.color-type-range'],
    [ColorType.function, 'widgets.color.color-type-function']
  ]
);

export interface ColorRange {
  from?: number;
  to?: number;
  color: string;
}

export interface ColorSettings {
  type: ColorType;
  color: string;
  rangeList?: ColorRange[];
  colorFunction?: string;
}

export const constantColor = (color: string): ColorSettings => ({
  type: ColorType.constant,
  color,
  colorFunction: 'var temperature = value;\n' +
    'if (typeof temperature !== undefined) {\n' +
    '  var percent = (temperature + 60)/120 * 100;\n' +
    '  return tinycolor.mix(\'blue\', \'red\', percent).toHexString();\n' +
    '}\n' +
    'return \'blue\';'
});

type ValueColorFunction = (value: any) => string;

export abstract class ColorProcessor {

  static fromSettings(color: ColorSettings): ColorProcessor {
    switch (color.type) {
      case ColorType.constant:
        return new ConstantColorProcessor(color);
      case ColorType.range:
        return new RangeColorProcessor(color);
      case ColorType.function:
        return new FunctionColorProcessor(color);
    }
  }

  color: string;

  protected constructor(protected settings: ColorSettings) {
    this.color = settings.color;
  }

  abstract update(value: any): void;

}

class ConstantColorProcessor extends ColorProcessor {
  constructor(protected settings: ColorSettings) {
    super(settings);
  }

  update(value: any): void {}
}

class RangeColorProcessor extends ColorProcessor {

  constructor(protected settings: ColorSettings) {
    super(settings);
  }

  update(value: any): void {
    this.color = this.computeFromRange(value);
  }

  private computeFromRange(value: any): string {
    if (this.settings.rangeList?.length && isDefinedAndNotNull(value) && isNumeric(value)) {
      const num = Number(value);
      for (const range of this.settings.rangeList) {
        if ((!isNumber(range.from) || num >= range.from) && (!isNumber(range.to) || num < range.to)) {
          return range.color;
        }
      }
    }
    return this.settings.color;
  }
}

class FunctionColorProcessor extends ColorProcessor {

  private readonly colorFunction: ValueColorFunction;

  constructor(protected settings: ColorSettings) {
    super(settings);
    this.colorFunction = parseFunction(settings.colorFunction, ['value']);
  }

  update(value: any): void {
    if (this.colorFunction) {
      this.color = this.colorFunction(value) || this.settings.color;
    }
  }
}

export enum BackgroundType {
  image = 'image',
  imageUrl = 'imageUrl',
  color = 'color'
}

export interface OverlaySettings {
  enabled: boolean;
  color: string;
  blur: number;
}

export interface BackgroundSettings {
  type: BackgroundType;
  imageBase64?: string;
  imageUrl?: string;
  color?: string;
  overlay: OverlaySettings;
}

export const iconStyle = (size: number, sizeUnit: cssUnit): ComponentStyle => {
  const iconSize = size + sizeUnit;
  return {
    width: iconSize,
    height: iconSize,
    fontSize: iconSize,
    lineHeight: iconSize
  };
};

export const textStyle = (font: Font, lineHeight = '1.5', letterSpacing = '0.25px'): ComponentStyle => ({
  font: font.style + ' normal ' + font.weight + ' ' + (font.size+font.sizeUnit) + '/' + lineHeight + ' ' + font.family +
    (font.family !== 'Roboto' ? ', Roboto' : ''),
  letterSpacing
});

export const backgroundStyle = (background: BackgroundSettings): ComponentStyle => {
  if (background.type === BackgroundType.color) {
    return {
      background: background.color
    };
  } else {
    const imageUrl = background.type === BackgroundType.image ? background.imageBase64 : background.imageUrl;
    return {
      background: `url(${imageUrl}) no-repeat`,
      backgroundSize: 'cover',
      backgroundPosition: '50% 50%'
    };
  }
};

export const overlayStyle = (overlay: OverlaySettings): ComponentStyle => (
  {
    display: overlay.enabled ? 'block' : 'none',
    background: overlay.color,
    backdropFilter: `blur(${overlay.blur}px)`
  }
);

export const getDataKey = (datasources?: Datasource[]): DataKey => {
  if (datasources && datasources.length) {
    const dataKeys = datasources[0].dataKeys;
    if (dataKeys && dataKeys.length) {
      return dataKeys[0];
    }
  }
  return null;
};

export const getLabel = (datasources?: Datasource[]): string => {
  const dataKey = getDataKey(datasources);
  if (dataKey) {
    return dataKey.label;
  }
  return '';
};

export const setLabel = (label: string, datasources?: Datasource[]): void => {
  const dataKey = getDataKey(datasources);
  if (dataKey) {
    dataKey.label = label;
  }
};

export const getSingleTsValue = (data: Array<DatasourceData>): [number, any] => {
  if (data.length) {
    const dsData = data[0];
    if (dsData.data.length) {
      return dsData.data[0];
    }
  }
  return null;
};
