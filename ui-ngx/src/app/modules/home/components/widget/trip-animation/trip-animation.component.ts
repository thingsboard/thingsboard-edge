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

import L from 'leaflet';
import _ from 'lodash';
import tinycolor from 'tinycolor2';
import { interpolateOnPointSegment } from 'leaflet-geometryutil';

import { Component, OnInit, Input, ViewChild, AfterViewInit, ChangeDetectorRef, SecurityContext } from '@angular/core';
import { MapWidgetController, TbMapWidgetV2 } from '../lib/maps/map-widget2';
import { MapProviders } from '../lib/maps/map-models';
import { parseArray, parseTemplate, safeExecute } from '@app/core/utils';
import { initSchema, addToSchema, addGroupInfo } from '@app/core/schema-utils';
import { tripAnimationSchema } from '../lib/maps/schemes';
import { DomSanitizer } from '@angular/platform-browser';
import { WidgetContext } from '@app/modules/home/models/widget-component.models';
import { getRatio, findAngle } from '../lib/maps/maps-utils';
import { JsonSettingsSchema, WidgetConfig } from '@shared/models/widget.models';


@Component({
  // tslint:disable-next-line:component-selector
  selector: 'trip-animation',
  templateUrl: './trip-animation.component.html',
  styleUrls: ['./trip-animation.component.scss']
})
export class TripAnimationComponent implements OnInit, AfterViewInit {

  constructor(private cd: ChangeDetectorRef, private sanitizer: DomSanitizer) { }

  @Input() ctx: WidgetContext;

  @ViewChild('map') mapContainer;

  mapWidget: MapWidgetController;
  historicalData;
  intervals;
  normalizationStep = 1000;
  interpolatedData = [];
  widgetConfig: WidgetConfig;
  settings;
  mainTooltip = '';
  visibleTooltip = false;
  activeTrip;

  static getSettingsSchema(): JsonSettingsSchema {
    const schema = initSchema();
    addToSchema(schema, TbMapWidgetV2.getProvidersSchema());
    addGroupInfo(schema, 'Map Provider Settings');
    addToSchema(schema, tripAnimationSchema);
    addGroupInfo(schema, 'Trip Animation Settings');
    return schema;
  }

  ngOnInit(): void {
    this.widgetConfig = this.ctx.widgetConfig;
    const settings = {
      normalizationStep: 1000,
      showLabel: false,
      buttonColor: tinycolor(this.widgetConfig.color).setAlpha(0.54).toRgbString(),
      disabledButtonColor: tinycolor(this.widgetConfig.color).setAlpha(0.3).toRgbString(),
      rotationAngle: 0
    }
    this.settings = { ...settings, ...this.ctx.settings };
    const subscription = this.ctx.subscriptions[Object.keys(this.ctx.subscriptions)[0]];
    if (subscription) subscription.callbacks.onDataUpdated = (updated) => {
      this.historicalData = parseArray(this.ctx.data);
      this.activeTrip = this.historicalData[0][0];
      this.calculateIntervals();
      this.timeUpdated(this.intervals[0]);
      this.mapWidget.map.updatePolylines(this.interpolatedData.map(ds => _.values(ds)));

      this.mapWidget.map.map?.invalidateSize();
      this.cd.detectChanges();
    }
  }

  ngAfterViewInit() {
    const ctxCopy: WidgetContext = _.cloneDeep(this.ctx);
    ctxCopy.settings.showLabel = false;
    ctxCopy.settings.showTooltip = false;
    this.mapWidget = new MapWidgetController(MapProviders.openstreet, false, ctxCopy, this.mapContainer.nativeElement);
  }

  timeUpdated(time: number) {
    const currentPosition = this.interpolatedData.map(dataSource => dataSource[time]);
    this.activeTrip = currentPosition[0];
    if (this.settings.showPolygon) {
      this.mapWidget.map.updatePolygons(this.interpolatedData);
    }
    this.mapWidget.map.updateMarkers(currentPosition);
  }

  setActiveTrip() {

  }

  calculateIntervals() {
    this.historicalData.forEach((dataSource, index) => {
      this.intervals = [];
      for (let time = dataSource[0]?.time; time < dataSource[dataSource.length - 1]?.time; time += this.normalizationStep) {
        this.intervals.push(time);
      }
      this.intervals.push(dataSource[dataSource.length - 1]?.time);
      this.interpolatedData[index] = this.interpolateArray(dataSource, this.intervals);
    });
  }

  showHideTooltip() {
    const tooltipText: string = this.settings.useTooltipFunction ?
      safeExecute(this.settings.tooolTipFunction, [this.activeTrip, this.historicalData, 0])
      : this.settings.tooltipPattern;

    this.mainTooltip = this.sanitizer.sanitize(SecurityContext.HTML, parseTemplate(tooltipText, this.activeTrip))
    this.visibleTooltip = !this.visibleTooltip;
  }

  interpolateArray(originData, interpolatedIntervals) {

    const result = {};

    for (let i = 1, j = 0; i < originData.length && j < interpolatedIntervals.length;) {
      const currentTime = interpolatedIntervals[j];
      while (originData[i].time < currentTime) i++;
      const before = originData[i - 1];
      const after = originData[i];
      const interpolation = interpolateOnPointSegment(
        new L.Point(before.latitude, before.longitude),
        new L.Point(after.latitude, after.longitude),
        getRatio(before.time, after.time, currentTime));
      result[currentTime] = ({
        ...originData[i],
        rotationAngle: findAngle(before, after) + this.settings.rotationAngle,
        latitude: interpolation.x,
        longitude: interpolation.y
      });
      j++;
    }
    return result;
  }
}

export let TbTripAnimationWidget = TripAnimationComponent;

