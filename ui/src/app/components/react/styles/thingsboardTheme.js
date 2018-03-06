/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
import { /*blueGrey500, blueGrey700, blueGrey100, orange500,*/
         grey100, grey500, grey900, grey600, white, grey400, darkBlack, cyan500, fullBlack/*, indigo500*/, indigo700, indigo100, deepOrange500 } from 'material-ui/styles/colors';
import {fade} from 'material-ui/utils/colorManipulator';
import spacing from 'material-ui/styles/spacing';

const PRIMARY_BACKGROUND_COLOR = "#305680";//"#3f51b5";

var thingsboardPalette = {
    primary1Color: PRIMARY_BACKGROUND_COLOR,
    primary2Color: indigo700,
    primary3Color: indigo100,
    accent1Color: deepOrange500,
    accent2Color: grey100,
    accent3Color: grey500,
    textColor: grey900,
    secondaryTextColor: grey600,
    alternateTextColor: white,
    canvasColor: white,
    borderColor: grey400,
    disabledColor: fade(darkBlack, 0.3),
    pickerHeaderColor: cyan500,
    clockCircleColor: fade(darkBlack, 0.07),
    shadowColor: fullBlack,
};

export default function createThingsboardTheme (primaryPalette, accentPalette) {
    thingsboardPalette.primary1Color = primaryPalette ? primaryPalette['500'].hex : PRIMARY_BACKGROUND_COLOR;
    thingsboardPalette.primary2Color = primaryPalette ? primaryPalette['700'].hex : indigo700;
    thingsboardPalette.primary3Color = primaryPalette ? primaryPalette['100'].hex : indigo100;
    thingsboardPalette.accent1Color = accentPalette ? accentPalette['500'].hex : deepOrange500;

    return {
        spacing: spacing,
        fontFamily: 'Roboto, \'Helvetica Neue\', sans-serif',
        palette: thingsboardPalette,
    };
}

