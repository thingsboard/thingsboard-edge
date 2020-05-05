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

import L, { LatLngExpression, LatLngTuple, LeafletMouseEvent } from 'leaflet';
import { createTooltip, parseWithTranslation, safeExecute } from './maps-utils';
import { PolygonSettings } from './map-models';
import { DatasourceData } from '@app/shared/models/widget.models';

export class Polygon {

    leafletPoly: L.Polygon;
    tooltip;
    data;
    dataSources;

    constructor(public map, polyData: DatasourceData, dataSources, private settings: PolygonSettings) {
        this.leafletPoly = L.polygon(polyData.data, {
            fill: true,
            fillColor: settings.polygonColor,
            color: settings.polygonStrokeColor,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity
        }).addTo(this.map);
        this.dataSources = dataSources;
        this.data = polyData;
        if (settings.showPolygonTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, settings, polyData.datasource);
            this.updateTooltip(polyData);
        }
        if (settings.polygonClick) {
            this.leafletPoly.on('click', (event: LeafletMouseEvent) => {
                for (const action in this.settings.polygonClick) {
                    if (typeof (this.settings.polygonClick[action]) === 'function') {
                        this.settings.polygonClick[action](event.originalEvent, polyData.datasource);
                    }
                }
            });
        }
    }

    updateTooltip(data: DatasourceData) {
        const pattern = this.settings.useTooltipFunction ?
            safeExecute(this.settings.tooltipFunction, [this.data, this.dataSources, this.data.dsIndex]) : this.settings.tooltipPattern;
        this.tooltip.setContent(parseWithTranslation.parseTemplate(pattern, data, true));
    }

    updatePolygon(data: LatLngTuple[], dataSources: DatasourceData[], settings: PolygonSettings) {
        this.data = data;
        this.dataSources = dataSources;
        this.leafletPoly.setLatLngs(data);
        if (settings.showPolygonTooltip)
            this.updateTooltip(this.data);
        this.updatePolygonColor(settings);
    }

    removePolygon() {
        this.map.removeLayer(this.leafletPoly);
    }

    updatePolygonColor(settings) {
        const style: L.PathOptions = {
            fill: true,
            fillColor: settings.polygonColor,
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
}
