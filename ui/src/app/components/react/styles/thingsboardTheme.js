/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

