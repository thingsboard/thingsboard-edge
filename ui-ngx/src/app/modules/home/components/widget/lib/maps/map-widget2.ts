///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
  defaultSettings,
  FormattedData,
  hereProviders,
  MapProviders,
  UnitedMapSettings
} from './map-models';
import LeafletMap from './leaflet-map';
import {
  commonMapSettingsSchema,
  mapPolygonSchema,
  mapProviderSchema,
  markerClusteringSettingsSchema,
  markerClusteringSettingsSchemaLeaflet,
  routeMapSettingsSchema
} from './schemes';
import { MapWidgetInterface, MapWidgetStaticInterface } from './map-widget.interface';
import {
  addCondition,
  addGroupInfo,
  addToSchema,
  initSchema,
  mergeSchemes
} from '@core/schema-utils';
import { WidgetContext } from '@app/modules/home/models/widget-component.models';
import { getDefCenterPosition, getProviderSchema, parseFunction, parseWithTranslation } from './common-maps-utils';
import { Datasource, DatasourceData, JsonSettingsSchema, WidgetActionDescriptor } from '@shared/models/widget.models';
import { EntityId } from '@shared/models/id/entity-id';
import { AttributeScope, DataKeyType, LatestTelemetry } from '@shared/models/telemetry/telemetry.models';
import { AttributeService } from '@core/http/attribute.service';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import { EntityDataPageLink } from '@shared/models/query/query.models';
import { isDefined } from '@core/utils';
import { forkJoin, Observable, of } from 'rxjs';
import { providerClass } from '@home/components/widget/lib/maps/providers';

// @dynamic
export class MapWidgetController implements MapWidgetInterface {

    constructor(
        public mapProvider: MapProviders,
        private drawRoutes: boolean,
        public ctx: WidgetContext,
        $element: HTMLElement,
        isEdit?: boolean
    ) {
        if (this.map) {
            this.map.map.remove();
            delete this.map;
        }

        this.data = ctx.data;
        if (!$element) {
            $element = ctx.$container[0];
        }
        this.settings = this.initSettings(ctx.settings, isEdit);
        this.settings.tooltipAction = this.getDescriptors('tooltipAction');
        this.settings.markerClick = this.getDescriptors('markerClick');
        this.settings.polygonClick = this.getDescriptors('polygonClick');

        const MapClass = providerClass[this.provider];
        if (!MapClass) {
            return;
        }
        parseWithTranslation.setTranslate(this.translate);
        this.map = new MapClass(this.ctx, $element, this.settings);
        this.map.saveMarkerLocation = this.setMarkerLocation;
        this.map.savePolygonLocation = this.savePolygonLocation;
        this.pageLink = {
          page: 0,
          pageSize: this.settings.mapPageSize,
          textSearch: null,
          dynamic: true
        };
        this.map.setLoading(true);
        this.ctx.defaultSubscription.subscribeAllForPaginatedData(this.pageLink, null);
    }

    map: LeafletMap;
    provider: MapProviders;
    schema: JsonSettingsSchema;
    data: DatasourceData[];
    settings: UnitedMapSettings;
    pageLink: EntityDataPageLink;

    public static dataKeySettingsSchema(): object {
        return {};
    }

    public static getProvidersSchema(mapProvider: MapProviders, ignoreImageMap = false) {
       return getProviderSchema(mapProvider, ignoreImageMap);
    }

    public static settingsSchema(mapProvider: MapProviders, drawRoutes: boolean): JsonSettingsSchema {
        const schema = initSchema();
        addToSchema(schema, this.getProvidersSchema(mapProvider));
        addGroupInfo(schema, 'Map Provider Settings');
        addToSchema(schema, commonMapSettingsSchema);
        addGroupInfo(schema, 'Common Map Settings');
        addToSchema(schema, addCondition(mapPolygonSchema, 'model.showPolygon === true', ['showPolygon']));
        addGroupInfo(schema, 'Polygon Settings');
        if (drawRoutes) {
            addToSchema(schema, routeMapSettingsSchema);
            addGroupInfo(schema, 'Route Map Settings');
        } else {
            const clusteringSchema = mergeSchemes([markerClusteringSettingsSchema,
                addCondition(markerClusteringSettingsSchemaLeaflet,
                    `model.useClusterMarkers === true && model.provider !== "image-map"`)]);
            addToSchema(schema, clusteringSchema);
            addGroupInfo(schema, 'Markers Clustering Settings');
        }
        return schema;
    }

    public static actionSources(): object {
        return {
            markerClick: {
                name: 'widget-action.marker-click',
                multiple: false
            },
            polygonClick: {
                name: 'widget-action.polygon-click',
                multiple: false
            },
            tooltipAction: {
                name: 'widget-action.tooltip-tag-action',
                multiple: true
            }
        };
    }

    translate = (key: string, defaultTranslation?: string): string => {
      if (key) {
        return (this.ctx.$injector.get(UtilsService).customTranslation(key, defaultTranslation || key)
          || this.ctx.$injector.get(TranslateService).instant(key));
      }
      return '';
    }

    getDescriptors(name: string): { [name: string]: ($event: Event, datasource: Datasource) => void } {
        const descriptors = this.ctx.actionsApi.getActionDescriptors(name);
        const actions = {};
        descriptors.forEach(descriptor => {
            actions[descriptor.name] = ($event: Event, datasource: Datasource) => this.onCustomAction(descriptor, $event, datasource);
        }, actions);
        return actions;
    }

    onInit() {
    }

    private onCustomAction(descriptor: WidgetActionDescriptor, $event: Event, entityInfo: Datasource) {
        if ($event) {
            $event.preventDefault();
            $event.stopPropagation();
        }
        const { entityId, entityName, entityLabel, entityType } = entityInfo;
        this.ctx.actionsApi.handleWidgetAction($event, descriptor, {
            entityType,
            id: entityId
        }, entityName, null, entityLabel);
    }

    setMarkerLocation = (e: FormattedData, lat?: number, lng?: number) => {
        const attributeService = this.ctx.$injector.get(AttributeService);

        const entityId: EntityId = {
            entityType: e.$datasource.entityType,
            id: e.$datasource.entityId
        };
        const attributes = [];
        const timeseries = [];

        const latProperties = [this.settings.latKeyName, this.settings.xPosKeyName];
        const lngProperties = [this.settings.lngKeyName, this.settings.yPosKeyName];
        e.$datasource.dataKeys.forEach(key => {
            let value;
            if (latProperties.includes(key.name)) {
                value = {
                    key: key.name,
                    value: isDefined(lat) ? lat : e[key.name]
                };
            } else if (lngProperties.includes(key.name)) {
              value = {
                key: key.name,
                value: isDefined(lng) ? lng : e[key.name]
              };
            }
            if (value) {
              if (key.type === DataKeyType.attribute) {
                attributes.push(value);
              }
              if (key.type === DataKeyType.timeseries) {
                timeseries.push(value);
              }
            }
        });
        const observables: Observable<any>[] = [];
        if (timeseries.length) {
            observables.push(attributeService.saveEntityTimeseries(
              entityId,
              LatestTelemetry.LATEST_TELEMETRY,
              timeseries
            ));
        }
        if (attributes.length) {
            observables.push(attributeService.saveEntityAttributes(
              entityId,
              AttributeScope.SERVER_SCOPE,
              attributes
            ));
        }
        if (observables.length) {
          return forkJoin(observables);
        } else {
          return of(null);
        }
    }

  savePolygonLocation = (e: FormattedData, coordinates?: Array<any>) => {
    const attributeService = this.ctx.$injector.get(AttributeService);

    const entityId: EntityId = {
      entityType: e.$datasource.entityType,
      id: e.$datasource.entityId
    };
    const attributes = [];
    const timeseries = [];

    const coordinatesProperties =  this.settings.polygonKeyName;
    e.$datasource.dataKeys.forEach(key => {
      let value;
      if (coordinatesProperties === key.name) {
        value = {
          key: key.name,
          value: isDefined(coordinates) ? coordinates : e[key.name]
        };
      }
      if (value) {
        if (key.type === DataKeyType.attribute) {
          attributes.push(value);
        }
        if (key.type === DataKeyType.timeseries) {
          timeseries.push(value);
        }
      }
    });
    const observables: Observable<any>[] = [];
    if (timeseries.length) {
      observables.push(attributeService.saveEntityTimeseries(
        entityId,
        LatestTelemetry.LATEST_TELEMETRY,
        timeseries
      ));
    }
    if (attributes.length) {
      observables.push(attributeService.saveEntityAttributes(
        entityId,
        AttributeScope.SERVER_SCOPE,
        attributes
      ));
    }
    if (observables.length) {
      return forkJoin(observables);
    } else {
      return of(null);
    }
  }

    initSettings(settings: UnitedMapSettings, isEditMap?: boolean): UnitedMapSettings {
        const functionParams = ['data', 'dsData', 'dsIndex'];
        this.provider = settings.provider || this.mapProvider;
        if (this.provider === MapProviders.here && !settings.mapProviderHere) {
          if (settings.mapProvider && hereProviders.includes(settings.mapProvider)) {
            settings.mapProviderHere = settings.mapProvider;
          } else {
            settings.mapProviderHere = hereProviders[0];
          }
        }
        const customOptions = {
            provider: this.provider,
            mapUrl: settings?.mapImageUrl,
            labelFunction: parseFunction(settings.labelFunction, functionParams),
            tooltipFunction: parseFunction(settings.tooltipFunction, functionParams),
            colorFunction: parseFunction(settings.colorFunction, functionParams),
            colorPointFunction: parseFunction(settings.colorPointFunction, functionParams),
            polygonColorFunction: parseFunction(settings.polygonColorFunction, functionParams),
            polygonTooltipFunction: parseFunction(settings.polygonTooltipFunction, functionParams),
            markerImageFunction: parseFunction(settings.markerImageFunction, ['data', 'images', 'dsData', 'dsIndex']),
            labelColor: this.ctx.widgetConfig.color,
            polygonKeyName: settings.polKeyName ? settings.polKeyName : settings.polygonKeyName,
            tooltipPattern: settings.tooltipPattern ||
                '<b>${entityName}</b><br/><br/><b>Latitude:</b> ${' +
                settings.latKeyName + ':7}<br/><b>Longitude:</b> ${' + settings.lngKeyName + ':7}',
            defaultCenterPosition: getDefCenterPosition(settings?.defaultCenterPosition),
            currentImage: (settings.markerImage?.length) ? {
                url: settings.markerImage,
                size: settings.markerImageSize || 34
            } : null
        };
        if (isEditMap && !settings.hasOwnProperty('draggableMarker')) {
            settings.draggableMarker = true;
        }
        if (isEditMap && !settings.hasOwnProperty('editablePolygon')) {
            settings.editablePolygon = true;
        }
        return { ...defaultSettings, ...settings, ...customOptions, };
    }

    update() {
        this.map.updateData(this.drawRoutes, this.settings.showPolygon);
        this.map.setLoading(false);
    }

    resize() {
        this.map?.invalidateSize();
        this.map.onResize();
    }

    destroy() {
      if (this.map) {
        this.map.remove();
      }
    }
}

export let TbMapWidgetV2: MapWidgetStaticInterface = MapWidgetController;


