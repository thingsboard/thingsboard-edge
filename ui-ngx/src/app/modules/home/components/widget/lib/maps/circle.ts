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

import L, { LeafletMouseEvent } from 'leaflet';
import { CircleData, WidgetCircleSettings } from '@home/components/widget/lib/maps/map-models';
import {
  functionValueCalculator,
  parseWithTranslation
} from '@home/components/widget/lib/maps/common-maps-utils';
import LeafletMap from '@home/components/widget/lib/maps/leaflet-map';
import { createTooltip } from '@home/components/widget/lib/maps/maps-utils';
import { FormattedData } from '@shared/models/widget.models';
import { fillDataPattern, processDataPattern, safeExecute } from '@core/utils';

export class Circle {

  private editing = false;
  private circleData: CircleData;

  leafletCircle: L.Circle;
  tooltip: L.Popup;

  constructor(private map: LeafletMap,
              private data: FormattedData,
              private dataSources: FormattedData[],
              private settings: Partial<WidgetCircleSettings>,
              private onDragendListener?,
              snappable = false) {
    this.circleData = this.map.convertToCircleFormat(JSON.parse(data[this.settings.circleKeyName]));
    const centerPosition = this.createCenterPosition();
    const circleFillColor = this.getFillColor();
    const circleStrokeColor = this.getStrokeColor();
    this.leafletCircle = L.circle(centerPosition, {
      radius: this.circleData.radius,
      fillColor: circleFillColor,
      color: circleStrokeColor,
      weight: settings.circleStrokeWeight,
      fillOpacity: settings.circleFillColorOpacity,
      opacity: settings.circleStrokeOpacity,
      pmIgnore: !settings.editableCircle,
      snapIgnore: !snappable
    }).addTo(this.map.map);

    if (settings.showCircleLabel) {
      this.updateLabel();
    }

    if (settings.showCircleTooltip) {
      this.tooltip = createTooltip(this.leafletCircle, settings, data.$datasource,
        settings.autoCloseCircleTooltip, settings.showCircleTooltipAction);
      this.updateTooltip();
    }

    this.createEventListeners();
  }

  private createEventListeners() {
    if (this.settings.editableCircle && this.onDragendListener) {
      // Change center position (call in drag drop mode)
      this.leafletCircle.on('pm:dragstart', () => this.editing = true);
      this.leafletCircle.on('pm:dragend', () => this.editing = false);
      // Change radius (call in edit mode)
      this.leafletCircle.on('pm:markerdragstart', () => this.editing = true);
      this.leafletCircle.on('pm:markerdragend', () => this.editing = false);
      this.leafletCircle.on('pm:edit', (e) => this.onDragendListener(e, this.data));
    }

    if (this.settings.circleClick) {
      this.leafletCircle.on('click', (event: LeafletMouseEvent) => {
        for (const action in this.settings.circleClick) {
          if (typeof (this.settings.circleClick[action]) === 'function') {
            this.settings.circleClick[action](event.originalEvent, this.data.$datasource);
          }
        }
      });
    }
  }

  private updateLabel() {
    this.leafletCircle.unbindTooltip();
    if (this.settings.showCircleLabel) {
      if (!this.map.circleLabelText || this.settings.useCircleLabelFunction) {
        const pattern = this.settings.useCircleLabelFunction ?
          safeExecute(this.settings.parsedCircleLabelFunction,
            [this.data, this.dataSources, this.data.dsIndex]) : this.settings.circleLabel;
        this.map.circleLabelText = parseWithTranslation.prepareProcessPattern(pattern, true);
        this.map.replaceInfoTooltipCircle = processDataPattern(this.map.circleLabelText, this.data);
      }
      const circleLabelText = fillDataPattern(this.map.circleLabelText, this.map.replaceInfoTooltipCircle, this.data);
      const labelColor = this.map.ctx.widgetConfig.color;
      this.leafletCircle.bindTooltip(`<div style="color: ${labelColor};"><b>${circleLabelText}</b></div>`,
        { className: 'tb-polygon-label', permanent: true, direction: 'center'})
        .openTooltip(this.leafletCircle.getLatLng());
    }
  }

  private updateTooltip() {
    const pattern = this.settings.useCircleTooltipFunction ?
      safeExecute(this.settings.parsedCircleTooltipFunction, [this.data, this.dataSources, this.data.dsIndex]) :
      this.settings.circleTooltipPattern;
    this.tooltip.setContent(parseWithTranslation.parseTemplate(pattern, this.data, true));
  }

  private updateCircleColor() {
    const circleFillColor = this.getFillColor();
    const circleStrokeColor = this.getStrokeColor();
    const style: L.PathOptions = {
      fillColor: circleFillColor,
      color: circleStrokeColor
    };
    this.leafletCircle.setStyle(style);
  }

  updateCircle(data: FormattedData, dataSources: FormattedData[]) {
    if (this.editing) {
      return;
    }
    this.data = data;
    this.dataSources = dataSources;
    this.circleData = this.map.convertToCircleFormat(JSON.parse(data[this.settings.circleKeyName]));
    const newCenterPosition = this.createCenterPosition();
    if (!this.leafletCircle.getLatLng().equals(newCenterPosition) && !this.editing) {
      this.leafletCircle.setLatLng(newCenterPosition);
    }
    if (this.leafletCircle.getRadius() !== this.circleData.radius) {
      this.leafletCircle.setRadius(this.circleData.radius);
    }
    if (this.settings.showCircleLabel) {
      this.updateLabel();
    }
    if (this.settings.showCircleTooltip) {
      this.updateTooltip();
    }
    if (this.settings.useCircleStrokeColorFunction || this.settings.useCircleFillColorFunction) {
      this.updateCircleColor();
    }
  }

  private createCenterPosition(): L.LatLng {
    return new L.LatLng(this.circleData.latitude, this.circleData.longitude);
  }

  private getFillColor(): string | null {
    return functionValueCalculator(this.settings.useCircleFillColorFunction, this.settings.parsedCircleFillColorFunction,
      [this.data, this.dataSources, this.data.dsIndex], this.settings.circleFillColor);
  }

  private getStrokeColor(): string | null {
    return functionValueCalculator(this.settings.useCircleStrokeColorFunction, this.settings.parsedCircleStrokeColorFunction,
      [this.data, this.dataSources, this.data.dsIndex], this.settings.circleStrokeColor);
  }

}
