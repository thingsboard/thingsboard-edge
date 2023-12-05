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

import { environment as env } from '@env/environment';
import { deepClone, isDefined, isUndefinedOrNull } from '@core/utils';
import { ColorPalette, extendDefaultPalette } from '@shared/models/material.models';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityType } from '@shared/models/entity-type.models';

export interface Favicon {
  url?: string;
}

export interface Palette {
  type: string;
  extends?: string;
  colors?: ColorPalette;
}

export interface PaletteSettings {
  primaryPalette?: Palette;
  accentPalette?: Palette;
}

export interface WhiteLabelingParams {
  logoImageUrl?: string;
  logoImageHeight?: number;
  appTitle?: string;
  favicon?: Favicon;
  paletteSettings?: PaletteSettings;
  helpLinkBaseUrl?: string;
  uiHelpBaseUrl?: string;
  enableHelpLinks?: boolean;
  showNameVersion?: boolean;
  platformName?: string;
  platformVersion?: string;
  customCss?: string;
}

export interface LoginWhiteLabelingParams extends WhiteLabelingParams {
  pageBackgroundColor?: string;
  darkForeground?: boolean;
  domainName?: string;
  adminSettingsId?: string;
  showNameBottom?: boolean;
}

const defaultImageUrl = 'assets/logo_title_white_pe.svg';

export const defaultWLParams: WhiteLabelingParams = {
  logoImageUrl: defaultImageUrl,
  logoImageHeight: 36,
  appTitle: 'ThingsBoard PE',
  favicon: {
    url: 'thingsboard.ico'
  },
  paletteSettings: {
    primaryPalette: {
      type: 'tb-primary'
    },
    accentPalette: {
      type: 'tb-accent'
    }
  },
  helpLinkBaseUrl: 'https://thingsboard.io',
  enableHelpLinks: true,
  showNameVersion: false,
  platformName: 'ThingsBoard',
  platformVersion: env.tbVersion
};

const loginWlParams = deepClone(defaultWLParams) as LoginWhiteLabelingParams;
loginWlParams.logoImageHeight = 50;
loginWlParams.pageBackgroundColor = '#eee';
loginWlParams.darkForeground = false;

export const defaultLoginWlParams = loginWlParams;

export const tbPrimaryPalette: ColorPalette = extendDefaultPalette('teal', {
  500: '#00695c'
});
export const tbAccentPalette: ColorPalette = extendDefaultPalette('deep-orange', {});

export const tbLoginPrimaryPalette: ColorPalette = extendDefaultPalette('teal', {
  200: '#00c3b6',
  500: '#00695c'
});
export const tbLoginAccentPalette: ColorPalette = extendDefaultPalette('deep-orange', {});

export const mergeDefaults = <T extends WhiteLabelingParams & LoginWhiteLabelingParams>(wlParams: T,
                              targetDefaultWlParams?: T): T => {
  if (!targetDefaultWlParams) {
    targetDefaultWlParams = defaultWLParams as T;
  }
  if (!wlParams) {
    wlParams = {} as T;
  }
  if (!wlParams.pageBackgroundColor && targetDefaultWlParams.pageBackgroundColor) {
    wlParams.pageBackgroundColor = targetDefaultWlParams.pageBackgroundColor;
  }
  if (!wlParams.logoImageUrl) {
    wlParams.logoImageUrl = targetDefaultWlParams.logoImageUrl;
  }
  if (!wlParams.logoImageHeight) {
    wlParams.logoImageHeight = targetDefaultWlParams.logoImageHeight;
  }
  if (!wlParams.appTitle) {
    wlParams.appTitle = targetDefaultWlParams.appTitle;
  }
  if (!wlParams.favicon || !wlParams.favicon.url) {
    wlParams.favicon = targetDefaultWlParams.favicon;
  }
  if (!wlParams.paletteSettings) {
    wlParams.paletteSettings = targetDefaultWlParams.paletteSettings;
  } else {
    if (!wlParams.paletteSettings.primaryPalette || !wlParams.paletteSettings.primaryPalette.type) {
      wlParams.paletteSettings.primaryPalette = targetDefaultWlParams.paletteSettings.primaryPalette;
    }
    if (!wlParams.paletteSettings.accentPalette || !wlParams.paletteSettings.accentPalette.type) {
      wlParams.paletteSettings.accentPalette = targetDefaultWlParams.paletteSettings.accentPalette;
    }
  }
  if (!wlParams.helpLinkBaseUrl && targetDefaultWlParams.helpLinkBaseUrl) {
    wlParams.helpLinkBaseUrl = targetDefaultWlParams.helpLinkBaseUrl;
  }
  if (isUndefinedOrNull(wlParams.enableHelpLinks) && isDefined(targetDefaultWlParams.enableHelpLinks)) {
    wlParams.enableHelpLinks = targetDefaultWlParams.enableHelpLinks;
  }
  if (isUndefinedOrNull(wlParams.showNameVersion)) {
    wlParams.showNameVersion = targetDefaultWlParams.showNameVersion;
  }
  if (wlParams.platformName === null) {
    wlParams.platformName = targetDefaultWlParams.platformName;
  }
  if (wlParams.platformVersion === null) {
    wlParams.platformVersion = targetDefaultWlParams.platformVersion;
  }
  return wlParams;
};

export const checkWlParams = <T extends WhiteLabelingParams & LoginWhiteLabelingParams>(whiteLabelParams: T): T  => {
  if (!whiteLabelParams) {
    whiteLabelParams = {} as T;
  }
  if (!whiteLabelParams.paletteSettings) {
    whiteLabelParams.paletteSettings = {};
  }
  if (!whiteLabelParams.favicon) {
    whiteLabelParams.favicon = {};
  }
  return whiteLabelParams;
};

export enum WhiteLabelingType {
  LOGIN = 'LOGIN',
  GENERAL = 'GENERAL',
  MAIL_TEMPLATES = 'MAIL_TEMPLATES'
}

export interface WhiteLabeling {
  tenantId: TenantId;
  customerId?: CustomerId;
  type: WhiteLabelingType;
  settings?: any;
  domain?: string;
}

export const pageByWhiteLabelingType = new Map<WhiteLabelingType, string>([
    [WhiteLabelingType.GENERAL, '/white-labeling/whiteLabel'],
    [WhiteLabelingType.LOGIN, '/white-labeling/loginWhiteLabel']
]);
