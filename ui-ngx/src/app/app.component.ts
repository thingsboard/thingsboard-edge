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

import 'hammerjs';

import { Component, OnInit } from '@angular/core';

import { environment as env } from '@env/environment';

import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import { selectUserReady } from '@core/auth/auth.selectors';
import { filter, skip, tap } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { ReportService } from '@core/http/report.service';
import { svgIcons, svgIconsUrl } from '@shared/models/icon.models';
import { ActionSettingsChangeLanguage } from '@core/settings/settings.actions';
import { SETTINGS_KEY } from '@core/settings/settings.effects';
import { initCustomJQueryEvents } from '@shared/models/jquery-event.models';
import { TranslateDefaultLoader } from '@core/translate/translate-default-loader';

@Component({
  selector: 'tb-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(private store: Store<AppState>,
              private storageService: LocalStorageService,
              private translate: TranslateService,
              private matIconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer,
              private authService: AuthService,
              private reportService: ReportService) {

    if (!env.production) {
      console.log(`ThingsBoard Version: ${env.tbVersion}`);
    }

    this.matIconRegistry.addSvgIconResolver((name, namespace) => {
      if (namespace === 'mdi') {
        return this.domSanitizer.bypassSecurityTrustResourceUrl(`./assets/mdi/${name}.svg`);
      } else {
        return null;
      }
    });

    for (const svgIcon of Object.keys(svgIcons)) {
      this.matIconRegistry.addSvgIconLiteral(
        svgIcon,
        this.domSanitizer.bypassSecurityTrustHtml(
          svgIcons[svgIcon]
        )
      );
    }

    for (const svgIcon of Object.keys(svgIconsUrl)) {
      this.matIconRegistry.addSvgIcon(svgIcon, this.domSanitizer.bypassSecurityTrustResourceUrl(svgIconsUrl[svgIcon]));
    }

    this.storageService.testLocalStorage();

    this.setupTranslate();
    this.setupAuth();

    initCustomJQueryEvents();
  }

  setupTranslate() {
    if (!env.production) {
      console.log(`Supported Langs: ${env.supportedLangs}`);
    }
    this.translate.addLangs(env.supportedLangs);
    if (!env.production) {
      console.log(`Default Lang: ${env.defaultLang}`);
    }
    // @ts-ignore
    this.translate.changeDefaultLang(env.defaultLang);
  }

  setupAuth() {
    this.store.select(selectUserReady).pipe(
      filter((data) => data.isUserLoaded),
      tap((data) => {
        if (!data.isAuthenticated) {
          const settings = this.storageService.getItem(SETTINGS_KEY);
          const userLang = settings?.userLang ?? null;
          (this.translate.currentLoader as TranslateDefaultLoader).isAuthenticated = false;
          this.notifyUserLang(userLang);
        } else {
          this.notifyUserLang(this.translate.currentLang, true);
        }
      }),
      skip(1),
    ).subscribe((data) => {
      this.authService.gotoDefaultPlace(data.isAuthenticated);
    });
    if (!this.reportService.loadReportParams()) {
      this.authService.reloadUser();
    }
  }

  ngOnInit() {
  }

  onActivateComponent($event: any) {
    const loadingElement = $('div#tb-loading-spinner');
    if (loadingElement.length) {
      loadingElement.remove();
    }
  }

  private notifyUserLang(userLang: string, ignoredLoad = false) {
    this.store.dispatch(new ActionSettingsChangeLanguage({userLang, reload: true, ignoredLoad}));
  }

}
