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

import {
  ComponentRef,
  Directive,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewContainerRef
} from '@angular/core';
import { DashboardState } from '@shared/models/dashboard.models';
import { IDashboardController } from '@home/components/dashboard-page/dashboard-page.models';
import { StatesControllerService } from '@home/components/dashboard-page/states/states-controller.service';
import { IStateControllerComponent } from '@home/components/dashboard-page/states/state-controller.models';
import { Subject } from 'rxjs';

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: 'tb-states-component'
})
export class StatesComponentDirective implements OnInit, OnDestroy, OnChanges {

  @Input()
  statesControllerId: string;

  @Input()
  dashboardCtrl: IDashboardController;

  @Input()
  dashboardId: string;

  @Input()
  states: {[id: string]: DashboardState };

  @Input()
  state: string;

  @Input()
  currentState: string;

  @Input()
  syncStateWithQueryParam: boolean;

  @Input()
  isMobile: boolean;

  stateControllerComponentRef: ComponentRef<IStateControllerComponent>;
  stateControllerComponent: IStateControllerComponent;

  private stateChangedSubject = new Subject<string>();

  constructor(private viewContainerRef: ViewContainerRef,
              private statesControllerService: StatesControllerService) {
  }

  ngOnInit(): void {
    this.init();
  }

  ngOnDestroy(): void {
    this.destroy();
    this.stateChangedSubject.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    let reInitController = false;
    let initController = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'statesControllerId') {
          this.reInit();
        } else if (propName === 'states') {
          this.stateControllerComponent.states = this.states;
        } else if (propName === 'dashboardId') {
          this.stateControllerComponent.dashboardId = this.dashboardId;
          reInitController = true;
        } else if (propName === 'isMobile') {
          this.stateControllerComponent.isMobile = this.isMobile;
        } else if (propName === 'state') {
          this.stateControllerComponent.state = this.state;
        } else if (propName === 'currentState') {
          this.stateControllerComponent.currentState = this.currentState;
          initController = true;
        } else if (propName === 'syncStateWithQueryParam') {
          this.stateControllerComponent.syncStateWithQueryParam = this.syncStateWithQueryParam;
        }
      }
    }
    if (initController) {
      this.stateControllerComponent.init();
    } else if (reInitController) {
      this.stateControllerComponent.reInit();
    }
  }

  private reInit() {
    this.destroy();
    this.init();
  }

  private init() {
    this.viewContainerRef.clear();
    let stateControllerData = this.statesControllerService.getStateController(this.statesControllerId);
    if (!stateControllerData) {
      stateControllerData = this.statesControllerService.getStateController('default');
    }
    const stateControllerInstanceId = this.dashboardCtrl.dashboardCtx.instanceId + '_' +  this.statesControllerId;
    const preservedState = this.statesControllerService.withdrawStateControllerState(stateControllerInstanceId);
    const stateControllerFactory = stateControllerData.factory;
    this.stateControllerComponentRef = this.viewContainerRef.createComponent(stateControllerFactory);
    this.stateControllerComponent = this.stateControllerComponentRef.instance;
    this.dashboardCtrl.dashboardCtx.stateController = this.stateControllerComponent;
    this.dashboardCtrl.dashboardCtx.stateChanged = this.stateChangedSubject.asObservable();
    this.stateControllerComponent.stateChanged().subscribe((state) => {
      this.stateChangedSubject.next(state);
    });
    this.stateControllerComponent.preservedState = preservedState;
    this.stateControllerComponent.dashboardCtrl = this.dashboardCtrl;
    this.stateControllerComponent.stateControllerInstanceId = stateControllerInstanceId;
    this.stateControllerComponent.state = this.state;
    this.stateControllerComponent.currentState = this.currentState;
    this.stateControllerComponent.syncStateWithQueryParam = this.syncStateWithQueryParam;
    this.stateControllerComponent.isMobile = this.isMobile;
    this.stateControllerComponent.states = this.states;
    this.stateControllerComponent.dashboardId = this.dashboardId;
  }

  private destroy() {
    if (this.stateControllerComponentRef) {
      this.stateControllerComponentRef.destroy();
    }
  }
}
