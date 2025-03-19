///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { EntityAliases, EntityAliasInfo, getEntityAliasId } from '@shared/models/alias.models';
import { FilterInfo, Filters, getFilterId } from '@shared/models/query/query.models';
import { Dashboard } from '@shared/models/dashboard.models';
import { DatasourceType, Widget } from '@shared/models/widget.models';
import { BaseMapSettings, MapDataSourceSettings, MapType } from '@shared/models/widget/maps/map.models';
import { WidgetExportDefinition } from '@shared/models/widget/widget-export.models';

interface ExportDataSourceInfo {
  aliases: {[dataLayerIndex: number]: EntityAliasInfo};
  filters: {[dataLayerIndex: number]: FilterInfo};
}

interface MapDatasourcesInfo {
  trips?: ExportDataSourceInfo;
  markers?: ExportDataSourceInfo;
  polygons?: ExportDataSourceInfo;
  circles?: ExportDataSourceInfo;
  additionalDataSources?: ExportDataSourceInfo;
}

export const MapExportDefinition: WidgetExportDefinition<MapDatasourcesInfo> = {
  testWidget(widget: Widget): boolean {
    if (widget?.config?.settings) {
      const settings = widget.config.settings;
      if (settings.mapType && [MapType.image, MapType.geoMap].includes(settings.mapType)) {
        if (settings.trips && Array.isArray(settings.trips)) {
          return true;
        }
        if (settings.markers && Array.isArray(settings.markers)) {
          return true;
        }
        if (settings.polygons && Array.isArray(settings.polygons)) {
          return true;
        }
        if (settings.circles && Array.isArray(settings.circles)) {
          return true;
        }
      }
    }
    return false;
  },
  prepareExportInfo(dashboard: Dashboard, widget: Widget): MapDatasourcesInfo {
    const settings: BaseMapSettings = widget.config.settings as BaseMapSettings;
    const info: MapDatasourcesInfo = {};
    if (settings.trips?.length) {
      info.trips = prepareExportDataSourcesInfo(dashboard, settings.trips);
    }
    if (settings.markers?.length) {
      info.markers = prepareExportDataSourcesInfo(dashboard, settings.markers);
    }
    if (settings.polygons?.length) {
      info.polygons = prepareExportDataSourcesInfo(dashboard, settings.polygons);
    }
    if (settings.circles?.length) {
      info.circles = prepareExportDataSourcesInfo(dashboard, settings.circles);
    }
    if (settings.additionalDataSources?.length) {
      info.additionalDataSources = prepareExportDataSourcesInfo(dashboard, settings.additionalDataSources);
    }
    return info;
  },
  updateFromExportInfo(widget: Widget, entityAliases: EntityAliases, filters: Filters, info: MapDatasourcesInfo): void {
    const settings: BaseMapSettings = widget.config.settings as BaseMapSettings;
    if (info?.trips) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.trips, info.trips);
    }
    if (info?.markers) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.markers, info.markers);
    }
    if (info?.polygons) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.polygons, info.polygons);
    }
    if (info?.circles) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.circles, info.circles);
    }
    if (info?.additionalDataSources) {
      updateMapDatasourceFromExportInfo(entityAliases, filters, settings.additionalDataSources, info.additionalDataSources);
    }
  }
};

const updateMapDatasourceFromExportInfo = (entityAliases: EntityAliases,
                                           filters: Filters, settings: MapDataSourceSettings[], info: ExportDataSourceInfo): void => {
  if (info.aliases) {
    for (const dsIndexStr of Object.keys(info.aliases)) {
      const dsIndex = Number(dsIndexStr);
      if (settings[dsIndex] && settings[dsIndex].dsType === DatasourceType.entity) {
        const aliasInfo = info.aliases[dsIndex];
        settings[dsIndex].dsEntityAliasId = getEntityAliasId(entityAliases, aliasInfo);
      }
    }
  }
  if (info.filters) {
    for (const dsIndexStr of Object.keys(info.filters)) {
      const dsIndex = Number(dsIndexStr);
      if (settings[dsIndex] && settings[dsIndex].dsType === DatasourceType.entity) {
        const filterInfo = info.filters[dsIndex];
        settings[dsIndex].dsFilterId = getFilterId(filters, filterInfo);
      }
    }
  }
}

const prepareExportDataSourcesInfo = (dashboard: Dashboard, settings: MapDataSourceSettings[]): ExportDataSourceInfo => {
  const info: ExportDataSourceInfo = {
    aliases: {},
    filters: {}
  };
  settings.forEach((dsSettings, index) => {
    prepareExportDataSourceInfo(dashboard, info, dsSettings, index);
  });
  return info;
}

const prepareExportDataSourceInfo = (dashboard: Dashboard, info: ExportDataSourceInfo, settings: MapDataSourceSettings, index: number): void => {
  if (settings.dsType === DatasourceType.entity) {
    const entityAlias = dashboard.configuration.entityAliases[settings.dsEntityAliasId];
    if (entityAlias) {
      info.aliases[index] = {
        alias: entityAlias.alias,
        filter: entityAlias.filter
      };
    }
    if (settings.dsFilterId && dashboard.configuration.filters) {
      const filter = dashboard.configuration.filters[settings.dsFilterId];
      if (filter) {
        info.filters[index] = {
          filter: filter.filter,
          keyFilters: filter.keyFilters,
          editable: filter.editable
        };
      }
    }
  }
}
