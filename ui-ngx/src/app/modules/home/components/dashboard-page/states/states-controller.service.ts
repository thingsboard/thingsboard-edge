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

import { ComponentFactory, ComponentFactoryResolver, Injectable, Type } from '@angular/core';
import { deepClone } from '@core/utils';
import { IStateControllerComponent } from '@home/components/dashboard-page/states/state-controller.models';

export interface StateControllerData {
  factory: ComponentFactory<IStateControllerComponent>;
}

@Injectable()
export class StatesControllerService {

  statesControllers: {[stateControllerId: string]: StateControllerData} = {};

  statesControllerStates: {[stateControllerInstanceId: string]: any} = {};

  constructor(private componentFactoryResolver: ComponentFactoryResolver) {
  }

  public registerStatesController(stateControllerId: string, stateControllerComponent: Type<IStateControllerComponent>): void {
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(stateControllerComponent);
    this.statesControllers[stateControllerId] = {
      factory: componentFactory
    };
  }

  public getStateControllers(): {[stateControllerId: string]: StateControllerData} {
    return this.statesControllers;
  }

  public getStateController(stateControllerId: string): StateControllerData {
    return this.statesControllers[stateControllerId];
  }

  public preserveStateControllerState(stateControllerInstanceId: string, state: any) {
    this.statesControllerStates[stateControllerInstanceId] = deepClone(state);
  }

  public withdrawStateControllerState(stateControllerInstanceId: string): any {
    const state = this.statesControllerStates[stateControllerInstanceId];
    delete this.statesControllerStates[stateControllerInstanceId];
    return state;
  }

  public cleanupPreservedStates() {
    this.statesControllerStates = {};
  }
}
