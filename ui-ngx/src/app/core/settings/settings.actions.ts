///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Action } from '@ngrx/store';

export enum SettingsActionTypes {
  CHANGE_LANGUAGE = '[Settings] Change Language',
  CHANGE_WHITE_LABELING = '[Settings] Change White-labeling',
}

export class ActionSettingsChangeLanguage implements Action {
  readonly type = SettingsActionTypes.CHANGE_LANGUAGE;

  constructor(readonly payload: { userLang: string }) {}
}

export class ActionSettingsChangeWhiteLabeling implements Action {
  readonly type = SettingsActionTypes.CHANGE_WHITE_LABELING;

  constructor(readonly payload: {}) {}
}

export type SettingsActions =
  | ActionSettingsChangeLanguage | ActionSettingsChangeWhiteLabeling;
