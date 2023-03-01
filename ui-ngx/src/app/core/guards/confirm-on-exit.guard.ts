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

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanDeactivate, RouterStateSnapshot } from '@angular/router';
import { UntypedFormGroup } from '@angular/forms';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthState } from '@core/auth/auth.models';
import { selectAuth } from '@core/auth/auth.selectors';
import { map, mergeMap, take } from 'rxjs/operators';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { isDefined } from '../utils';
import { Observable, of } from 'rxjs';

export interface HasConfirmForm {
  confirmForm(): UntypedFormGroup;
  onExit?(): Observable<any>;
}

export interface HasDirtyFlag {
  isDirty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ConfirmOnExitGuard implements CanDeactivate<HasConfirmForm & HasDirtyFlag> {

  constructor(private store: Store<AppState>,
              private dialogService: DialogService,
              private translate: TranslateService) { }

  canDeactivate(component: HasConfirmForm & HasDirtyFlag,
                route: ActivatedRouteSnapshot,
                state: RouterStateSnapshot) {


    let auth: AuthState = null;
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        auth = authState;
      }
    );

    if (auth && auth.isAuthenticated) {
      let isDirty = false;
      if (component.confirmForm) {
        const confirmForm = component.confirmForm();
        if (confirmForm) {
          isDirty = confirmForm.dirty;
        }
      } else if (isDefined(component.isDirty)) {
        isDirty = component.isDirty;
      }
      if (isDirty) {
        return this.dialogService.confirm(
          this.translate.instant('confirm-on-exit.title'),
          this.translate.instant('confirm-on-exit.html-message')
        ).pipe(
          mergeMap(result => {
            if (result && component.onExit) {
              return component.onExit().pipe(map(() => result));
            } else {
              return of(result);
            }
          }),
          map((dialogResult) => {
            if (dialogResult) {
              if (component.confirmForm && component.confirmForm()) {
                component.confirmForm().markAsPristine();
              } else {
                component.isDirty = false;
              }
            }
            return dialogResult;
          })
        );
      }
    }
    if (component.onExit) {
      return component.onExit().pipe(map(() => true));
    }
    return true;
  }
}
