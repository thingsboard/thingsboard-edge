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

import { AuthPayload, AuthState } from './auth.models';
import { AuthActions, AuthActionTypes } from './auth.actions';
import { initialUserSettings } from '@shared/models/user-settings.models';

const emptyUserAuthState: AuthPayload = {
  authUser: null,
  userDetails: null,
  userTokenAccessEnabled: false,
  forceFullscreen: false,
  edgesSupportEnabled: false,
  whiteLabelingAllowed: false,
  customerWhiteLabelingAllowed: false,
  hasRepository: false,
  tbelEnabled: false,
  persistDeviceStateToTelemetry: false,
  userSettings: initialUserSettings,
  allowedDashboardIds: []
};

export const initialState: AuthState = {
  isAuthenticated: false,
  isUserLoaded: false,
  lastPublicDashboardId: null,
  ...emptyUserAuthState
};

export const authReducer = (
  state: AuthState = initialState,
  action: AuthActions
): AuthState => {
  switch (action.type) {
    case AuthActionTypes.AUTHENTICATED:
      return { ...state, isAuthenticated: true, ...action.payload };

    case AuthActionTypes.UNAUTHENTICATED:
      return { ...state, isAuthenticated: false, ...emptyUserAuthState };

    case AuthActionTypes.LOAD_USER:
      return { ...state, ...action.payload, isAuthenticated: action.payload.isUserLoaded ? state.isAuthenticated : false,
        ...action.payload.isUserLoaded ? {} : emptyUserAuthState };

    case AuthActionTypes.UPDATE_USER_DETAILS:
      return { ...state, ...action.payload};

    case AuthActionTypes.UPDATE_LAST_PUBLIC_DASHBOARD_ID:
      return { ...state, ...action.payload};

    case AuthActionTypes.UPDATE_HAS_REPOSITORY:
      return { ...state, ...action.payload};

    case AuthActionTypes.UPDATE_OPENED_MENU_SECTION:
      const openedMenuSections = new Set(state.userSettings.openedMenuSections);
      if (action.payload.opened) {
        if (!openedMenuSections.has(action.payload.path)) {
          openedMenuSections.add(action.payload.path);
        }
      } else {
        openedMenuSections.delete(action.payload.path);
      }
      const userSettings = {...state.userSettings, ...{ openedMenuSections: Array.from(openedMenuSections)}};
      return { ...state, ...{ userSettings }};

    default:
      return state;
  }
};
