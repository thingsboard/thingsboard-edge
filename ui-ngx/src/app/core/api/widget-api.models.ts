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

import { Observable } from 'rxjs';
import { EntityId } from '@app/shared/models/id/entity-id';
import {
  DataSet,
  Datasource,
  DatasourceData,
  DatasourceType,
  KeyInfo,
  LegendConfig,
  LegendData,
  WidgetActionDescriptor,
  widgetType
} from '@shared/models/widget.models';
import { TimeService } from '../services/time.service';
import { DeviceService } from '../http/device.service';
import { UtilsService } from '@core/services/utils.service';
import { Timewindow, WidgetTimewindow } from '@shared/models/time/time.models';
import { EntityType } from '@shared/models/entity-type.models';
import { HttpErrorResponse } from '@angular/common/http';
import { RafService } from '@core/services/raf.service';
import { EntityAliases } from '@shared/models/alias.models';
import { EntityInfo } from '@app/shared/models/entity.models';
import { IDashboardComponent } from '@home/models/dashboard-component.models';
import * as moment_ from 'moment';
import { DatePipe } from '@angular/common';
import {
  AlarmData,
  AlarmDataPageLink,
  EntityData,
  EntityDataPageLink,
  EntityFilter,
  Filter,
  FilterInfo,
  Filters,
  KeyFilter
} from '@shared/models/query/query.models';
import { EntityDataService } from '@core/api/entity-data.service';
import { PageData } from '@shared/models/page/page-data';
import { TranslateService } from '@ngx-translate/core';
import { AlarmDataService } from '@core/api/alarm-data.service';
import { IDashboardController } from '@home/components/dashboard-page/dashboard-page.models';
import { PopoverPlacement } from '@shared/components/popover.models';
import { PersistentRpc } from '@shared/models/rpc.models';
import { EventEmitter } from '@angular/core';

export interface TimewindowFunctions {
  onUpdateTimewindow: (startTimeMs: number, endTimeMs: number, interval?: number) => void;
  onResetTimewindow: () => void;
}

export interface WidgetSubscriptionApi {
  createSubscription: (options: WidgetSubscriptionOptions, subscribe?: boolean) => Observable<IWidgetSubscription>;
  createSubscriptionFromInfo: (type: widgetType, subscriptionsInfo: Array<SubscriptionInfo>,
                               options: WidgetSubscriptionOptions, useDefaultComponents: boolean, subscribe: boolean)
    => Observable<IWidgetSubscription>;
  removeSubscription: (id: string) => void;
}

export interface RpcApi {
  sendOneWayCommand: (method: string, params?: any, timeout?: number, persistent?: boolean,
                      persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string) => Observable<any>;
  sendTwoWayCommand: (method: string, params?: any, timeout?: number, persistent?: boolean,
                      persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string) => Observable<any>;
  completedCommand: () => void;
}

export interface IWidgetUtils {
  formatValue: (value: any, dec?: number, units?: string, showZeroDecimals?: boolean) => string | undefined;
}

export interface WidgetActionsApi {
  actionDescriptorsBySourceId: {[sourceId: string]: Array<WidgetActionDescriptor>};
  getActionDescriptors: (actionSourceId: string) => Array<WidgetActionDescriptor>;
  handleWidgetAction: ($event: Event, descriptor: WidgetActionDescriptor,
                       entityId?: EntityId, entityName?: string, additionalParams?: any, entityLabel?: string) => void;
  elementClick: ($event: Event) => void;
  getActiveEntityInfo: () => SubscriptionEntityInfo;
  openDashboardStateInSeparateDialog: (targetDashboardStateId: string, params?: StateParams, dialogTitle?: string,
                                       hideDashboardToolbar?: boolean, dialogWidth?: number, dialogHeight?: number) => void;
  openDashboardStateInPopover: ($event: Event, targetDashboardStateId: string, params?: StateParams,
                                hideDashboardToolbar?: boolean, preferredPlacement?: PopoverPlacement,
                                hideOnClickOutside?: boolean, popoverWidth?: string,
                                popoverHeight?: string, popoverStyle?: { [klass: string]: any }) => void;
}

export interface AliasInfo {
  alias?: string;
  stateEntity?: boolean;
  entityFilter?: EntityFilter;
  currentEntity?: EntityInfo;
  entityParamName?: string;
  resolveMultiple?: boolean;
}

export interface StateEntityInfo {
  entityParamName: string;
  entityId: EntityId;
}

export interface IAliasController {
  entityAliasesChanged: Observable<Array<string>>;
  entityAliasResolved: Observable<string>;
  filtersChanged: Observable<Array<string>>;
  getAliasInfo(aliasId: string): Observable<AliasInfo>;
  getEntityAliasId(aliasName: string): string;
  getInstantAliasInfo(aliasId: string): AliasInfo;
  resolveSingleEntityInfo(aliasId: string): Observable<EntityInfo>;
  resolveDatasources(datasources: Array<Datasource>, singleEntity?: boolean, pageSize?: number): Observable<Array<Datasource>>;
  resolveAlarmSource(alarmSource: Datasource): Observable<Datasource>;
  getEntityAliases(): EntityAliases;
  getFilters(): Filters;
  getFilterInfo(filterId: string): FilterInfo;
  getKeyFilters(filterId: string): Array<KeyFilter>;
  updateCurrentAliasEntity(aliasId: string, currentEntity: EntityInfo);
  updateUserFilter(filter: Filter);
  updateEntityAliases(entityAliases: EntityAliases);
  updateFilters(filters: Filters);
  updateAliases(aliasIds?: Array<string>);
  dashboardStateChanged();
}

export interface StateObject {
  id?: string;
  params?: StateParams;
}

export interface StateParams {
  entityName?: string;
  entityLabel?: string;
  targetEntityParamName?: string;
  entityId?: EntityId;
  entityGroupType?: EntityType;
  [key: string]: any | null;
}

export type StateControllerHolder = () => IStateController;

export interface IStateController {
  dashboardCtrl: IDashboardController;
  getStateParams(): StateParams;
  stateChanged(): Observable<string>;
  getStateParamsByStateId(stateId: string): StateParams;
  openState(id: string, params?: StateParams, openRightLayout?: boolean): void;
  updateState(id?: string, params?: StateParams, openRightLayout?: boolean): void;
  resetState(): void;
  openRightLayout(): void;
  preserveState(): void;
  cleanupPreservedStates(): void;
  navigatePrevState(index: number): void;
  getStateId(): string;
  getStateIndex(): number;
  getStateIdAtIndex(index: number): string;
  getEntityId(entityParamName: string): EntityId;
  getCurrentStateName(): string;
}

export interface SubscriptionInfo {
  type: DatasourceType;
  name?: string;
  entityType?: EntityType;
  entityId?: string;
  entityIds?: Array<string>;
  entityName?: string;
  entityNamePrefix?: string;
  timeseries?: Array<KeyInfo>;
  attributes?: Array<KeyInfo>;
  functions?: Array<KeyInfo>;
  alarmFields?: Array<KeyInfo>;

  deviceId?: string;
  deviceName?: string;
  deviceNamePrefix?: string;
  deviceIds?: Array<string>;
  pageSize?: number;
}

export class WidgetSubscriptionContext {

  constructor(private dashboard: IDashboardComponent) {}

  get aliasController(): IAliasController {
    return this.dashboard.aliasController;
  }

  dashboardTimewindowApi: TimewindowFunctions = {
    onResetTimewindow: this.dashboard.onResetTimewindow.bind(this.dashboard),
    onUpdateTimewindow: this.dashboard.onUpdateTimewindow.bind(this.dashboard)
  };

  timeService: TimeService;
  deviceService: DeviceService;
  translate: TranslateService;
  entityDataService: EntityDataService;
  alarmDataService: AlarmDataService;
  utils: UtilsService;
  datePipe: DatePipe;
  raf: RafService;
  widgetUtils: IWidgetUtils;
  getServerTimeDiff: () => Observable<number>;
}

export type SubscriptionMessageSeverity = 'info' | 'warn' | 'error' | 'success';

export interface SubscriptionMessage {
  severity: SubscriptionMessageSeverity;
  message: string;
}

export interface WidgetSubscriptionCallbacks {
  onDataUpdated?: (subscription: IWidgetSubscription, detectChanges: boolean) => void;
  onLatestDataUpdated?: (subscription: IWidgetSubscription, detectChanges: boolean) => void;
  onDataUpdateError?: (subscription: IWidgetSubscription, e: any) => void;
  onLatestDataUpdateError?: (subscription: IWidgetSubscription, e: any) => void;
  onSubscriptionMessage?: (subscription: IWidgetSubscription, message: SubscriptionMessage) => void;
  onInitialPageDataChanged?: (subscription: IWidgetSubscription, nextPageData: PageData<EntityData>) => void;
  forceReInit?: () => void;
  dataLoading?: (subscription: IWidgetSubscription) => void;
  legendDataUpdated?: (subscription: IWidgetSubscription, detectChanges: boolean) => void;
  timeWindowUpdated?: (subscription: IWidgetSubscription, timeWindowConfig: Timewindow) => void;
  rpcStateChanged?: (subscription: IWidgetSubscription) => void;
  onRpcSuccess?: (subscription: IWidgetSubscription) => void;
  onRpcFailed?: (subscription: IWidgetSubscription) => void;
  onRpcErrorCleared?: (subscription: IWidgetSubscription) => void;
}

export interface WidgetSubscriptionOptions {
  type?: widgetType;
  stateData?: boolean;
  alarmSource?: Datasource;
  datasources?: Array<Datasource>;
  datasourcesOptional?: boolean;
  hasDataPageLink?: boolean;
  singleEntity?: boolean;
  pageSize?: number;
  warnOnPageDataOverflow?: boolean;
  ignoreDataUpdateOnIntervalTick?: boolean;
  targetDeviceAliasIds?: Array<string>;
  targetDeviceIds?: Array<string>;
  useDashboardTimewindow?: boolean;
  displayTimewindow?: boolean;
  timeWindowConfig?: Timewindow;
  dashboardTimewindow?: Timewindow;
  onTimewindowChangeFunction?: (timewindow: Timewindow) => Timewindow;
  legendConfig?: LegendConfig;
  comparisonEnabled?: boolean;
  timeForComparison?: moment_.unitOfTime.DurationConstructor;
  comparisonCustomIntervalValue?: number;
  decimals?: number;
  units?: string;
  callbacks?: WidgetSubscriptionCallbacks;
}

export interface SubscriptionEntityInfo {
  entityId: EntityId;
  entityName: string;
  entityLabel: string;
  entityDescription: string;
}

export interface IWidgetSubscription {

  options: WidgetSubscriptionOptions;
  id: string;
  init$: Observable<IWidgetSubscription>;
  ctx: WidgetSubscriptionContext;
  type: widgetType;
  callbacks: WidgetSubscriptionCallbacks;

  loadingData: boolean;
  useDashboardTimewindow: boolean;

  legendData: LegendData;

  datasourcePages?: PageData<Datasource>[];
  dataPages?: PageData<Array<DatasourceData>>[];
  datasources?: Array<Datasource>;
  data?: Array<DatasourceData>;
  latestData?: Array<DatasourceData>;
  hiddenData?: Array<{data: DataSet}>;
  timeWindowConfig?: Timewindow;
  timeWindow?: WidgetTimewindow;
  onTimewindowChangeFunction?: (timewindow: Timewindow) => Timewindow;
  widgetTimewindowChanged$: Observable<WidgetTimewindow>;
  comparisonEnabled?: boolean;
  comparisonTimeWindow?: WidgetTimewindow;

  persistentRequests?: PageData<PersistentRpc>;

  alarms?: PageData<AlarmData>;
  alarmSource?: Datasource;

  targetDeviceAliasIds?: Array<string>;
  targetDeviceIds?: Array<string>;

  rpcEnabled?: boolean;
  executingRpcRequest?: boolean;
  rpcErrorText?: string;
  rpcRejection?: HttpErrorResponse;

  getFirstEntityInfo(): SubscriptionEntityInfo;

  onAliasesChanged(aliasIds: Array<string>): boolean;

  onFiltersChanged(filterIds: Array<string>): boolean;

  onDashboardTimewindowChanged(dashboardTimewindow: Timewindow): void;

  updateDataVisibility(index: number): void;

  onUpdateTimewindow(startTimeMs: number, endTimeMs: number, interval?: number): void;
  onResetTimewindow(): void;
  updateTimewindowConfig(newTimewindow: Timewindow): void;

  sendOneWayCommand(method: string, params?: any, timeout?: number, persistent?: boolean,
                    persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string): Observable<any>;
  sendTwoWayCommand(method: string, params?: any, timeout?: number, persistent?: boolean,
                    persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string): Observable<any>;
  clearRpcError(): void;

  subscribe(): void;

  subscribeAllForPaginatedData(pageLink: EntityDataPageLink,
                               keyFilters: KeyFilter[]): void;

  subscribeForPaginatedData(datasourceIndex: number,
                            pageLink: EntityDataPageLink,
                            keyFilters: KeyFilter[]): Observable<any>;

  paginatedDataSubscriptionUpdated: EventEmitter<void>;

  subscribeForAlarms(pageLink: AlarmDataPageLink,
                     keyFilters: KeyFilter[]): void;

  isDataResolved(): boolean;

  exportData(): {[key: string]: any}[];

  destroy(): void;

  update(): void;

  [key: string]: any;
}
