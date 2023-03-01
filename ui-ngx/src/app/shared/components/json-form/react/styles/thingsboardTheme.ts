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

import { indigo, deepOrange } from '@material-ui/core/colors';
import { ThemeOptions } from '@material-ui/core/styles';
import { PaletteOptions, SimplePaletteColorOptions } from '@material-ui/core/styles/createPalette';
import { mergeDeep } from '@core/utils';
import { ColorPalette } from '@shared/models/material.models';

const PRIMARY_COLOR = '#305680';
const SECONDARY_COLOR = '#527dad';
const HUE3_COLOR = '#a7c1de';

const tbIndigo = mergeDeep<any>({}, indigo, {
  500: PRIMARY_COLOR,
  600: SECONDARY_COLOR,
  700: PRIMARY_COLOR,
  A100: HUE3_COLOR
});

const thingsboardPalette: PaletteOptions = {
  primary: tbIndigo,
  secondary: deepOrange,
  background: {
    default: '#eee'
  },
  type: 'light'
};

export default function createThingsboardTheme(primaryPalette: ColorPalette, accentPalette: ColorPalette): ThemeOptions {
  thingsboardPalette.primary = mergeDeep<any>({}, thingsboardPalette.primary, primaryPalette);
  thingsboardPalette.secondary = mergeDeep<any>({}, thingsboardPalette.secondary, accentPalette);
  (thingsboardPalette.secondary as SimplePaletteColorOptions).main = thingsboardPalette.secondary['500'];
  return {
    typography: {
      fontFamily: 'Roboto, \'Helvetica Neue\', sans-serif'
    },
    palette: thingsboardPalette
  };
}
