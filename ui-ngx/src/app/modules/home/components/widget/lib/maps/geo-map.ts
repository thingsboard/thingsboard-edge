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

import {
  DEFAULT_ZOOM_LEVEL,
  defaultGeoMapSettings,
  GeoMapSettings,
  latLngPointToBounds,
  MapZoomAction,
  TbCircleData,
  TbPolygonCoordinate,
  TbPolygonCoordinates,
  TbPolygonRawCoordinate,
  TbPolygonRawCoordinates
} from '@shared/models/widget/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { forkJoin, Observable, of } from 'rxjs';
import L from 'leaflet';
import { map, tap } from 'rxjs/operators';
import { TbMapLayer } from '@home/components/widget/lib/maps/map-layer';
import { TbMap } from '@home/components/widget/lib/maps/map';

export class TbGeoMap extends TbMap<GeoMapSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<GeoMapSettings>,
              protected containerElement: HTMLElement) {
    super(ctx, inputSettings, containerElement);
  }

  protected defaultSettings(): GeoMapSettings {
    return defaultGeoMapSettings;
  }

  protected createMap(): Observable<L.Map> {
    const map = L.map(this.mapElement, {
      scrollWheelZoom: this.settings.zoomActions.includes(MapZoomAction.scroll),
      doubleClickZoom: this.settings.zoomActions.includes(MapZoomAction.doubleClick),
      zoomControl: this.settings.zoomActions.includes(MapZoomAction.controlButtons),
      zoom: this.settings.defaultZoomLevel || DEFAULT_ZOOM_LEVEL,
      center: this.defaultCenterPosition
    }).setView(this.defaultCenterPosition, this.settings.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
    return of(map);
  }

  protected onResize(): void {}

  protected fitBounds(bounds: L.LatLngBounds) {
    if (bounds.isValid()) {
      if (!this.settings.fitMapBounds && this.settings.defaultZoomLevel) {
        this.map.setZoom(this.settings.defaultZoomLevel, { animate: false });
        if (this.settings.useDefaultCenterPosition) {
          this.map.panTo(this.defaultCenterPosition, { animate: false });
        }
        else {
          this.map.panTo(bounds.getCenter());
        }
      } else {
        this.map.once('zoomend', () => {
          let minZoom = this.settings.minZoomLevel;
          if (this.settings.defaultZoomLevel) {
            minZoom = Math.max(minZoom, this.settings.defaultZoomLevel);
          }
          if (this.map.getZoom() > minZoom) {
            this.map.setZoom(minZoom, { animate: false });
          }
        });
        if (this.settings.useDefaultCenterPosition) {
          bounds = bounds.extend(this.defaultCenterPosition);
        }
        this.map.fitBounds(bounds, { padding: [50, 50], animate: false });
        this.map.invalidateSize();
      }
    }
  }

  protected doSetupControls(): Observable<any> {
    return this.loadLayers().pipe(
      tap((layers: L.TB.LayerData[]) => {
        if (layers.length) {
          const layer = layers[0];
          layer.layer.addTo(this.map);
          this.map.attributionControl.setPrefix(layer.attributionPrefix);
          if (layers.length > 1) {
            const sidebar = this.getSidebar();
            L.TB.layers({
              layers,
              sidebar,
              position: this.settings.controlsPosition,
              uiClass: 'tb-layers',
              paneTitle: this.ctx.translate.instant('widgets.maps.layer.map-layers'),
              buttonTitle: this.ctx.translate.instant('widgets.maps.layer.layers'),
            }).addTo(this.map);
          }
        }
      })
    );

  }

  private loadLayers(): Observable<L.TB.LayerData[]> {
    const layers = this.settings.layers.map(settings => TbMapLayer.fromSettings(this.ctx, settings));
    return forkJoin(layers.map(layer => layer.loadLayer(this.map))).pipe(
      map((layersData) => {
        return layersData.filter(l => l !== null);
      })
    );
  }

  public locationDataToLatLng(position: {x: number; y: number}): L.LatLng {
    return L.latLng(position.x, position.y) as L.LatLng;
  }

  public latLngToLocationData(position: L.LatLng): {x: number; y: number} {
    position = position ? latLngPointToBounds(position, this.southWest, this.northEast, 0) : {lat: null, lng: null} as L.LatLng;
    return {
      x: position.lat,
      y: position.lng
    }
  }

  public polygonDataToCoordinates(expression: TbPolygonRawCoordinates): TbPolygonRawCoordinates {
    return (expression).map((el: TbPolygonRawCoordinate) => {
      if (!Array.isArray(el[0]) && !Array.isArray(el[1]) && el.length === 2) {
        return el;
      } else if (Array.isArray(el) && el.length) {
        return this.polygonDataToCoordinates(el as TbPolygonRawCoordinates) as TbPolygonRawCoordinate;
      } else {
        return null;
      }
    }).filter(el => !!el);
  }

  public coordinatesToPolygonData(coordinates: TbPolygonCoordinates): TbPolygonRawCoordinates {
    if (coordinates.length) {
      return coordinates.map((point: TbPolygonCoordinate) => {
        if (Array.isArray(point)) {
          return this.coordinatesToPolygonData(point) as TbPolygonRawCoordinate;
        } else {
          const convertPoint = latLngPointToBounds(point, this.southWest, this.northEast);
          return [convertPoint.lat, convertPoint.lng];
        }
      });
    }
    return [];
  }

  public circleDataToCoordinates(circle: TbCircleData): TbCircleData {
    const centerPoint = latLngPointToBounds(new L.LatLng(circle.latitude, circle.longitude), this.southWest, this.northEast);
    circle.latitude = centerPoint.lat;
    circle.longitude = centerPoint.lng;
    return circle;
  }

  public coordinatesToCircleData(center: L.LatLng, radius: number): TbCircleData {
    let circleData: TbCircleData = null;
    if (center) {
      const position = latLngPointToBounds(center, this.southWest, this.northEast);
      circleData = {
        latitude: position.lat,
        longitude: position.lng,
        radius
      };
    }
    return circleData;
  }


}
