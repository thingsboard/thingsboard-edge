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

import L, { LatLngExpression, LeafletMouseEvent } from 'leaflet';
import { createTooltip } from './maps-utils';
import { functionValueCalculator, parseWithTranslation, safeExecute } from './common-maps-utils';
import 'leaflet-editable/src/Leaflet.Editable';
import { FormattedData, PolygonSettings } from './map-models';

export class Polygon {

    leafletPoly: L.Polygon;
    tooltip: L.Popup;
    data: FormattedData;
    dataSources: FormattedData[];

    constructor(public map, polyData: FormattedData, dataSources: FormattedData[], private settings: PolygonSettings, onDragendListener?) {
        this.dataSources = dataSources;
        this.data = polyData;
        const polygonColor = this.getPolygonColor(settings);
        this.leafletPoly = L.polygon(polyData[this.settings.polygonKeyName], {
          fill: true,
          fillColor: polygonColor,
          color: settings.polygonStrokeColor,
          weight: settings.polygonStrokeWeight,
          fillOpacity: settings.polygonOpacity,
          opacity: settings.polygonStrokeOpacity
        }).addTo(this.map);
        if (settings.editablePolygon) {
            this.leafletPoly.enableEdit(this.map);
            if (onDragendListener) {
                this.leafletPoly.on('editable:vertex:dragend', e => onDragendListener(e, this.data));
                this.leafletPoly.on('editable:vertex:deleted', e => onDragendListener(e, this.data));
            }
        }


        if (settings.showPolygonTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, settings, polyData.$datasource);
            this.updateTooltip(polyData);
        }
        if (settings.polygonClick) {
            this.leafletPoly.on('click', (event: LeafletMouseEvent) => {
                for (const action in this.settings.polygonClick) {
                    if (typeof (this.settings.polygonClick[action]) === 'function') {
                        this.settings.polygonClick[action](event.originalEvent, polyData.$datasource);
                    }
                }
            });
        }
    }

    updateTooltip(data: FormattedData) {
        const pattern = this.settings.usePolygonTooltipFunction ?
            safeExecute(this.settings.polygonTooltipFunction, [this.data, this.dataSources, this.data.dsIndex]) :
            this.settings.polygonTooltipPattern;
        this.tooltip.setContent(parseWithTranslation.parseTemplate(pattern, data, true));
    }

    updatePolygon(data: FormattedData, dataSources: FormattedData[], settings: PolygonSettings) {
      this.data = data;
      this.dataSources = dataSources;
      if (settings.editablePolygon) {
        this.leafletPoly.disableEdit();
      }
      this.leafletPoly.setLatLngs(data[this.settings.polygonKeyName]);
      if (settings.editablePolygon) {
        this.leafletPoly.enableEdit(this.map);
      }
      if (settings.showPolygonTooltip) {
        this.updateTooltip(this.data);
      }
      this.updatePolygonColor(settings);
    }

    removePolygon() {
        this.map.removeLayer(this.leafletPoly);
    }

    updatePolygonColor(settings: PolygonSettings) {
        const polygonColor = this.getPolygonColor(settings);
        const style: L.PathOptions = {
            fill: true,
            fillColor: polygonColor,
            color: settings.polygonStrokeColor,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity
        };
        this.leafletPoly.setStyle(style);
    }

    getPolygonLatLngs() {
        return this.leafletPoly.getLatLngs();
    }

    setPolygonLatLngs(latLngs: LatLngExpression[]) {
        this.leafletPoly.setLatLngs(latLngs);
        this.leafletPoly.redraw();
    }

    private getPolygonColor(settings: PolygonSettings): string | null {
      return functionValueCalculator(settings.usePolygonColorFunction, settings.polygonColorFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.polygonColor);
    }
}
