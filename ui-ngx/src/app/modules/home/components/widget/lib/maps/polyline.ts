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

// @ts-ignore
import L, { PolylineDecorator, PolylineDecoratorOptions, Symbol } from 'leaflet';
import 'leaflet-polylinedecorator';

import { WidgetPolylineSettings } from './map-models';
import { functionValueCalculator } from '@home/components/widget/lib/maps/common-maps-utils';
import { FormattedData } from '@shared/models/widget.models';

export class Polyline {

  leafletPoly: L.Polyline;
  polylineDecorator: PolylineDecorator;

  constructor(private map: L.Map,
              locations: L.LatLng[],
              private data: FormattedData,
              private dataSources: FormattedData[],
              settings: Partial<WidgetPolylineSettings>) {

    this.leafletPoly = L.polyline(locations,
      this.getPolyStyle(settings)
    ).addTo(this.map);

    if (settings.usePolylineDecorator) {
      this.polylineDecorator = new PolylineDecorator(this.leafletPoly, this.getDecoratorSettings(settings)).addTo(this.map);
    }
  }

  getDecoratorSettings(settings: Partial<WidgetPolylineSettings>): PolylineDecoratorOptions {
    return {
      patterns: [
        {
          offset: settings.decoratorOffset,
          endOffset: settings.endDecoratorOffset,
          repeat: settings.decoratorRepeat,
          symbol: Symbol[settings.decoratorSymbol]({
            pixelSize: settings.decoratorSymbolSize,
            polygon: false,
            pathOptions: {
              color: settings.useDecoratorCustomColor ? settings.decoratorCustomColor : this.getPolyStyle(settings).color,
              stroke: true
            }
          })
        }
      ]
    };
  }

  updatePolyline(locations: L.LatLng[], data: FormattedData, dataSources: FormattedData[], settings: Partial<WidgetPolylineSettings>) {
    this.data = data;
    this.dataSources = dataSources;
    this.leafletPoly.setLatLngs(locations);
    this.leafletPoly.setStyle(this.getPolyStyle(settings));
    if (this.polylineDecorator) {
      this.polylineDecorator.setPaths(this.leafletPoly);
    }
  }

  getPolyStyle(settings: Partial<WidgetPolylineSettings>): L.PolylineOptions {
    return {
      interactive: false,
      color: functionValueCalculator(settings.useColorFunction, settings.parsedColorFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.color),
      opacity: functionValueCalculator(settings.useStrokeOpacityFunction, settings.parsedStrokeOpacityFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.strokeOpacity),
      weight: functionValueCalculator(settings.useStrokeWeightFunction, settings.parsedStrokeWeightFunction,
        [this.data, this.dataSources, this.data.dsIndex], settings.strokeWeight),
      pmIgnore: true
    };
  }

  removePolyline() {
    this.map.removeLayer(this.leafletPoly);
  }

  getPolylineLatLngs() {
    return this.leafletPoly.getLatLngs();
  }
}
