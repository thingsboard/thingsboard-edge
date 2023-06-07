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

import tinycolor from 'tinycolor2';
import { GroupInfo } from '@shared/models/widget.models';
import { ColorPalette } from '@shared/models/material.models';
import { MouseEvent } from 'react';

export interface SchemaValidationResult {
  valid: boolean;
  error?: {
    message?: string;
  };
}

export interface FormOption {
  formDefaults?: {
    startEmpty?: boolean;
    readonly?: boolean;
  };
  supressPropertyTitles?: boolean;
}

export interface DefaultsFormOptions {
  global?: FormOption;
  required?: boolean;
  path?: string[];
  lookup?: {[key: string]: any};
  ignore?: {[key: string]: boolean};
}

export type onChangeFn = (key: (string | number)[], val: any, forceUpdate?: boolean) => void;
export type OnColorClickFn = (key: (string | number)[], val: tinycolor.ColorFormats.RGBA,
                              colorSelectedFn: (color: tinycolor.ColorFormats.RGBA) => void) => void;
export type OnIconClickFn = (key: (string | number)[], val: string,
                             iconSelectedFn: (icon: string) => void) => void;
export type onToggleFullscreenFn = (fullscreenFinishFn?: (el: Element) => void) => void;
export type onHelpClickFn = (event: MouseEvent, helpId: string, helpVisibleFn: (visible: boolean) => void,
                             helpReadyFn: (ready: boolean) => void) => void;

export interface JsonFormProps {
  model?: any;
  schema?: any;
  form?: any;
  groupInfoes?: GroupInfo[];
  isFullscreen: boolean;
  ignore?: {[key: string]: boolean};
  option: FormOption;
  onModelChange?: onChangeFn;
  onColorClick?: OnColorClickFn;
  onIconClick?: OnIconClickFn;
  onToggleFullscreen?: onToggleFullscreenFn;
  onHelpClick?: onHelpClickFn;
  isHelpEnabled?: boolean;
  mapper?: {[type: string]: any};
  primaryPalette?: ColorPalette;
  accentPalette?: ColorPalette;
}

export interface KeyLabelItem {
  key: string;
  label: string;
  value?: string;
}

export interface JsonSchemaData {
  type: string;
  default: any;
  items?: JsonSchemaData;
  properties?: any;
}

export interface JsonFormData {
  type: string;
  key: (string | number)[];
  title: string;
  readonly: boolean;
  required: boolean;
  default?: any;
  condition?: string;
  style?: any;
  rows?: number;
  rowsMax?: number;
  placeholder?: string;
  schema: JsonSchemaData;
  titleMap: {
    value: any;
    name: string;
  }[];
  items?: Array<KeyLabelItem> | Array<JsonFormData>;
  tabs?: Array<JsonFormData>;
  tags?: any;
  helpId?: string;
  startEmpty?: boolean;
  [key: string]: any;
}

export type ComponentBuilderFn = (form: JsonFormData,
                                  model: any,
                                  index: number,
                                  onChange: onChangeFn,
                                  onColorClick: OnColorClickFn,
                                  onIconClick: OnIconClickFn,
                                  onToggleFullscreen: onToggleFullscreenFn,
                                  onHelpClick: onHelpClickFn,
                                  isHelpEnabled: boolean,
                                  mapper: {[type: string]: any}) => JSX.Element;

export interface JsonFormFieldProps {
  value: any;
  model: any;
  form: JsonFormData;
  builder: ComponentBuilderFn;
  mapper?: {[type: string]: any};
  onChange?: onChangeFn;
  onColorClick?: OnColorClickFn;
  onIconClick?: OnIconClickFn;
  onChangeValidate?: (e: any, forceUpdate?: boolean) => void;
  onToggleFullscreen?: onToggleFullscreenFn;
  onHelpClick?: onHelpClickFn;
  isHelpEnabled?: boolean;
  valid?: boolean;
  error?: string;
  options?: {
    setSchemaDefaults?: boolean;
  };
}

export interface JsonFormFieldState {
  value?: any;
  valid?: boolean;
  error?: string;
}
