///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import { createTooltip, isCutPolygon } from './maps-utils';
import {
  functionValueCalculator,
  parseWithTranslation
} from './common-maps-utils';
import { PolygonSettings, UnitedMapSettings } from './map-models';
import { FormattedData } from '@shared/models/widget.models';
import { fillDataPattern, processDataPattern, safeExecute } from '@core/utils';

export class Polygon {

    private editing = false;

    leafletPoly: L.Polygon;
    tooltip: L.Popup;
    data: FormattedData;
    dataSources: FormattedData[];

    constructor(public map, data: FormattedData, dataSources: FormattedData[], private settings: UnitedMapSettings,
                private onDragendListener?) {
        this.dataSources = dataSources;
        this.data = data;
        const polygonColor = this.getPolygonColor(settings);
        const polygonStrokeColor = this.getPolygonStrokeColor(settings);
        const polyData = data[this.settings.polygonKeyName];
        const polyConstructor = isCutPolygon(polyData) || polyData.length !== 2 ? L.polygon : L.rectangle;
        this.leafletPoly = polyConstructor(polyData, {
          fill: true,
          fillColor: polygonColor,
          color: polygonStrokeColor,
          weight: settings.polygonStrokeWeight,
          fillOpacity: settings.polygonOpacity,
          opacity: settings.polygonStrokeOpacity,
          pmIgnore: !settings.editablePolygon,
          snapIgnore: !settings.snappable
        }).addTo(this.map);

        if (settings.showPolygonLabel) {
          this.updateLabel(settings);
        }

        if (settings.showPolygonTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, settings, data.$datasource,
              settings.autoClosePolygonTooltip, settings.showPolygonTooltipAction);
            this.updateTooltip(data);
        }
        this.createEventListeners();
    }

    private createEventListeners() {
      if (this.settings.editablePolygon && this.onDragendListener) {
        // Change position (call in drag drop mode)
        this.leafletPoly.on('pm:dragstart', () => this.editing = true);
        this.leafletPoly.on('pm:dragend', () => this.editing = false);
        // Rotate (call in rotate mode)
        this.leafletPoly.on('pm:rotatestart', () => this.editing = true);
        this.leafletPoly.on('pm:rotateend', () => this.editing = false);
        // Change size/point (call in edit mode)
        this.leafletPoly.on('pm:markerdragstart', () => this.editing = true);
        this.leafletPoly.on('pm:markerdragend', () => this.editing = false);
        this.leafletPoly.on('pm:edit', (e) => this.onDragendListener(e, this.data));
      }

      if (this.settings.polygonClick) {
        this.leafletPoly.on('click', (event: LeafletMouseEvent) => {
          for (const action in this.settings.polygonClick) {
            if (typeof (this.settings.polygonClick[action]) === 'function') {
              this.settings.polygonClick[action](event.originalEvent, this.data.$datasource);
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

    updateLabel(settings: PolygonSettings) {
        this.leafletPoly.unbindTooltip();
        if (settings.showPolygonLabel) {
            if (!this.map.polygonLabelText || settings.usePolygonLabelFunction) {
                const pattern = settings.usePolygonLabelFunction ?
                  safeExecute(settings.polygonLabelFunction, [this.data, this.dataSources, this.data.dsIndex]) : settings.polygonLabel;
                this.map.polygonLabelText = parseWithTranslation.prepareProcessPattern(pattern, true);
                this.map.replaceInfoLabelPolygon = processDataPattern(this.map.polygonLabelText, this.data);
            }
            const polygonLabelText = fillDataPattern(this.map.polygonLabelText, this.map.replaceInfoLabelPolygon, this.data);
            this.leafletPoly.bindTooltip(`<div style="color: ${settings.polygonLabelColor};"><b>${polygonLabelText}</b></div>`,
              { className: 'tb-polygon-label', permanent: true, sticky: true, direction: 'center' })
              .openTooltip(this.leafletPoly.getBounds().getCenter());
        }
    }

    updatePolygon(data: FormattedData, dataSources: FormattedData[], settings: PolygonSettings) {
      if (this.editing) {
        return;
      }
      this.data = data;
      this.dataSources = dataSources;
      const polyData = data[this.settings.polygonKeyName];
      if (isCutPolygon(polyData) || polyData.length !== 2) {
        if (this.leafletPoly instanceof L.Rectangle) {
          this.map.removeLayer(this.leafletPoly);
          const polygonColor = this.getPolygonColor(settings);
          const polygonStrokeColor = this.getPolygonStrokeColor(settings);
          this.leafletPoly = L.polygon(polyData, {
            fill: true,
            fillColor: polygonColor,
            color: polygonStrokeColor,
            weight: settings.polygonStrokeWeight,
            fillOpacity: settings.polygonOpacity,
            opacity: settings.polygonStrokeOpacity,
            pmIgnore: !settings.editablePolygon
          }).addTo(this.map);
          if (settings.showPolygonTooltip) {
            this.tooltip = createTooltip(this.leafletPoly, settings, data.$datasource,
              settings.autoClosePolygonTooltip, settings.showPolygonTooltipAction);
          }
          this.createEventListeners();
        } else {
          this.leafletPoly.setLatLngs(polyData);
        }
      } else if (polyData.length === 2) {
        const bounds = new L.LatLngBounds(polyData);
        // @ts-ignore
        this.leafletPoly.setBounds(bounds);
      }
      if (settings.showPolygonTooltip) {
        this.updateTooltip(this.data);
      }
      if (settings.showPolygonLabel) {
        this.updateLabel(settings);
      }
      this.updatePolygonColor(settings);
    }

    removePolygon() {
        this.map.removeLayer(this.leafletPoly);
    }

    updatePolygonColor(settings: PolygonSettings) {
        const polygonColor = this.getPolygonColor(settings);
        const polygonStrokeColor = this.getPolygonStrokeColor(settings);
        const style: L.PathOptions = {
            fillColor: polygonColor,
            color: polygonStrokeColor
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

  private getPolygonStrokeColor(settings: PolygonSettings): string | null {
    return functionValueCalculator(settings.usePolygonStrokeColorFunction, settings.polygonStrokeColorFunction,
      [this.data, this.dataSources, this.data.dsIndex], settings.polygonStrokeColor);
  }
}
