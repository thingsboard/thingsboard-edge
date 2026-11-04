// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import 'hammerjs';

import { Component } from '@angular/core';

import { environment as env } from '@env/environment';

import { TranslateService, TranslateStore } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { LocalStorageService } from '@core/local-storage/local-storage.service';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import { selectUserReady } from '@core/auth/auth.selectors';
import { filter, skip, tap } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { DashboardReportService } from '@core/http/dashboard-report.service';
import { DevelopmentService } from '@core/http/development.service';
import { svgIcons, svgIconsUrl } from '@shared/models/icon.models';
import { ActionSettingsChangeLanguage } from '@core/settings/settings.actions';
import { SETTINGS_KEY } from '@core/settings/settings.effects';
import { initCustomJQueryEvents } from '@shared/models/jquery-event.models';
import { TranslateDefaultLoader } from '@core/translate/translate-default-loader';

@Component({
    selector: 'tb-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
})
export class AppComponent {

  constructor(private store: Store<AppState>,
              private storageService: LocalStorageService,
              private translateStore: TranslateStore,
              private translate: TranslateService,
              private matIconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer,
              private authService: AuthService,
              private reportService: DashboardReportService,
              private developmentService: DevelopmentService) {

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

    this.developmentService.checkIsDevelopment();

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
    this.translateStore.setFallbackLang(env.defaultLang);
    this.translate.setTranslation(undefined, {
      "access": {
        "refresh-token-expired": "Session has expired",
        "refresh-token-failed": "Unable to refresh session"
      }
    }, true);
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

  onActivateComponent(_$event: any) {
    const loadingElement = $('div#tb-loading-spinner');
    if (loadingElement.length) {
      loadingElement.remove();
    }
  }

  private notifyUserLang(userLang: string, ignoredLoad = false) {
    this.store.dispatch(new ActionSettingsChangeLanguage({userLang, reload: true, ignoredLoad}));
  }

}
