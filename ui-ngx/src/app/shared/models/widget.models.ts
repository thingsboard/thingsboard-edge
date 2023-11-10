///
/// Copyright © 2016-2023 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { AggregationType, ComparisonDuration, Timewindow } from '@shared/models/time/time.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from './telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import * as moment_ from 'moment';
import {
  AlarmFilter,
  AlarmFilterConfig,
  EntityDataPageLink,
  EntityFilter,
  KeyFilter
} from '@shared/models/query/query.models';
import { PopoverPlacement } from '@shared/components/popover.models';
import { PageComponent } from '@shared/components/page.component';
import { AfterViewInit, Directive, EventEmitter, Inject, OnInit, Type } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, UntypedFormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { Dashboard } from '@shared/models/dashboard.models';
import { IAliasController } from '@core/api/widget-api.models';
import { isNotEmptyStr } from '@core/utils';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { ComponentStyle, Font, TimewindowStyle } from '@shared/models/widget-settings.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';

export enum widgetType {
  timeseries = 'timeseries',
  latest = 'latest',
  rpc = 'rpc',
  alarm = 'alarm',
  static = 'static'
}

export interface WidgetTypeTemplate {
  fullFqn: string;
}

export interface WidgetTypeData {
  name: string;
  icon: string;
  configHelpLinkId: string;
  template: WidgetTypeTemplate;
}

export const widgetTypesData = new Map<widgetType, WidgetTypeData>(
  [
    [
      widgetType.timeseries,
      {
        name: 'widget.timeseries',
        icon: 'timeline',
        configHelpLinkId: 'widgetsConfigTimeseries',
        template: {
          fullFqn: 'system.charts.basic_timeseries'
        }
      }
    ],
    [
      widgetType.latest,
      {
        name: 'widget.latest',
        icon: 'track_changes',
        configHelpLinkId: 'widgetsConfigLatest',
        template: {
          fullFqn: 'system.cards.attributes_card'
        }
      }
    ],
    [
      widgetType.rpc,
      {
        name: 'widget.rpc',
        icon: 'mdi:developer-board',
        configHelpLinkId: 'widgetsConfigRpc',
        template: {
          fullFqn: 'system.gpio_widgets.basic_gpio_control'
        }
      }
    ],
    [
      widgetType.alarm,
      {
        name: 'widget.alarm',
        icon: 'error',
        configHelpLinkId: 'widgetsConfigAlarm',
        template: {
          fullFqn: 'system.alarm_widgets.alarms_table'
        }
      }
    ],
    [
      widgetType.static,
      {
        name: 'widget.static',
        icon: 'font_download',
        configHelpLinkId: 'widgetsConfigStatic',
        template: {
          fullFqn: 'system.cards.html_card'
        }
      }
    ]
  ]
);

export interface WidgetResource {
  url: string;
  isModule?: boolean;
}

export interface WidgetActionSource {
  name: string;
  value: string;
  multiple: boolean;
  hasShowCondition?: boolean;
}

export const widgetActionSources: {[acionSourceId: string]: WidgetActionSource} = {
    headerButton:
    {
      name: 'widget-action.header-button',
      value: 'headerButton',
      multiple: true,
      hasShowCondition: true
    }
};

export interface WidgetTypeDescriptor {
  type: widgetType;
  resources: Array<WidgetResource>;
  templateHtml: string;
  templateCss: string;
  controllerScript: string;
  settingsSchema?: string | any;
  dataKeySettingsSchema?: string | any;
  latestDataKeySettingsSchema?: string | any;
  settingsDirective?: string;
  dataKeySettingsDirective?: string;
  latestDataKeySettingsDirective?: string;
  hasBasicMode?: boolean;
  basicModeDirective?: string;
  defaultConfig: string;
  sizeX: number;
  sizeY: number;
}

export interface WidgetTypeParameters {
  useCustomDatasources?: boolean;
  maxDatasources?: number;
  maxDataKeys?: number;
  datasourcesOptional?: boolean;
  dataKeysOptional?: boolean;
  stateData?: boolean;
  hasDataPageLink?: boolean;
  singleEntity?: boolean;
  hasAdditionalLatestDataKeys?: boolean;
  warnOnPageDataOverflow?: boolean;
  ignoreDataUpdateOnIntervalTick?: boolean;
  processNoDataByWidget?: boolean;
  previewWidth?: string;
  previewHeight?: string;
  embedTitlePanel?: boolean;
  hideDataSettings?: boolean;
  defaultDataKeysFunction?: (configComponent: any, configData: any) => DataKey[];
  defaultLatestDataKeysFunction?: (configComponent: any, configData: any) => DataKey[];
}

export interface WidgetControllerDescriptor {
  widgetTypeFunction?: any;
  settingsSchema?: string | any;
  dataKeySettingsSchema?: string | any;
  latestDataKeySettingsSchema?: string | any;
  typeParameters?: WidgetTypeParameters;
  actionSources?: {[actionSourceId: string]: WidgetActionSource};
}

export interface BaseWidgetType extends BaseData<WidgetTypeId> {
  tenantId: TenantId;
  fqn: string;
  name: string;
  deprecated: boolean;
}

export const fullWidgetTypeFqn = (type: BaseWidgetType): string =>
  ((!type.tenantId || type.tenantId?.id === NULL_UUID) ? 'system' : 'tenant') + '.' + type.fqn;

export const widgetTypeFqn = (fullFqn: string): string => {
  if (isNotEmptyStr(fullFqn)) {
    const parts = fullFqn.split('.');
    if (parts.length > 1) {
      const scopeQualifier = parts[0];
      if (['system', 'tenant'].includes(scopeQualifier)) {
        return fullFqn.substring(scopeQualifier.length + 1);
      }
    }
  }
  return fullFqn;
};

export const isValidWidgetFullFqn = (fullFqn: string): boolean => {
  if (isNotEmptyStr(fullFqn)) {
    const parts = fullFqn.split('.');
    if (parts.length > 1) {
      const scopeQualifier = parts[0];
      return ['system', 'tenant'].includes(scopeQualifier);
    }
  }
  return false;
};

export interface WidgetType extends BaseWidgetType {
  descriptor: WidgetTypeDescriptor;
}

export interface WidgetTypeInfo extends BaseWidgetType {
  image: string;
  description: string;
  tags: string[];
  widgetType: widgetType;
}

export interface WidgetTypeDetails extends WidgetType, ExportableEntity<WidgetTypeId> {
  image: string;
  description: string;
  tags: string[];
}

export enum DeprecatedFilter {
  ALL = 'ALL',
  ACTUAL = 'ACTUAL',
  DEPRECATED = 'DEPRECATED'
}

export enum LegendDirection {
  column = 'column',
  row = 'row'
}

export const legendDirectionTranslationMap = new Map<LegendDirection, string>(
  [
    [ LegendDirection.column, 'direction.column' ],
    [ LegendDirection.row, 'direction.row' ]
  ]
);

export enum LegendPosition {
  top = 'top',
  bottom = 'bottom',
  left = 'left',
  right = 'right'
}

export const legendPositionTranslationMap = new Map<LegendPosition, string>(
  [
    [ LegendPosition.top, 'position.top' ],
    [ LegendPosition.bottom, 'position.bottom' ],
    [ LegendPosition.left, 'position.left' ],
    [ LegendPosition.right, 'position.right' ]
  ]
);

export interface LegendConfig {
  position: LegendPosition;
  direction?: LegendDirection;
  sortDataKeys: boolean;
  showMin: boolean;
  showMax: boolean;
  showAvg: boolean;
  showTotal: boolean;
  showLatest: boolean;
}

export const defaultLegendConfig = (wType: widgetType): LegendConfig => ({
  direction: LegendDirection.column,
  position: LegendPosition.bottom,
  sortDataKeys: false,
  showMin: false,
  showMax: false,
  showAvg: wType === widgetType.timeseries,
  showTotal: false,
  showLatest: false
});

export enum ComparisonResultType {
  PREVIOUS_VALUE = 'PREVIOUS_VALUE',
  DELTA_ABSOLUTE = 'DELTA_ABSOLUTE',
  DELTA_PERCENT = 'DELTA_PERCENT'
}

export const comparisonResultTypeTranslationMap = new Map<ComparisonResultType, string>(
  [
    [ComparisonResultType.PREVIOUS_VALUE, 'datakey.delta-calculation-result-previous-value'],
    [ComparisonResultType.DELTA_ABSOLUTE, 'datakey.delta-calculation-result-delta-absolute'],
    [ComparisonResultType.DELTA_PERCENT, 'datakey.delta-calculation-result-delta-percent']
  ]
);

export interface KeyInfo {
  name: string;
  aggregationType?: AggregationType;
  comparisonEnabled?: boolean;
  timeForComparison?: ComparisonDuration;
  comparisonCustomIntervalValue?: number;
  comparisonResultType?: ComparisonResultType;
  label?: string;
  color?: string;
  funcBody?: string;
  postFuncBody?: string;
  units?: string;
  decimals?: number;
}

export const dataKeyAggregationTypeHintTranslationMap = new Map<AggregationType, string>(
  [
    [AggregationType.MIN, 'datakey.aggregation-type-min-hint'],
    [AggregationType.MAX, 'datakey.aggregation-type-max-hint'],
    [AggregationType.AVG, 'datakey.aggregation-type-avg-hint'],
    [AggregationType.SUM, 'datakey.aggregation-type-sum-hint'],
    [AggregationType.COUNT, 'datakey.aggregation-type-count-hint'],
    [AggregationType.NONE, 'datakey.aggregation-type-none-hint'],
  ]
);


export interface DataKey extends KeyInfo {
  type: DataKeyType;
  pattern?: string;
  settings?: any;
  usePostProcessing?: boolean;
  hidden?: boolean;
  inLegend?: boolean;
  isAdditional?: boolean;
  origDataKeyIndex?: number;
  _hash?: number;
}

export enum DataKeyConfigMode {
  general = 'general',
  advanced = 'advanced'
}

export enum DatasourceType {
  function = 'function',
  device = 'device',
  entity = 'entity',
  entityCount = 'entityCount',
  alarmCount = 'alarmCount'
}

export const datasourceTypeTranslationMap = new Map<DatasourceType, string>(
  [
    [ DatasourceType.function, 'function.function' ],
    [ DatasourceType.device, 'device.device' ],
    [ DatasourceType.entity, 'entity.entity' ],
    [ DatasourceType.entityCount, 'entity.entities-count' ],
    [ DatasourceType.alarmCount, 'entity.alarms-count' ]
  ]
);

export interface Datasource {
  type?: DatasourceType | any;
  name?: string;
  aliasName?: string;
  dataKeys?: Array<DataKey>;
  latestDataKeys?: Array<DataKey>;
  entityType?: EntityType;
  entityId?: string;
  entityName?: string;
  deviceId?: string;
  entityAliasId?: string;
  filterId?: string;
  unresolvedStateEntity?: boolean;
  dataReceived?: boolean;
  entity?: BaseData<EntityId>;
  entityLabel?: string;
  entityDescription?: string;
  generated?: boolean;
  isAdditional?: boolean;
  origDatasourceIndex?: number;
  pageLink?: EntityDataPageLink;
  keyFilters?: Array<KeyFilter>;
  entityFilter?: EntityFilter;
  alarmFilterConfig?: AlarmFilterConfig;
  alarmFilter?: AlarmFilter;
  dataKeyStartIndex?: number;
  latestDataKeyStartIndex?: number;
  [key: string]: any;
}

export const datasourcesHasAggregation = (datasources?: Array<Datasource>): boolean => {
  if (datasources) {
    const foundDatasource = datasources.find(datasource => {
      const found = datasource.dataKeys && datasource.dataKeys.find(key => key?.type === DataKeyType.timeseries &&
        key?.aggregationType && key.aggregationType !== AggregationType.NONE);
      return !!found;
    });
    if (foundDatasource) {
      return true;
    }
  }
  return false;
};

export const datasourcesHasOnlyComparisonAggregation = (datasources?: Array<Datasource>): boolean => {
  if (!datasourcesHasAggregation(datasources)) {
    return false;
  }
  if (datasources) {
    const foundDatasource = datasources.find(datasource => {
      const found = datasource.dataKeys && datasource.dataKeys.find(key => key?.type === DataKeyType.timeseries &&
        key?.aggregationType && key.aggregationType !== AggregationType.NONE && !key.comparisonEnabled);
      return !!found;
    });
    if (foundDatasource) {
      return false;
    }
  }
  return true;
};

export interface FormattedData {
  $datasource: Datasource;
  entityName: string;
  deviceName: string;
  entityId: string;
  entityType: EntityType;
  entityLabel: string;
  entityDescription: string;
  aliasName: string;
  dsIndex: number;
  dsName: string;
  deviceType: string;
  [key: string]: any;
}

export interface ReplaceInfo {
  variable: string;
  valDec?: number;
  dataKeyName: string;
}

export type DataSet = [number, any][];

export interface DataSetHolder {
  data: DataSet;
}

export interface DatasourceData extends DataSetHolder {
  datasource: Datasource;
  dataKey: DataKey;
}

export interface LegendKey {
  dataKey: DataKey;
  dataIndex: number;
}

export interface LegendKeyData {
  min: string;
  max: string;
  avg: string;
  total: string;
  latest: string;
  hidden: boolean;
}

export interface LegendData {
  keys: Array<LegendKey>;
  data: Array<LegendKeyData>;
}

export enum WidgetActionType {
  openDashboardState = 'openDashboardState',
  updateDashboardState = 'updateDashboardState',
  openDashboard = 'openDashboard',
  custom = 'custom',
  customPretty = 'customPretty',
  mobileAction = 'mobileAction'
}

export enum WidgetMobileActionType {
  takePictureFromGallery = 'takePictureFromGallery',
  takePhoto = 'takePhoto',
  mapDirection = 'mapDirection',
  mapLocation = 'mapLocation',
  scanQrCode = 'scanQrCode',
  makePhoneCall = 'makePhoneCall',
  getLocation = 'getLocation',
  takeScreenshot = 'takeScreenshot'
}

export const widgetActionTypeTranslationMap = new Map<WidgetActionType, string>(
  [
    [ WidgetActionType.openDashboardState, 'widget-action.open-dashboard-state' ],
    [ WidgetActionType.updateDashboardState, 'widget-action.update-dashboard-state' ],
    [ WidgetActionType.openDashboard, 'widget-action.open-dashboard' ],
    [ WidgetActionType.custom, 'widget-action.custom' ],
    [ WidgetActionType.customPretty, 'widget-action.custom-pretty' ],
    [ WidgetActionType.mobileAction, 'widget-action.mobile-action' ]
  ]
);

export const widgetMobileActionTypeTranslationMap = new Map<WidgetMobileActionType, string>(
  [
    [ WidgetMobileActionType.takePictureFromGallery, 'widget-action.mobile.take-picture-from-gallery' ],
    [ WidgetMobileActionType.takePhoto, 'widget-action.mobile.take-photo' ],
    [ WidgetMobileActionType.mapDirection, 'widget-action.mobile.map-direction' ],
    [ WidgetMobileActionType.mapLocation, 'widget-action.mobile.map-location' ],
    [ WidgetMobileActionType.scanQrCode, 'widget-action.mobile.scan-qr-code' ],
    [ WidgetMobileActionType.makePhoneCall, 'widget-action.mobile.make-phone-call' ],
    [ WidgetMobileActionType.getLocation, 'widget-action.mobile.get-location' ],
    [ WidgetMobileActionType.takeScreenshot, 'widget-action.mobile.take-screenshot' ]
  ]
);

export interface MobileLaunchResult {
  launched: boolean;
}

export interface MobileImageResult {
  imageUrl: string;
}

export interface MobileQrCodeResult {
  code: string;
  format: string;
}

export interface MobileLocationResult {
  latitude: number;
  longitude: number;
}

export type MobileActionResult = MobileLaunchResult &
                                 MobileImageResult &
                                 MobileQrCodeResult &
                                 MobileLocationResult;

export interface WidgetMobileActionResult<T extends MobileActionResult> {
  result?: T;
  hasResult: boolean;
  error?: string;
  hasError: boolean;
}

export interface ProcessImageDescriptor {
  processImageFunction: string;
}

export interface ProcessLaunchResultDescriptor {
  processLaunchResultFunction?: string;
}

export interface LaunchMapDescriptor extends ProcessLaunchResultDescriptor {
  getLocationFunction: string;
}

export interface ScanQrCodeDescriptor {
  processQrCodeFunction: string;
}

export interface MakePhoneCallDescriptor extends ProcessLaunchResultDescriptor {
  getPhoneNumberFunction: string;
}

export interface GetLocationDescriptor {
  processLocationFunction: string;
}

export type WidgetMobileActionDescriptors = ProcessImageDescriptor &
                                            LaunchMapDescriptor &
                                            ScanQrCodeDescriptor &
                                            MakePhoneCallDescriptor &
                                            GetLocationDescriptor;

export interface WidgetMobileActionDescriptor extends WidgetMobileActionDescriptors {
  type: WidgetMobileActionType;
  handleErrorFunction?: string;
  handleEmptyResultFunction?: string;
}

export interface CustomActionDescriptor {
  customFunction?: string;
  customResources?: Array<WidgetResource>;
  customHtml?: string;
  customCss?: string;
  customModules?: Type<any>[];
}

export interface WidgetActionDescriptor extends CustomActionDescriptor {
  id: string;
  name: string;
  icon: string;
  displayName?: string;
  type: WidgetActionType;
  targetDashboardId?: string;
  targetDashboardStateId?: string;
  openRightLayout?: boolean;
  openNewBrowserTab?: boolean;
  openInPopover?: boolean;
  popoverHideDashboardToolbar?: boolean;
  popoverPreferredPlacement?: PopoverPlacement;
  popoverHideOnClickOutside?: boolean;
  popoverWidth?: string;
  popoverHeight?: string;
  popoverStyle?: { [klass: string]: any };
  openInSeparateDialog?: boolean;
  dialogTitle?: string;
  dialogHideDashboardToolbar?: boolean;
  dialogWidth?: number;
  dialogHeight?: number;
  setEntityId?: boolean;
  stateEntityParamName?: string;
  mobileAction?: WidgetMobileActionDescriptor;
  useShowWidgetActionFunction?: boolean;
  showWidgetActionFunction?: string;
}

export interface WidgetComparisonSettings {
  comparisonEnabled?: boolean;
  timeForComparison?: moment_.unitOfTime.DurationConstructor;
  comparisonCustomIntervalValue?: number;
}

export interface WidgetSettings {
  [key: string]: any;
}

export enum WidgetConfigMode {
  basic = 'basic',
  advanced = 'advanced'
}

export interface WidgetConfig {
  configMode?: WidgetConfigMode;
  title?: string;
  titleFont?: Font;
  titleColor?: string;
  titleIcon?: string;
  showTitle?: boolean;
  showTitleIcon?: boolean;
  iconColor?: string;
  iconSize?: string;
  titleTooltip?: string;
  dropShadow?: boolean;
  enableFullscreen?: boolean;
  useDashboardTimewindow?: boolean;
  displayTimewindow?: boolean;
  timewindow?: Timewindow;
  timewindowStyle?: TimewindowStyle;
  desktopHide?: boolean;
  mobileHide?: boolean;
  mobileHeight?: number;
  mobileOrder?: number;
  color?: string;
  backgroundColor?: string;
  padding?: string;
  margin?: string;
  borderRadius?: string;
  widgetStyle?: ComponentStyle;
  widgetCss?: string;
  titleStyle?: ComponentStyle;
  units?: string;
  decimals?: number;
  noDataDisplayMessage?: string;
  pageSize?: number;
  actions?: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  settings?: WidgetSettings;
  alarmSource?: Datasource;
  alarmFilterConfig?: AlarmFilterConfig;
  datasources?: Array<Datasource>;
  targetDeviceAliasIds?: Array<string>;
  [key: string]: any;
}

export interface BaseWidgetInfo {
  id?: string;
  typeFullFqn: string;
  type: widgetType;
}

export interface Widget extends BaseWidgetInfo {
  typeId?: WidgetTypeId;
  sizeX: number;
  sizeY: number;
  row: number;
  col: number;
  config: WidgetConfig;
}

export interface WidgetInfo extends BaseWidgetInfo {
  title: string;
  image?: string;
  description?: string;
  deprecated?: boolean;
}

export interface GroupInfo {
  formIndex: number;
  GroupTitle: string;
}

export interface JsonSchema {
  type: string;
  title?: string;
  properties: {[key: string]: any};
  required?: string[];
}

export interface JsonSettingsSchema {
  schema?: JsonSchema;
  form?: any[];
  groupInfoes?: GroupInfo[];
}

export interface WidgetPosition {
  row: number;
  column: number;
}

export interface WidgetSize {
  sizeX: number;
  sizeY: number;
}

export interface IWidgetSettingsComponent {
  aliasController: IAliasController;
  dashboard: Dashboard;
  widget: Widget;
  widgetConfig: WidgetConfigComponentData;
  functionScopeVariables: string[];
  settings: WidgetSettings;
  settingsChanged: Observable<WidgetSettings>;
  validateSettings(): boolean;
  [key: string]: any;
}

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class WidgetSettingsComponent extends PageComponent implements
  IWidgetSettingsComponent, OnInit, AfterViewInit {

  aliasController: IAliasController;

  dashboard: Dashboard;

  widget: Widget;

  widgetConfigValue: WidgetConfigComponentData;

  set widgetConfig(value: WidgetConfigComponentData) {
    this.widgetConfigValue = value;
    this.onWidgetConfigSet(value);
  }

  get widgetConfig(): WidgetConfigComponentData {
    return this.widgetConfigValue;
  }

  functionScopeVariables: string[];

  settingsValue: WidgetSettings;

  private settingsSet = false;

  set settings(value: WidgetSettings) {
    if (!value) {
      this.settingsValue = this.defaultSettings();
    } else {
      this.settingsValue = {...this.defaultSettings(), ...value};
    }
    if (!this.settingsSet) {
      this.settingsSet = true;
      this.setupSettings(this.settingsValue);
    } else {
      this.updateSettings(this.settingsValue);
    }
  }

  get settings(): WidgetSettings {
    return this.settingsValue;
  }

  settingsChangedEmitter = new EventEmitter<WidgetSettings>();
  settingsChanged = this.settingsChangedEmitter.asObservable();

  protected constructor(@Inject(Store) protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {}

  ngAfterViewInit(): void {
    if (!this.validateSettings()) {
      setTimeout(() => {
        this.onSettingsChanged(this.prepareOutputSettings(this.settingsForm().getRawValue()));
      }, 0);
    }
  }

  public validateSettings(): boolean {
    return this.settingsForm().valid;
  }

  protected setupSettings(settings: WidgetSettings) {
    this.onSettingsSet(this.prepareInputSettings(settings));
    this.updateValidators(false);
    for (const trigger of this.validatorTriggers()) {
      const path = trigger.split('.');
      let control: AbstractControl = this.settingsForm();
      for (const part of path) {
        control = control.get(part);
      }
      control.valueChanges.subscribe(() => {
        this.updateValidators(true, trigger);
      });
    }
    this.settingsForm().valueChanges.subscribe(() => {
      this.onSettingsChanged(this.prepareOutputSettings(this.settingsForm().getRawValue()));
    });
  }

  protected updateSettings(settings: WidgetSettings) {
    settings = this.prepareInputSettings(settings);
    this.settingsForm().reset(settings, {emitEvent: false});
    this.doUpdateSettings(this.settingsForm(), settings);
    this.updateValidators(false);
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
  }

  protected validatorTriggers(): string[] {
    return [];
  }

  protected onSettingsChanged(updated: WidgetSettings) {
    this.settingsValue = updated;
    this.settingsChangedEmitter.emit(this.settingsValue);
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return settings;
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings;
  }

  protected abstract settingsForm(): UntypedFormGroup;

  protected abstract onSettingsSet(settings: WidgetSettings);

  protected defaultSettings(): WidgetSettings {
    return {};
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
  }

}
