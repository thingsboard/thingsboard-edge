///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  checkWlParams,
  defaultLoginWlParams,
  LoginWhiteLabelingParams,
  mergeDefaults,
  PaletteSettings,
  tbAccentPalette,
  tbPrimaryPalette,
  WhiteLabelingParams
} from '@shared/models/white-labeling.models';
import { Observable, of, ReplaySubject } from 'rxjs';
import { ColorPalette, extendPalette, materialColorPalette } from '@shared/models/material.models';
import { deepClone, isEqual } from '@core/utils';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import { throwError } from 'rxjs/internal/observable/throwError';
import { environment as env } from '@env/environment';
import { ActionSettingsChangeWhiteLabeling } from '@core/settings/settings.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import cssjs from '@core/css/css';

const cssParser = new cssjs();
cssParser.testMode = false;

@Injectable({
  providedIn: 'root'
})
export class WhiteLabelingService {

  private changeWhiteLabelingSubject = new ReplaySubject(1);

  private loginLogo: string;
  private loginLogoHeight: number;
  private loginPageBackgroundColor: string;
  private loginShowNameVersion: boolean;
  private showNameBottom: boolean;
  private platformName: string;
  private platformVersion: string;

  public loginLogo$ = this.asWhiteLabelingObservable(() => this.loginLogo);
  public loginLogoHeight$ = this.asWhiteLabelingObservable(() => this.loginLogoHeight);
  public loginPageBackgroundColor$ = this.asWhiteLabelingObservable(() => this.loginPageBackgroundColor);
  public loginShowNameVersion$ = this.asWhiteLabelingObservable(() => this.loginShowNameVersion);
  public showNameBottom$ = this.asWhiteLabelingObservable(() => this.showNameBottom);
  public platformName$ = this.asWhiteLabelingObservable(() => this.platformName);
  public platformVersion$ = this.asWhiteLabelingObservable(() => this.platformVersion);

  private currentWLParams: WhiteLabelingParams;
  private currentLoginWLParams: LoginWhiteLabelingParams;
  private loginWlParams: LoginWhiteLabelingParams;
  private userWlParams: WhiteLabelingParams;

  private isUserWlMode = false;
  private isPreviewWlMode = false;

  private primaryPaletteName = 'tb-primary';
  private accentPaletteName = 'tb-accent';

  private PALETTES: {[palette: string]: ColorPalette} = deepClone(materialColorPalette);

  constructor(
    private http: HttpClient,
    private store: Store<AppState>,
    private utils: UtilsService,
  ) {
    this.definePalette('tb-primary', tbPrimaryPalette);
    this.definePalette('tb-accent', tbAccentPalette);
  }

  public logoImageUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().logoImageUrl : '';
  }

  public logoImageUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.logoImageUrl());
  }

  public logoImageHeight(): number {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().logoImageHeight: null;
  }

  public logoImageHeight$(): Observable<number> {
    return this.asWhiteLabelingObservable(() => this.logoImageHeight());
  }

  public appTitle(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().appTitle : '';
  }

  public appTitle$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.appTitle());
  }

  public faviconUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().favicon.url : '';
  }

  public faviconUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.faviconUrl());
  }

  public faviconType(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().favicon.type : '';
  }

  public faviconType$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.faviconType());
  }

  public getPrimaryPalette(): ColorPalette {
    return this.PALETTES[this.primaryPaletteName];
  }

  public getPrimaryColor(hue: string): string {
    return this.PALETTES[this.primaryPaletteName][hue];
  }

  public getAccentPalette(): ColorPalette {
    return this.PALETTES[this.accentPaletteName];
  }

  public getHelpLinkBaseUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().helpLinkBaseUrl : '';
  }

  public getHelpLinkBaseUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getHelpLinkBaseUrl());
  }

  public isEnableHelpLinks(): boolean {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().enableHelpLinks : true;
  }

  public isEnableHelpLinks$(): Observable<boolean> {
    return this.asWhiteLabelingObservable(() => this.isEnableHelpLinks());
  }

  public isShowVersion(): boolean {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().showNameVersion : false;
  }

  public isShowVersion$(): Observable<boolean> {
    return this.asWhiteLabelingObservable(() => this.isShowVersion());
  }

  public getPlatformName(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().platformName : '';
  }

  public getPlatformName$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getPlatformName());
  }

  public getPlatformVersion(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().platformVersion : '';
  }

  public getPlatformVersion$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getPlatformVersion());
  }

  public loadLoginWhiteLabelingParams(): Observable<LoginWhiteLabelingParams> {
    const storedLogoImageChecksum = localStorage.getItem('login_logo_image_checksum');
    const storedFaviconChecksum = localStorage.getItem('login_favicon_checksum');
    let url = '/api/noauth/whiteLabel/loginWhiteLabelParams';
    if (storedLogoImageChecksum) {
      url += `?logoImageChecksum=${storedLogoImageChecksum}`;
    }
    if (storedFaviconChecksum) {
      if (storedLogoImageChecksum) {
        url += '&'
      } else {
        url += '?'
      }
      url += `faviconChecksum=${storedFaviconChecksum}`;
    }
    return this.http.get<LoginWhiteLabelingParams>(url).pipe(
      mergeMap((loginWlParams) => {
        this.loginWlParams = mergeDefaults(loginWlParams, defaultLoginWlParams);
        this.updateImages(this.loginWlParams, 'login');
        return this.onLoginWlParamsLoaded().pipe(map(() => this.loginWlParams));
      }),
      catchError((err) => {
        if (this.loginWlParams) {
          return this.onLoginWlParamsLoaded().pipe(map(() => this.loginWlParams));
        } else {
          return throwError(err);
        }
      })
    );
  }

  private onLoginWlParamsLoaded(): Observable<any> {
    const loginWlChanged = this.setLoginWlParams(this.loginWlParams);
    let observable: Observable<any>;
    if (loginWlChanged) {
      this.applyLoginWlParams(this.currentLoginWLParams);
      this.applyCustomCss(this.currentLoginWLParams.customCss, true);
      observable = this.applyLoginThemePalettes(this.currentLoginWLParams.paletteSettings, this.currentLoginWLParams.darkForeground);
    } else {
      observable = of(null);
    }
    if (loginWlChanged || this.isUserWlMode) {
      this.isUserWlMode = false;
      observable = observable.pipe(tap(() => this.notifyWlChanged()));
    }
    return observable;
  }

  public loadUserWhiteLabelingParams(): Observable<WhiteLabelingParams> {
    const storedLogoImageChecksum = localStorage.getItem('user_logo_image_checksum');
    const storedFaviconChecksum = localStorage.getItem('user_favicon_checksum');
    let url = '/api/whiteLabel/whiteLabelParams';
    if (storedLogoImageChecksum) {
      url += `?logoImageChecksum=${storedLogoImageChecksum}`;
    }
    if (storedFaviconChecksum) {
      if (storedLogoImageChecksum) {
        url += '&'
      } else {
        url += '?'
      }
      url += `faviconChecksum=${storedFaviconChecksum}`;
    }
    return this.http.get<WhiteLabelingParams>(url).pipe(
      mergeMap((userWlParams) => {
        this.userWlParams = mergeDefaults(userWlParams);
        this.updateImages(this.userWlParams, 'user');
        return this.onUserWlParamsLoaded().pipe(map(() => this.userWlParams));
      }),
      catchError((err) => {
        if (this.userWlParams) {
          return this.onUserWlParamsLoaded().pipe(map(() => this.userWlParams));
        } else {
          return throwError(err);
        }
      })
    );
  }

  private onUserWlParamsLoaded(): Observable<any> {
    if (this.setWlParams(this.userWlParams) || !this.isUserWlMode) {
      this.isUserWlMode = true;
      return this.wlChanged();
    } else {
      return of(null);
    }
  }

  public whiteLabelPreview(wLParams: WhiteLabelingParams): Observable<WhiteLabelingParams> {
    return this.http.post<WhiteLabelingParams>('/api/whiteLabel/previewWhiteLabelParams', wLParams).pipe(
      mergeMap((previewWlParams) => {
        this.currentWLParams = mergeDefaults(previewWlParams);
        this.isPreviewWlMode = true;
        return this.wlChanged().pipe(map(() => previewWlParams));
      })
    );
  }

  public cancelWhiteLabelPreview(): Observable<any> {
    if (this.isPreviewWlMode) {
      this.isPreviewWlMode = false;
      this.currentWLParams = this.userWlParams;
      return this.wlChanged();
    } else {
      return of(null);
    }
  }

  public getCurrentWhiteLabelParams(): Observable<WhiteLabelingParams> {
    return this.http.get<WhiteLabelingParams>('/api/whiteLabel/currentWhiteLabelParams').pipe(
      map((wlParams) => {
        return checkWlParams(wlParams);
      })
    );
  }

  public getCurrentLoginWhiteLabelParams(): Observable<LoginWhiteLabelingParams> {
    return this.http.get<LoginWhiteLabelingParams>('/api/whiteLabel/currentLoginWhiteLabelParams').pipe(
      map((wlParams) => {
        return checkWlParams(wlParams);
      })
    );
  }

  public saveWhiteLabelParams(wlParams: WhiteLabelingParams): Observable<WhiteLabelingParams> {
    return this.http.post<WhiteLabelingParams>('/api/whiteLabel/whiteLabelParams', wlParams).pipe(
      mergeMap(() => {
        return this.loadUserWhiteLabelingParams();
      })
    );
  }

  public saveLoginWhiteLabelParams(wlParams: LoginWhiteLabelingParams): Observable<LoginWhiteLabelingParams> {
    return this.http.post<WhiteLabelingParams>('/api/whiteLabel/loginWhiteLabelParams', wlParams);
  }

  public isWhiteLabelingAllowed(): Observable<boolean> {
    return this.http.get<boolean>('/api/whiteLabel/isWhiteLabelingAllowed');
  }

  public isCustomerWhiteLabelingAllowed(): Observable<boolean> {
    return this.http.get<boolean>('/api/whiteLabel/isCustomerWhiteLabelingAllowed');
  }

  private getLoginThemeCss(paletteSettings: PaletteSettings, darkForeground: boolean): Observable<string> {
    return this.http.post(`/api/noauth/whiteLabel/loginThemeCss?darkForeground=${darkForeground}`, paletteSettings,
      {
        responseType: 'text'
      }
    );
  }

  private getAppThemeCss(paletteSettings: PaletteSettings): Observable<string> {
    return this.http.post('/api/whiteLabel/appThemeCss', paletteSettings,
      {
        responseType: 'text'
      }
    );
  }

  private wlChanged(): Observable<any> {
    this.applyCustomCss(this.currentWLParams.customCss, false);
    return this.applyThemePalettes(this.currentWLParams.paletteSettings).pipe(
      tap(() => {
        this.notifyWlChanged();
      })
    );
  }

  private notifyWlChanged() {
    this.store.dispatch(new ActionSettingsChangeWhiteLabeling({}));
    this.changeWhiteLabelingSubject.next();
  }

  private getCurrentWlParams(): WhiteLabelingParams {
    return this.isUserWlMode ? this.currentWLParams : this.currentLoginWLParams;
  }

  private definePalette(paletteName: string, palette: ColorPalette) {
    this.PALETTES[paletteName] = palette;
  }

  private setLoginWlParams(newWlParams: LoginWhiteLabelingParams): boolean {
    if (!isEqual(this.currentLoginWLParams, newWlParams)) {
      this.currentLoginWLParams = newWlParams;
      return true;
    } else {
      return false;
    }
  }

  private setWlParams(newWlParams: WhiteLabelingParams) {
    if (!isEqual(this.currentWLParams, newWlParams)) {
      this.currentWLParams = newWlParams;
      return true;
    } else {
      return false;
    }
  }

  private applyThemePalettes(paletteSettings: PaletteSettings): Observable<any> {
    this.cleanupPalettes('custom-primary');
    this.cleanupPalettes('custom-accent');

    const primaryPalette = paletteSettings.primaryPalette;
    const accentPalette = paletteSettings.accentPalette;

    if (primaryPalette.type === 'tb-primary' &&
      accentPalette.type === 'tb-accent') {
      this.primaryPaletteName = primaryPalette.type;
      this.accentPaletteName = accentPalette.type;
      this.cleanupThemeStyle(false);
      return of(null);
    }

    if (primaryPalette.type !== 'custom') {
      this.primaryPaletteName = primaryPalette.type;
    } else {
      this.primaryPaletteName = 'custom-primary';
      const customPrimaryPalette = extendPalette(this.PALETTES, primaryPalette.extends, primaryPalette.colors);
      this.definePalette(this.primaryPaletteName, customPrimaryPalette);
    }
    if (accentPalette.type !== 'custom') {
      this.accentPaletteName = accentPalette.type;
    } else {
      this.accentPaletteName = 'custom-accent';
      const customAccentPalette = extendPalette(this.PALETTES, accentPalette.extends, accentPalette.colors);
      this.definePalette(this.accentPaletteName, customAccentPalette);
    }
    return this.generateThemeStyle(paletteSettings, false, false);
  }

  private applyLoginWlParams(wlParams: LoginWhiteLabelingParams) {
    this.loginLogo = wlParams.logoImageUrl;
    this.loginLogoHeight = wlParams.logoImageHeight;
    this.loginPageBackgroundColor = wlParams.pageBackgroundColor;
    this.loginShowNameVersion = wlParams.showNameVersion;
    this.showNameBottom = wlParams.showNameBottom;
    this.platformName = !wlParams.platformName ? 'ThingsBoard' : wlParams.platformName;
    this.platformVersion = !wlParams.platformVersion ? env.tbVersion : wlParams.platformVersion;
  }

  private applyLoginThemePalettes(paletteSettings: PaletteSettings, darkForeground: boolean): Observable<any> {
    const primaryPalette = paletteSettings.primaryPalette;
    const accentPalette = paletteSettings.accentPalette;
    if (primaryPalette.type === 'tb-primary' &&
      accentPalette.type === 'tb-accent' && !darkForeground) {
      this.cleanupThemeStyle(true);
      return of(null);
    }
    return this.generateThemeStyle(paletteSettings, true, darkForeground);
  }

  private cleanupPalettes(prefix: string) {
    for (const palette in this.PALETTES) {
      if (palette.startsWith(prefix)) {
        delete this.PALETTES[palette];
      }
    }
  }

  private generateThemeStyle(paletteSettings: PaletteSettings,
                             isLoginTheme: boolean,
                             darkForeground: boolean): Observable<any> {
    let themeChecksum;
    if (isLoginTheme) {
      themeChecksum = this.utils.objectHashCode({...paletteSettings, ...{darkForeground}});
    } else {
      themeChecksum = this.utils.objectHashCode(paletteSettings);
    }
    const prefix = isLoginTheme ? 'tb-login' : 'tb-app';
    const storedThemeChecksum = localStorage.getItem(prefix+'_theme_checksum');
    const storedThemeCss = localStorage.getItem(prefix+'_theme_css');
    let themeCssObservable: Observable<string>;
    let storeCssTheme;
    if (!storedThemeChecksum || !isEqual(themeChecksum, storedThemeChecksum)
      || !storedThemeCss || !storedThemeCss.length) {
      storeCssTheme = true;
      themeCssObservable = isLoginTheme ? this.getLoginThemeCss(paletteSettings, darkForeground) :
        this.getAppThemeCss(paletteSettings);
    } else {
      storeCssTheme = false;
      themeCssObservable = of(storedThemeCss);
    }
    return themeCssObservable.pipe(
      tap((themeCss) => {
        if (storeCssTheme) {
          localStorage.setItem(prefix+'_theme_checksum', themeChecksum);
          localStorage.setItem(prefix+'_theme_css', themeCss);
        }
        this.applyThemeStyle(themeCss, isLoginTheme);
      })
    );
  }

  private cleanupThemeStyle(isLoginTheme: boolean) {
    const target = isLoginTheme ? 'tb-login-theme' : 'tb-app-theme';
    const targetStyle = $(`#${target}`);
    if (targetStyle.length) {
      targetStyle.text('');
    }
  }

  private applyThemeStyle(themeCss: string, isLoginTheme: boolean) {
    const favicon = $('link[rel="icon"]');
    const afterStyle = favicon.next('style').next('style');
    const target = isLoginTheme ? 'tb-login-theme' : 'tb-app-theme';
    let targetStyle = $(`#${target}`);
    if (!targetStyle.length) {
      targetStyle = $(`<style id="${target}"></style>`);
      if (isLoginTheme) {
        const appThemeStyle = $('#tb-app-theme');
        if (!appThemeStyle.length) {
          targetStyle.insertAfter(afterStyle);
        } else {
          targetStyle.insertAfter(appThemeStyle);
        }
      } else {
        const loginThemeStyle = $('#tb-login-theme');
        if (!loginThemeStyle.length) {
          targetStyle.insertAfter(afterStyle);
        } else {
          targetStyle.insertBefore(loginThemeStyle);
        }
      }
    }
    targetStyle.text(themeCss);
  }

  private applyCustomCss(customCss: string, isLoginTheme: boolean) {
    const target = isLoginTheme ? 'tb-login-custom-css' : 'tb-app-custom-css';
    let targetStyle = $(`#${target}`);
    if (!targetStyle.length) {
      targetStyle = $(`<style id="${target}"></style>`);
      $('head').append(targetStyle);
    }
    let css;
    if (customCss && customCss.length) {
      const namespace = isLoginTheme ? 'tb-dark' : 'tb-default';
      cssParser.cssPreviewNamespace = namespace;
      css = cssParser.applyNamespacing(customCss);
      if (typeof css !== 'string') {
        css = cssParser.getCSSForEditor(css);
      }
    } else {
      css = '';
    }
    targetStyle.text(css);
  }

  private updateImages(wlParams: WhiteLabelingParams, prefix: string) {
    const storedLogoImageChecksum = localStorage.getItem(prefix+'_logo_image_checksum');
    const storedFaviconChecksum = localStorage.getItem(prefix+'_favicon_checksum');
    const logoImageChecksum = wlParams.logoImageChecksum;
    if (logoImageChecksum && !isEqual(storedLogoImageChecksum, logoImageChecksum)) {
      const logoImageUrl = wlParams.logoImageUrl;
      localStorage.setItem(prefix+'_logo_image_checksum', logoImageChecksum);
      localStorage.setItem(prefix+'_logo_image_url', logoImageUrl);
    } else {
      wlParams.logoImageUrl = localStorage.getItem(prefix+'_logo_image_url');
    }
    const faviconChecksum = wlParams.faviconChecksum;
    if (faviconChecksum && !isEqual(storedFaviconChecksum, faviconChecksum)) {
      const favicon = wlParams.favicon;
      localStorage.setItem(prefix+'_favicon_checksum', faviconChecksum);
      localStorage.setItem(prefix+'_favicon_url', favicon.url);
      localStorage.setItem(prefix+'_favicon_type', favicon.type);
    } else {
      wlParams.favicon = {
        url: localStorage.getItem(prefix+'_favicon_url'),
        type: localStorage.getItem(prefix+'_favicon_type'),
      };
    }
  }

  private asWhiteLabelingObservable<T> (valueSource: () => T): Observable<T> {
    return this.changeWhiteLabelingSubject.pipe(
      map(() => valueSource())
    );
  }

}
