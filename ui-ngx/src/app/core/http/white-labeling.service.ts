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
  LoginWhiteLabelingParams, mergeDefaults, PaletteSettings,
  tbAccentPalette, tbDarkPrimaryBackgroundPalette, tbDarkPrimaryPalette,
  tbPrimaryPalette,
  WhiteLabelingParams
} from '@shared/models/white-labeling.models';
import { BehaviorSubject, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { ColorPalette, extendPalette, materialColorPalette } from '@shared/models/material.models';
import { deepClone, isEqual } from '@core/utils';
import { catchError, map, mergeMap, share, tap } from 'rxjs/operators';
import { throwError } from 'rxjs/internal/observable/throwError';
import { environment as env } from '@env/environment';
import {
  ActionSettingsChangeLanguage,
  ActionSettingsChangeWhiteLabeling,
  SettingsActions, SettingsActionTypes
} from '@core/settings/settings.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Actions, ofType } from '@ngrx/effects';

@Injectable({
  providedIn: 'root'
})
export class WhiteLabelingService {

  private changeWhiteLabelingSubject = new ReplaySubject(1);

  private currentLoginTheme: string;
  private loginLogo: string;
  private loginLogoHeight: number;
  private loginPageBackgroundColor: string;
  private loginShowNameVersion: boolean;
  private showNameBottom: boolean;
  private platformName: string;
  private platformVersion: string;

  private currentTheme: string;

  public currentLoginTheme$ = this.asWhiteLabelingObservable(() => this.currentLoginTheme);
  public currentTheme$ = this.asWhiteLabelingObservable(() => this.currentTheme);

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

  private primaryPaletteName: string;
  private accentPaletteName: string;

  private PALETTES: {[palette: string]: ColorPalette} = deepClone(materialColorPalette);

  constructor(
    private http: HttpClient,
    private store: Store<AppState>
  ) {
    this.definePalette('tb-primary', tbPrimaryPalette);
    this.definePalette('tb-accent', tbAccentPalette);
    this.definePalette('tb-dark-primary', tbDarkPrimaryPalette)
    this.definePalette('tb-dark-primary-background', tbDarkPrimaryBackgroundPalette);
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
    this.isUserWlMode = false;
    return this.http.get<LoginWhiteLabelingParams>(url).pipe(
      map((loginWlParams) => {
        this.loginWlParams = mergeDefaults(loginWlParams, defaultLoginWlParams);
        this.updateImages(this.loginWlParams, 'login');
        if (this.setLoginWlParams(this.loginWlParams)) {
          this.applyLoginWlParams(this.currentLoginWLParams);
          this.applyLoginThemePalettes(this.currentLoginWLParams.paletteSettings, this.currentLoginWLParams.darkForeground);
          this.notifyWlChanged();
        }
        return this.loginWlParams;
      }),
      catchError((err) => {
        if (this.loginWlParams) {
          if (this.setLoginWlParams(this.loginWlParams)) {
            this.applyLoginWlParams(this.currentLoginWLParams);
            this.applyLoginThemePalettes(this.currentLoginWLParams.paletteSettings, this.currentLoginWLParams.darkForeground);
            this.notifyWlChanged();
          }
          return of(this.loginWlParams);
        } else {
          return throwError(err);
        }
      })
    );
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
      map((userWlParams) => {
        this.isUserWlMode = true;
        this.userWlParams = mergeDefaults(userWlParams);
        this.updateImages(this.userWlParams, 'user');
        if (this.setWlParams(userWlParams)) {
          this.wlChanged();
        }
        return this.userWlParams;
      }),
      catchError((err) => {
        this.isUserWlMode = true;
        if (this.userWlParams) {
          if (this.setWlParams(this.userWlParams)) {
            this.wlChanged();
          }
          return of(this.userWlParams);
        } else {
          return throwError(err);
        }
      })
    );
  }

  public whiteLabelPreview(wLParams: WhiteLabelingParams): Observable<WhiteLabelingParams> {
    return this.http.post<WhiteLabelingParams>('/api/whiteLabel/previewWhiteLabelParams', wLParams).pipe(
      tap((previewWlParams) => {
        this.currentWLParams = mergeDefaults(previewWlParams);
        this.wlChanged();
      })
    );
  }

  public cancelWhiteLabelPreview() {
    this.currentWLParams = this.userWlParams;
    this.wlChanged();
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

  private wlChanged() {
    this.applyThemePalettes(this.currentWLParams.paletteSettings);
    this.notifyWlChanged();
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

  private applyThemePalettes(paletteSettings: PaletteSettings) {
    this.cleanupPalettes('custom-primary');
    this.cleanupPalettes('custom-accent');

    const primaryPalette = paletteSettings.primaryPalette;
    const accentPalette = paletteSettings.accentPalette;

    if (primaryPalette.type === 'tb-primary' &&
      accentPalette.type === 'tb-accent') {
      this.primaryPaletteName = primaryPalette.type;
      this.accentPaletteName = accentPalette.type;
      this.currentTheme = 'tb-default';
      return;
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

    this.cleanupThemes('tbCustomTheme');

    const themeName = 'tbCustomTheme' + (Math.random()*1000).toFixed(0);

    // TODO: CSS styling

    this.currentTheme = themeName;
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

  private applyLoginThemePalettes(paletteSettings: PaletteSettings, darkForeground: boolean) {
    const primaryPalette = paletteSettings.primaryPalette;
    const accentPalette = paletteSettings.accentPalette;

    if (primaryPalette.type === 'tb-primary' &&
      accentPalette.type === 'tb-accent') {
      this.currentLoginTheme = 'tb-dark';
      return;
    }

    this.cleanupPalettes('custom-login-');
    const primaryExtends = primaryPalette.type !== 'custom' ? primaryPalette.type : primaryPalette.extends;
    const primaryColors = primaryPalette.colors ? deepClone(primaryPalette.colors) : {};
    const primaryBackgroundContrastColor = primaryColors['200'] ? primaryColors['200'] : this.PALETTES[primaryExtends]['200'];
    const primaryBackgroundColor = primaryColors['500'] ? primaryColors['500'] : this.PALETTES[primaryExtends]['500'];
    primaryColors['500'] = primaryBackgroundContrastColor;
    const primaryPaletteName = 'custom-login-primary';
    const customLoginPrimaryPalette = extendPalette(this.PALETTES, primaryExtends, primaryColors);
    this.definePalette(primaryPaletteName, customLoginPrimaryPalette);
    let accentPaletteName: string;
    if (accentPalette.type !== 'custom') {
      accentPaletteName = accentPalette.type;
    } else {
      accentPaletteName = 'custom-login-accent';
      const customLoginAccentPalette = extendPalette(this.PALETTES, accentPalette.extends, accentPalette.colors);
      this.definePalette(accentPaletteName, customLoginAccentPalette);
    }
    const backgroundPaletteName = 'custom-login-background';
    const backgroundPaletteColors: ColorPalette = {
      800:  primaryBackgroundColor
    };
    const customLoginBackgroundPalette = extendPalette(this.PALETTES, primaryPaletteName,
      backgroundPaletteColors);
    this.definePalette(backgroundPaletteName, customLoginBackgroundPalette);
    this.cleanupThemes('tbCustomLoginTheme');

    const themeName = 'tbCustomLoginTheme' + (Math.random()*1000).toFixed(0);

    // TODO: CSS styling

    this.currentLoginTheme = themeName;
  }

  private cleanupPalettes(prefix: string) {
    for (const palette in this.PALETTES) {
      if (palette.startsWith(prefix)) {
        delete this.PALETTES[palette];
      }
    }
  }

  private cleanupThemes(prefix) {
    const styleElements = $('style');
    styleElements.each((index, styleElement) => {
      // TODO: CSS styling
      /*if (styleElement.hasAttribute('md-theme-style')) {
        const content = styleElement.innerHTML || styleElement.innerText || styleElement.textContent;
        if( content.indexOf(prefix) >= 0){
          styleElement.parentNode.removeChild(styleElement);
        }
      }*/
    });
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
