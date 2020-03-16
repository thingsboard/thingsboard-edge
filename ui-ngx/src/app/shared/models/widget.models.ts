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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { Timewindow } from '@shared/models/time/time.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { DataKeyType } from './telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import * as moment_ from 'moment';

export enum widgetType {
  timeseries = 'timeseries',
  latest = 'latest',
  rpc = 'rpc',
  alarm = 'alarm',
  static = 'static'
}

export interface WidgetTypeTemplate {
  bundleAlias: string;
  alias: string;
}

export interface WidgetTypeData {
  name: string;
  icon: string;
  isMdiIcon?: boolean;
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
          bundleAlias: 'charts',
          alias: 'basic_timeseries'
        }
      }
    ],
    [
      widgetType.latest,
      {
        name: 'widget.latest-values',
        icon: 'track_changes',
        configHelpLinkId: 'widgetsConfigLatest',
        template: {
          bundleAlias: 'cards',
          alias: 'attributes_card'
        }
      }
    ],
    [
      widgetType.rpc,
      {
        name: 'widget.rpc',
        icon: 'mdi:developer-board',
        configHelpLinkId: 'widgetsConfigRpc',
        isMdiIcon: true,
        template: {
          bundleAlias: 'gpio_widgets',
          alias: 'basic_gpio_control'
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
          bundleAlias: 'alarm_widgets',
          alias: 'alarms_table'
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
          bundleAlias: 'cards',
          alias: 'html_card'
        }
      }
    ]
  ]
);

export interface WidgetResource {
  url: string;
}

export interface WidgetActionSource {
  name: string;
  value: string;
  multiple: boolean;
}

export const widgetActionSources: {[acionSourceId: string]: WidgetActionSource} = {
    headerButton:
    {
      name: 'widget-action.header-button',
      value: 'headerButton',
      multiple: true,
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
  defaultConfig: string;
  sizeX: number;
  sizeY: number;
}

export interface WidgetTypeParameters {
  useCustomDatasources?: boolean;
  maxDatasources?: number;
  maxDataKeys?: number;
  dataKeysOptional?: boolean;
  stateData?: boolean;
}

export interface WidgetControllerDescriptor {
  widgetTypeFunction?: any;
  settingsSchema?: string | any;
  dataKeySettingsSchema?: string | any;
  typeParameters?: WidgetTypeParameters;
  actionSources?: {[actionSourceId: string]: WidgetActionSource};
}

export interface WidgetType extends BaseData<WidgetTypeId> {
  tenantId: TenantId;
  bundleAlias: string;
  alias: string;
  name: string;
  descriptor: WidgetTypeDescriptor;
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
  showMin: boolean;
  showMax: boolean;
  showAvg: boolean;
  showTotal: boolean;
}

export function defaultLegendConfig(wType: widgetType): LegendConfig {
  return {
    direction: LegendDirection.column,
    position: LegendPosition.bottom,
    showMin: false,
    showMax: false,
    showAvg: wType === widgetType.timeseries,
    showTotal: false
  };
}

export interface KeyInfo {
  name: string;
  label?: string;
  color?: string;
  funcBody?: string;
  postFuncBody?: string;
  units?: string;
  decimals?: number;
}

export interface DataKey extends KeyInfo {
  type: DataKeyType;
  pattern?: string;
  title?: string;
  settings?: any;
  usePostProcessing?: boolean;
  hidden?: boolean;
  inLegend?: boolean;
  _hash?: number;
}

export enum DatasourceType {
  function = 'function',
  entity = 'entity'
}

export const datasourceTypeTranslationMap = new Map<DatasourceType, string>(
  [
    [ DatasourceType.function, 'function.function' ],
    [ DatasourceType.entity, 'entity.entity' ]
  ]
);

export interface Datasource {
  type?: DatasourceType | any;
  name?: string;
  aliasName?: string;
  dataKeys?: Array<DataKey>;
  entityType?: EntityType;
  entityId?: string;
  entityName?: string;
  entityAliasId?: string;
  unresolvedStateEntity?: boolean;
  dataReceived?: boolean;
  entity?: BaseData<EntityId>;
  entityLabel?: string;
  entityDescription?: string;
  generated?: boolean;
  isAdditional?: boolean;
  [key: string]: any;
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
  customPretty = 'customPretty'
}

export const widgetActionTypeTranslationMap = new Map<WidgetActionType, string>(
  [
    [ WidgetActionType.openDashboardState, 'widget-action.open-dashboard-state' ],
    [ WidgetActionType.updateDashboardState, 'widget-action.update-dashboard-state' ],
    [ WidgetActionType.openDashboard, 'widget-action.open-dashboard' ],
    [ WidgetActionType.custom, 'widget-action.custom' ],
    [ WidgetActionType.customPretty, 'widget-action.custom-pretty' ]
  ]
);

export interface CustomActionDescriptor {
  customFunction?: string;
  customResources?: Array<WidgetResource>;
  customHtml?: string;
  customCss?: string;
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
  setEntityId?: boolean;
  stateEntityParamName?: string;
}

export interface WidgetComparisonSettings {
  comparisonEnabled?: boolean;
  timeForComparison?: moment_.unitOfTime.DurationConstructor;
}

export interface WidgetConfig {
  title?: string;
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
  showLegend?: boolean;
  legendConfig?: LegendConfig;
  timewindow?: Timewindow;
  mobileHeight?: number;
  mobileOrder?: number;
  color?: string;
  backgroundColor?: string;
  padding?: string;
  margin?: string;
  widgetStyle?: {[klass: string]: any};
  titleStyle?: {[klass: string]: any};
  units?: string;
  decimals?: number;
  actions?: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  settings?: any;
  alarmSource?: Datasource;
  alarmSearchStatus?: AlarmSearchStatus;
  alarmsPollingInterval?: number;
  alarmsMaxCountLoad?: number;
  alarmsFetchSize?: number;
  datasources?: Array<Datasource>;
  targetDeviceAliasIds?: Array<string>;
  [key: string]: any;
}

export interface Widget {
  id?: string;
  typeId?: WidgetTypeId;
  isSystemType: boolean;
  bundleAlias: string;
  typeAlias: string;
  type: widgetType;
  title: string;
  sizeX: number;
  sizeY: number;
  row: number;
  col: number;
  config: WidgetConfig;
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
  groupInfoes?: GroupInfo[]
}

export interface WidgetPosition {
  row: number;
  column: number;
}

export interface WidgetSize {
  sizeX: number;
  sizeY: number;
}
