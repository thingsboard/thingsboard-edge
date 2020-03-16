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

import { Injectable, NgZone } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  RouterStateSnapshot
} from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { select, Store } from '@ngrx/store';
import { AppState } from '../core.state';
import { selectAuth } from '../auth/auth.selectors';
import { catchError, map, skipWhile, take } from 'rxjs/operators';
import { AuthState } from '../auth/auth.models';
import { Observable, of } from 'rxjs';
import { enterZone } from '@core/operator/enterZone';
import { Authority } from '@shared/models/authority.enum';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild {

  constructor(private store: Store<AppState>,
              private authService: AuthService,
              private dialogService: DialogService,
              private utils: UtilsService,
              private translate: TranslateService,
              private zone: NgZone) {}

  getAuthState(): Observable<AuthState> {
    return this.store.pipe(
      select(selectAuth),
      skipWhile((authState) => !authState || !authState.isUserLoaded),
      take(1),
      enterZone(this.zone)
    );
  }

  canActivate(next: ActivatedRouteSnapshot,
              state: RouterStateSnapshot) {

    return this.getAuthState().pipe(
      map((authState) => {
        const url: string = state.url;

        let lastChild = state.root;
        const urlSegments: string[] = [];
        if (lastChild.url) {
          urlSegments.push(...lastChild.url.map(segment => segment.path));
        }
        while (lastChild.children.length) {
          lastChild = lastChild.children[0];
          if (lastChild.url) {
            urlSegments.push(...lastChild.url.map(segment => segment.path));
          }
        }
        const path = urlSegments.join('.');
        const publicId = this.utils.getQueryParam('publicId');
        const data = lastChild.data || {};
        const params = lastChild.params || {};
        const isPublic = data.module === 'public';

        if (!authState.isAuthenticated) {
          if (publicId && publicId.length > 0) {
            this.authService.setUserFromJwtToken(null, null, false);
            this.authService.reloadUser();
            return false;
          } else if (!isPublic) {
            this.authService.redirectUrl = url;
            // this.authService.gotoDefaultPlace(false);
            return this.authService.defaultUrl(false);
          } else {
            return true;
          }
        } else {
          if (authState.authUser.isPublic) {
            if (this.authService.parsePublicId() !== publicId) {
              if (publicId && publicId.length > 0) {
                this.authService.setUserFromJwtToken(null, null, false);
                this.authService.reloadUser();
              } else {
                this.authService.logout();
              }
              return false;
            }
            if (!authState.lastPublicDashboardId) {
              this.dialogService.forbidden();
              return false;
            }
          }
          const defaultUrl = this.authService.defaultUrl(true, authState, path, params);
          if (defaultUrl) {
            // this.authService.gotoDefaultPlace(true);
            return defaultUrl;
          } else {
            const authority = Authority[authState.authUser.authority];
            if (data.auth && data.auth.indexOf(authority) === -1) {
              this.dialogService.forbidden();
              return false;
            } else {
              return true;
            }
          }
        }
      }),
      catchError((err => { console.error(err); return of(false); } ))
    );
  }

  canActivateChild(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot) {
    return this.canActivate(route, state);
  }
}
