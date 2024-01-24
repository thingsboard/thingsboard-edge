///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { BackgroundSettings } from '@shared/models/widget-settings.models';

export enum RpcInitialStateAction {
  DO_NOTHING = 'DO_NOTHING',
  EXECUTE_RPC = 'EXECUTE_RPC',
  GET_ATTRIBUTE = 'GET_ATTRIBUTE',
  GET_TIME_SERIES = 'GET_TIME_SERIES'
}

export const rpcInitialStateActions = Object.keys(RpcInitialStateAction) as RpcInitialStateAction[];

export const rpcInitialStateTranslations = new Map<RpcInitialStateAction, string>(
  [
    [RpcInitialStateAction.DO_NOTHING, 'widgets.rpc-state.do-nothing'],
    [RpcInitialStateAction.EXECUTE_RPC, 'widgets.rpc-state.execute-rpc'],
    [RpcInitialStateAction.GET_ATTRIBUTE, 'widgets.rpc-state.get-attribute'],
    [RpcInitialStateAction.GET_TIME_SERIES, 'widgets.rpc-state.get-time-series']
  ]
);

export interface RpcSettings {
  method: string;
  requestTimeout: number;
  requestPersistent: boolean;
  persistentPollingInterval: number;
}

export interface RpcTelemetrySettings {
  key: string;
}

export interface RpcGetAttributeSettings extends RpcTelemetrySettings {
  scope: AttributeScope | null;
}

export interface RpcSetAttributeSettings extends RpcTelemetrySettings {
  scope: AttributeScope.SERVER_SCOPE | AttributeScope.SHARED_SCOPE;
}

export enum RpcDataToStateType {
  NONE = 'NONE',
  FUNCTION = 'FUNCTION'
}

export interface RpcDataToStateSettings {
  type: RpcDataToStateType;
  dataToStateFunction: string;
  compareToValue?: any;
}

export interface RpcActionSettings {
  actionLabel?: string;
}

export interface RpcInitialStateSettings<V> extends RpcActionSettings {
  action: RpcInitialStateAction;
  defaultValue: V;
  executeRpc: RpcSettings;
  getAttribute: RpcGetAttributeSettings;
  getTimeSeries: RpcTelemetrySettings;
  dataToState: RpcDataToStateSettings;
}

export enum RpcUpdateStateAction {
  EXECUTE_RPC = 'EXECUTE_RPC',
  SET_ATTRIBUTE = 'SET_ATTRIBUTE',
  ADD_TIME_SERIES = 'ADD_TIME_SERIES'
}

export const rpcUpdateStateActions = Object.keys(RpcUpdateStateAction) as RpcUpdateStateAction[];

export const rpcUpdateStateTranslations = new Map<RpcUpdateStateAction, string>(
  [
    [RpcUpdateStateAction.EXECUTE_RPC, 'widgets.rpc-state.execute-rpc'],
    [RpcUpdateStateAction.SET_ATTRIBUTE, 'widgets.rpc-state.set-attribute'],
    [RpcUpdateStateAction.ADD_TIME_SERIES, 'widgets.rpc-state.add-time-series']
  ]
);

export enum RpcStateToParamsType {
  CONSTANT = 'CONSTANT',
  FUNCTION = 'FUNCTION',
  NONE = 'NONE'
}

export interface RpcStateToParamsSettings {
  type: RpcStateToParamsType;
  constantValue: any;
  stateToParamsFunction: string;
}

export interface RpcUpdateStateSettings extends RpcActionSettings {
  action: RpcUpdateStateAction;
  executeRpc: RpcSettings;
  setAttribute: RpcSetAttributeSettings;
  putTimeSeries: RpcTelemetrySettings;
  stateToParams: RpcStateToParamsSettings;
}

export interface RpcStateBehaviourSettings<V> {
  initialState: RpcInitialStateSettings<V>;
  updateStateByValue: (value: V) => RpcUpdateStateSettings;
}

export interface RpcStateWidgetSettings<V> {
  initialState: RpcInitialStateSettings<V>;
  background: BackgroundSettings;
}
