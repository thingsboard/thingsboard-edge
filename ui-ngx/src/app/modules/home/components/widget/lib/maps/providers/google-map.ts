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


import L from 'leaflet';
import LeafletMap from '../leaflet-map';
import { DEFAULT_ZOOM_LEVEL, WidgetUnitedMapSettings } from '../map-models';
import 'leaflet.gridlayer.googlemutant';
import { ResourcesService } from '@core/services/resources.service';
import { WidgetContext } from '@home/models/widget-component.models';

const gmGlobals: GmGlobal = {};

interface GmGlobal {
  [key: string]: boolean;
}

export class GoogleMap extends LeafletMap {
  private resource: ResourcesService;

  constructor(ctx: WidgetContext, $container, options: WidgetUnitedMapSettings) {
    super(ctx, $container, options);
    let mapUuid: string;
    if (this.ctx.reportService.reportView) {
      mapUuid = this.ctx.reportService.onWaitForMap();
    }
    this.resource = ctx.$injector.get(ResourcesService);
    this.loadGoogle(() => {
      const map = L.map($container, {
        attributionControl: false,
        doubleClickZoom: !this.options.disableDoubleClickZooming,
        zoomControl: !this.options.disableZoomControl,
        fadeAnimation: !ctx.reportService.reportView
      }).setView(options?.parsedDefaultCenterPosition, options?.defaultZoomLevel || DEFAULT_ZOOM_LEVEL);
      const tileLayer = (L.gridLayer as any).googleMutant({
        type: options?.gmDefaultMapType || 'roadmap'
      });
      tileLayer.addTo(map);
      if (this.ctx.reportService.reportView) {
        tileLayer.once('load', () => {
          this.ctx.reportService.onMapLoaded(mapUuid);
        });
      }
      super.setMap(map);
    }, options.gmApiKey);
  }

  private loadGoogle(callback, apiKey = 'AIzaSyDoEx2kaGz3PxwbI9T7ccTSg5xjdw8Nw8Q') {
    if (gmGlobals[apiKey]) {
      callback();
    } else {
      this.resource.loadResource(`https://maps.googleapis.com/maps/api/js?key=${apiKey}`).subscribe(
        () => {
          gmGlobals[apiKey] = true;
          callback();
        },
        (error) => {
          gmGlobals[apiKey] = false;
          console.error(`Google map api load failed!`, error);
        }
      );
    }
  }
}
