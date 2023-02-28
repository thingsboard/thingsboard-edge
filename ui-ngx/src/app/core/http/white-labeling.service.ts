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

import { Inject, Injectable, Renderer2, RendererFactory2, RendererStyleFlags2 } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  checkWlParams,
  defaultLoginWlParams,
  LoginWhiteLabelingParams,
  mergeDefaults,
  Palette,
  PaletteSettings,
  tbAccentPalette,
  tbLoginAccentPalette,
  tbLoginPrimaryPalette,
  tbPrimaryPalette,
  WhiteLabelingParams
} from '@shared/models/white-labeling.models';
import { Observable, of, ReplaySubject, throwError } from 'rxjs';
import {
  ColorPalette,
  extendDefaultPalette,
  getContrastColor,
  materialColorPalette
} from '@shared/models/material.models';
import { isEqual } from '@core/utils';
import { catchError, map, mergeMap, tap } from 'rxjs/operators';
import { environment as env } from '@env/environment';
import { ActionSettingsChangeWhiteLabeling } from '@core/settings/settings.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UtilsService } from '@core/services/utils.service';
import cssjs from '@core/css/css';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { DOCUMENT } from '@angular/common';

const cssParser = new cssjs();
cssParser.testMode = false;

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class WhiteLabelingService {

  private changeWhiteLabelingSubject = new ReplaySubject(1);

  private loginLogo: string;
  private loginLogoSafeUrl: SafeUrl;
  private loginLogoHeight: number;
  private loginPageBackgroundColor: string;
  private loginShowNameVersion: boolean;
  private loginDarkForeground: boolean;
  private showNameBottom: boolean;
  private platformName: string;
  private platformVersion: string;

  public loginLogo$ = this.asWhiteLabelingObservable(() => this.loginLogo);
  public loginLogoSafeUrl$ = this.asWhiteLabelingObservable(() => this.loginLogoSafeUrl);
  public loginLogoHeight$ = this.asWhiteLabelingObservable(() => this.loginLogoHeight);
  public loginPageBackgroundColor$ = this.asWhiteLabelingObservable(() => this.loginPageBackgroundColor);
  public loginShowNameVersion$ = this.asWhiteLabelingObservable(() => this.loginShowNameVersion);
  public loginDarkForeground$ = this.asWhiteLabelingObservable(() => this.loginDarkForeground);
  public showNameBottom$ = this.asWhiteLabelingObservable(() => this.showNameBottom);
  public platformName$ = this.asWhiteLabelingObservable(() => this.platformName);
  public platformVersion$ = this.asWhiteLabelingObservable(() => this.platformVersion);

  private currentWLParams: WhiteLabelingParams;
  private currentLoginWLParams: LoginWhiteLabelingParams;
  private loginWlParams: LoginWhiteLabelingParams;
  private userWlParams: WhiteLabelingParams;

  private isUserWlMode = false;
  private isPreviewWlMode = false;

  private primaryPalette: Palette = {
    type: 'tb-primary',
    colors: tbPrimaryPalette,
    extends: 'teal'
  };

  private accentPalette: Palette = {
    type: 'tb-accent',
    colors: tbAccentPalette,
    extends: 'deep-orange'
  };

  private loginPrimaryPalette: Palette = {
    type: 'tb-primary',
    colors: tbLoginPrimaryPalette,
    extends: 'teal'
  };

  private loginAccentPalette: Palette = {
    type: 'tb-accent',
    colors: tbAccentPalette,
    extends: 'deep-orange'
  };

  private renderer: Renderer2;
  private readonly ROOT: HTMLElement;

  constructor(
    private http: HttpClient,
    private store: Store<AppState>,
    private utils: UtilsService,
    private sanitizer: DomSanitizer,
    rendererFactory: RendererFactory2,
    @Inject(DOCUMENT) private document: Document
  ) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.ROOT = this.document.documentElement;
  }

  public logoImageUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().logoImageUrl : '';
  }

  public logoImageSafeUrl(): SafeUrl {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().logoImageSafeUrl : '';
  }

  public logoImageUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.logoImageUrl());
  }

  public logoImageSafeUrl$(): Observable<SafeUrl> {
    return this.asWhiteLabelingObservable(() => this.logoImageSafeUrl());
  }

  public logoImageHeight(): number {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().logoImageHeight : null;
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
    return this.primaryPalette.colors;
  }

  public getPrimaryColor(hue: string): string {
    return this.primaryPalette.colors[hue];
  }

  public getAccentPalette(): ColorPalette {
    return this.accentPalette.colors;
  }

  public getLoginPrimaryPalette(): ColorPalette {
    return this.loginPrimaryPalette.colors;
  }

  public getLoginAccentPalette(): ColorPalette {
    return this.loginAccentPalette.colors;
  }

  public getHelpLinkBaseUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().helpLinkBaseUrl : '';
  }

  public getHelpLinkBaseUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getHelpLinkBaseUrl());
  }

  public getUiHelpBaseUrl(): string {
    return this.getCurrentWlParams() ? this.getCurrentWlParams().uiHelpBaseUrl : '';
  }

  public getUiHelpBaseUrl$(): Observable<string> {
    return this.asWhiteLabelingObservable(() => this.getUiHelpBaseUrl());
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
        url += '&';
      } else {
        url += '?';
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
      applyCustomCss(this.currentLoginWLParams.customCss, true);
      observable = this.applyLoginThemePalettes(this.currentLoginWLParams.paletteSettings);
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
        url += '&';
      } else {
        url += '?';
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
        this.currentWLParams.logoImageSafeUrl = this.sanitizer.bypassSecurityTrustUrl(this.currentWLParams.logoImageUrl);
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

  private wlChanged(): Observable<any> {
    applyCustomCss(this.currentWLParams.customCss, false);
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
    this.primaryPalette = this.configurePalette(paletteSettings.primaryPalette, tbPrimaryPalette, 'tb-primary', 'teal');
    this.accentPalette = this.configurePalette(paletteSettings.accentPalette, tbAccentPalette, 'tb-accent', 'deep-orange');
    this.applyThemeColors(false);
    return of(null);
  }

  private applyLoginThemePalettes(paletteSettings: PaletteSettings): Observable<any> {
    this.loginPrimaryPalette = this.configurePalette(paletteSettings.primaryPalette, tbLoginPrimaryPalette,  'tb-primary', 'teal');
    this.loginAccentPalette = this.configurePalette(paletteSettings.accentPalette, tbLoginAccentPalette, 'tb-accent', 'deep-orange');
    this.applyThemeColors(true);
    return of(null);
  }


  private applyLoginWlParams(wlParams: LoginWhiteLabelingParams) {
    this.loginLogo = wlParams.logoImageUrl;
    this.loginLogoSafeUrl = wlParams.logoImageSafeUrl;
    this.loginLogoHeight = wlParams.logoImageHeight;
    this.loginPageBackgroundColor = wlParams.pageBackgroundColor;
    this.loginShowNameVersion = wlParams.showNameVersion;
    this.showNameBottom = wlParams.showNameBottom;
    this.platformName = !wlParams.platformName ? 'ThingsBoard' : wlParams.platformName;
    this.platformVersion = !wlParams.platformVersion ? env.tbVersion : wlParams.platformVersion;
    this.loginDarkForeground = wlParams.darkForeground;
  }

  private configurePalette(paletteConfig: Palette, defaultColors: ColorPalette,
                           defaultType: string, defaultExtends: string): Palette {
    if (paletteConfig.type === defaultType) {
      return {
        type: defaultType,
        extends: defaultExtends,
        colors: defaultColors
      };
    } else {
      if (paletteConfig.type !== 'custom') {
        return {
          type: paletteConfig.type,
          extends: paletteConfig.type,
          colors: materialColorPalette[paletteConfig.type]
        };
      } else {
        return {
          type: 'custom',
          extends: paletteConfig.extends,
          colors: extendDefaultPalette(paletteConfig.extends, paletteConfig.colors)
        };
      }
    }
  }

  private applyThemeColors(isLoginTheme: boolean) {
    const primaryPalette = isLoginTheme ? this.loginPrimaryPalette : this.primaryPalette;
    const accentPalette = isLoginTheme ? this.loginAccentPalette : this.accentPalette;
    const primaryPrefix = isLoginTheme ? '--tb-login-primary-' : '--tb-primary-';
    const accentPrefix = isLoginTheme ? '--tb-login-accent-' : '--tb-accent-';
    this.applyPaletteColors(primaryPalette, primaryPrefix);
    this.applyPaletteColors(accentPalette, accentPrefix);
  }

  private applyPaletteColors(palette: Palette, cssVarPrefix: string) {
    for (const hue of Object.keys(palette.colors)) {
      const cssVar = `${cssVarPrefix}${hue}`;
      const color = palette.colors[hue];
      this.renderer.setStyle(this.ROOT, cssVar, color, RendererStyleFlags2.DashCase);
      const contrastCssVar = `${cssVarPrefix}contrast-${hue}`;
      const contrastColor = getContrastColor(palette.extends, hue);
      this.renderer.setStyle(this.ROOT, contrastCssVar, contrastColor, RendererStyleFlags2.DashCase);
    }
  }

  private updateImages(wlParams: WhiteLabelingParams, prefix: string) {
    const storedLogoImageChecksum = localStorage.getItem(prefix + '_logo_image_checksum');
    const storedFaviconChecksum = localStorage.getItem(prefix + '_favicon_checksum');
    const logoImageChecksum = wlParams.logoImageChecksum;
    if (logoImageChecksum && !isEqual(storedLogoImageChecksum, logoImageChecksum)) {
      const logoImageUrl = wlParams.logoImageUrl;
      localStorage.setItem(prefix + '_logo_image_checksum', logoImageChecksum);
      localStorage.setItem(prefix + '_logo_image_url', logoImageUrl);
    } else {
      wlParams.logoImageUrl = localStorage.getItem(prefix + '_logo_image_url');
    }
    wlParams.logoImageSafeUrl = this.sanitizer.bypassSecurityTrustUrl(wlParams.logoImageUrl);
    const faviconChecksum = wlParams.faviconChecksum;
    if (faviconChecksum && !isEqual(storedFaviconChecksum, faviconChecksum)) {
      const favicon = wlParams.favicon;
      localStorage.setItem(prefix + '_favicon_checksum', faviconChecksum);
      localStorage.setItem(prefix + '_favicon_url', favicon.url);
      localStorage.setItem(prefix + '_favicon_type', favicon.type);
    } else {
      wlParams.favicon = {
        url: localStorage.getItem(prefix + '_favicon_url'),
        type: localStorage.getItem(prefix + '_favicon_type'),
      };
    }
  }

  private asWhiteLabelingObservable<T>(valueSource: () => T): Observable<T> {
    return this.changeWhiteLabelingSubject.pipe(
      map(() => valueSource())
    );
  }

}

function applyCustomCss(customCss: string, isLoginTheme: boolean) {
  const target = isLoginTheme ? 'tb-login-custom-css' : 'tb-app-custom-css';
  let targetStyle = $(`#${target}`);
  if (!targetStyle.length) {
    targetStyle = $(`<style id="${target}"></style>`);
    $('head').append(targetStyle);
  }
  let css;
  if (customCss && customCss.length) {
    cssParser.cssPreviewNamespace = isLoginTheme ? 'tb-custom-css' : 'tb-default';
    css = cssParser.applyNamespacing(customCss);
    if (typeof css !== 'string') {
      css = cssParser.getCSSForEditor(css);
    }
  } else {
    css = '';
  }
  targetStyle.text(css);
}
