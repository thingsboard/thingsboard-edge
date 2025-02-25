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

import { ActivationEnd, Router } from '@angular/router';
import { Inject, Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { TranslateService } from '@ngx-translate/core';
import { merge } from 'rxjs';
import { filter, tap, withLatestFrom } from 'rxjs/operators';

import { SettingsActions, SettingsActionTypes, } from './settings.actions';
import { selectSettingsState } from './settings.selectors';
import { AppState } from '@app/core/core.state';
import { LocalStorageService } from '@app/core/local-storage/local-storage.service';
import { TitleService } from '@app/core/services/title.service';
import { updateUserLang } from '@app/core/settings/settings.utils';
import { UtilsService } from '@core/services/utils.service';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { ActionAuthUpdateLastPublicDashboardId } from '../auth/auth.actions';
import { DOCUMENT } from '@angular/common';
import { FaviconService } from '@core/services/favicon.service';
import { ReportService } from '@core/http/report.service';

export const SETTINGS_KEY = 'SETTINGS';

@Injectable()
export class SettingsEffects {
  constructor(
    private actions$: Actions<SettingsActions>,
    private store: Store<AppState>,
    private utils: UtilsService,
    private router: Router,
    private localStorageService: LocalStorageService,
    private titleService: TitleService,
    private translate: TranslateService,
    @Inject(DOCUMENT) private document: Document,
    private faviconService: FaviconService,
    private reportService: ReportService,
  ) {
  }

  setTranslateServiceLanguage = createEffect(() => this.actions$.pipe(
    ofType(
      SettingsActionTypes.CHANGE_LANGUAGE,
    ),
    withLatestFrom(this.store.pipe(select(selectSettingsState))),
    tap(([action, settings]) => {
      this.localStorageService.setItem(SETTINGS_KEY, {userLang: settings.userLang});
      if (!settings.ignoredLoad) {
        const availableLocales = getCurrentAuthState(this.store)?.availableLocales;
        updateUserLang(this.translate, this.document, settings.userLang, availableLocales, settings.reload)
          .subscribe(() => {});
      }
    })
  ), {dispatch: false});

  setTitle = createEffect(() => merge(
    this.actions$.pipe(ofType(SettingsActionTypes.CHANGE_LANGUAGE, SettingsActionTypes.CHANGE_WHITE_LABELING)),
    this.router.events.pipe(filter(event => event instanceof ActivationEnd))
  ).pipe(
    tap(() => {
      this.titleService.setTitle(
        this.router.routerState.snapshot.root,
        this.translate
      );
    })
  ), {dispatch: false});

  setFavicon = createEffect(() => merge(
    this.actions$.pipe(ofType(SettingsActionTypes.CHANGE_WHITE_LABELING)),
  ).pipe(
    tap(() => {
      this.faviconService.setFavicon();
    })
  ), {dispatch: false});

  setPublicId = createEffect(() => merge(
    this.router.events.pipe(filter(event => event instanceof ActivationEnd))
  ).pipe(
    tap((event) => {
      const authUser = getCurrentAuthUser(this.store);
      const snapshot = (event as ActivationEnd).snapshot;
      if (!this.reportService.reportView && authUser && authUser.isPublic && snapshot.url && snapshot.url.length
          && snapshot.url[0].path === 'dashboard') {
        this.utils.updateQueryParam('publicId', authUser.sub);
        this.store.dispatch(new ActionAuthUpdateLastPublicDashboardId(
          { lastPublicDashboardId: snapshot.params.dashboardId}));
      }
    })
  ), {dispatch: false});
}
