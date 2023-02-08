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

import { Directive, OnDestroy } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable, Subscription } from 'rxjs';
import { selectIsLoading } from '@core/interceptors/load.selectors';
import { delay, share } from 'rxjs/operators';
import { AbstractControl } from '@angular/forms';
import { Operation, Resource } from '@shared/models/security.models';

@Directive()
export abstract class PageComponent implements OnDestroy {

  isLoading$: Observable<boolean>;
  loadingSubscription: Subscription;
  disabledOnLoadFormControls: Array<AbstractControl> = [];

  resource = Resource;
  operation = Operation;

  protected constructor(protected store: Store<AppState>) {
    this.isLoading$ = this.store.pipe(delay(0), select(selectIsLoading), share());
  }

  protected registerDisableOnLoadFormControl(control: AbstractControl) {
    this.disabledOnLoadFormControls.push(control);
    if (!this.loadingSubscription) {
      this.loadingSubscription = this.isLoading$.subscribe((isLoading) => {
        for (const formControl of this.disabledOnLoadFormControls) {
          if (isLoading) {
            formControl.disable({emitEvent: false});
          } else {
            formControl.enable({emitEvent: false});
          }
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.loadingSubscription) {
      this.loadingSubscription.unsubscribe();
    }
  }

}
