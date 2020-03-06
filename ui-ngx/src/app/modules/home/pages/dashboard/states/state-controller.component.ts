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

import { IStateControllerComponent, StateControllerState } from '@home/pages/dashboard/states/state-controller.models';
import { IDashboardController } from '../dashboard-page.models';
import { DashboardState } from '@app/shared/models/dashboard.models';
import { Subscription } from 'rxjs';
import { NgZone, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { StatesControllerService } from '@home/pages/dashboard/states/states-controller.service';
import { EntityId } from '@app/shared/models/id/entity-id';
import { StateParams } from '@app/core/api/widget-api.models';

export abstract class StateControllerComponent implements IStateControllerComponent, OnInit, OnDestroy {

  stateObject: StateControllerState = [];
  dashboardCtrl: IDashboardController;
  preservedState: any;

  isMobileValue: boolean;
  set isMobile(val: boolean) {
    if (this.isMobileValue !== val) {
      this.isMobileValue = val;
      if (this.inited) {
        this.onMobileChanged();
      }
    }
  }
  get isMobile(): boolean {
    return this.isMobileValue;
  }

  stateValue: string;
  set state(val: string) {
    if (this.stateValue !== val) {
      this.stateValue = val;
      if (this.inited) {
        this.onStateIdChanged();
      }
    }
  }
  get state(): string {
    return this.stateValue;
  }

  dashboardIdValue: string;
  set dashboardId(val: string) {
    if (this.dashboardIdValue !== val) {
      this.dashboardIdValue = val;
/*      if (this.inited) {
        this.currentState = this.route.snapshot.queryParamMap.get('state');
        this.init();
      }*/
    }
  }
  get dashboardId(): string {
    return this.dashboardIdValue;
  }

  statesValue: { [id: string]: DashboardState };
  set states(val: { [id: string]: DashboardState }) {
    if (this.statesValue !== val) {
      this.statesValue = val;
      if (this.inited) {
        this.onStatesChanged();
      }
    }
  }
  get states(): { [id: string]: DashboardState } {
    return this.statesValue;
  }

  currentState: string;

  private rxSubscriptions = new Array<Subscription>();

  private inited = false;

  protected constructor(protected router: Router,
                        protected route: ActivatedRoute,
                        protected ngZone: NgZone,
                        protected statesControllerService: StatesControllerService) {
  }

  ngOnInit(): void {
    this.rxSubscriptions.push(this.route.queryParamMap.subscribe((paramMap) => {
      const dashboardId = this.route.snapshot.params.dashboardId;
      if (this.dashboardId === dashboardId) {
        const newState = paramMap.get('state');
        if (this.currentState !== newState) {
          this.currentState = newState;
          if (this.inited) {
            this.onStateChanged();
          }
        }
      }
    }));
    this.init();
    this.inited = true;
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  protected updateStateParam(newState: string) {
    this.currentState = newState;
    const queryParams: Params = { state: this.currentState };
    this.ngZone.run(() => {
      this.router.navigate(
        [],
        {
          relativeTo: this.route,
          queryParams,
          queryParamsHandling: 'merge',
        });
    });
  }

  public openRightLayout(): void {
    this.dashboardCtrl.openRightLayout();
  }

  public preserveState() {
    this.statesControllerService.preserveStateControllerState(this.stateControllerId(), this.stateObject);
  }

  public cleanupPreservedStates() {
    this.statesControllerService.cleanupPreservedStates();
  }

  public reInit() {
    this.currentState = this.route.snapshot.queryParamMap.get('state');
    this.init();
  }

  protected abstract init();

  protected abstract onMobileChanged();

  protected abstract onStateIdChanged();

  protected abstract onStatesChanged();

  protected abstract onStateChanged();

  protected abstract stateControllerId(): string;

  public abstract getEntityId(entityParamName: string): EntityId;

  public abstract getStateId(): string;

  public abstract getStateIdAtIndex(index: number): string;

  public abstract getStateIndex(): number;

  public abstract getStateParams(): StateParams;

  public abstract getStateParamsByStateId(stateId: string): StateParams;

  public abstract navigatePrevState(index: number): void;

  public abstract openState(id: string, params?: StateParams, openRightLayout?: boolean): void;

  public abstract resetState(): void;

  public abstract updateState(id?: string, params?: StateParams, openRightLayout?: boolean): void;

}
