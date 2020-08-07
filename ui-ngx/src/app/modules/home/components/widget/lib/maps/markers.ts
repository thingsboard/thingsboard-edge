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

import L, { LeafletMouseEvent } from 'leaflet';
import { FormattedData, MarkerSettings } from './map-models';
import { aspectCache, bindPopupActions, createTooltip, parseWithTranslation, safeExecute } from './maps-utils';
import tinycolor from 'tinycolor2';
import { isDefined } from '@core/utils';
import LeafletMap from './leaflet-map';

export class Marker {
    leafletMarker: L.Marker;
    tooltipOffset: L.LatLngTuple;
    markerOffset: L.LatLngTuple;
    tooltip: L.Popup;
    location: L.LatLngExpression;
    data: FormattedData;
    dataSources: FormattedData[];

  constructor(private map: LeafletMap, location: L.LatLngExpression, public settings: MarkerSettings,
              data?: FormattedData, dataSources?, onDragendListener?) {
        this.setDataSources(data, dataSources);
        this.leafletMarker = L.marker(location, {
            draggable: settings.draggableMarker
        });

        this.markerOffset = [
          isDefined(settings.markerOffsetX) ? settings.markerOffsetX : 0.5,
          isDefined(settings.markerOffsetY) ? settings.markerOffsetY : 1,
        ];

        this.createMarkerIcon((iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
            this.tooltipOffset = [0, -iconInfo.size[1] * this.markerOffset[1] + 10];
            this.updateMarkerLabel(settings);
        });

        if (settings.showTooltip) {
            this.tooltip = createTooltip(this.leafletMarker, settings, data.$datasource);
            this.updateMarkerTooltip(data);
        }

        if (this.settings.markerClick) {
            this.leafletMarker.on('click', (event: LeafletMouseEvent) => {
                for (const action in this.settings.markerClick) {
                    if (typeof (this.settings.markerClick[action]) === 'function') {
                        this.settings.markerClick[action](event.originalEvent, this.data.$datasource);
                    }
                }
            });
        }

        if (onDragendListener) {
            this.leafletMarker.on('dragend', (e) => onDragendListener(e, this.data));
        }
    }

    setDataSources(data: FormattedData, dataSources: FormattedData[]) {
        this.data = data;
        this.dataSources = dataSources;
    }

    updateMarkerTooltip(data: FormattedData) {
        const pattern = this.settings.useTooltipFunction ?
            safeExecute(this.settings.tooltipFunction, [this.data, this.dataSources, this.data.dsIndex]) : this.settings.tooltipPattern;
        this.tooltip.setContent(parseWithTranslation.parseTemplate(pattern, data, true));
      if (this.tooltip.isOpen() && this.tooltip.getElement()) {
        bindPopupActions(this.tooltip, this.settings, data.$datasource);
      }
    }

    updateMarkerPosition(position: L.LatLngExpression) {
        this.leafletMarker.setLatLng(position);
    }

    updateMarkerLabel(settings: MarkerSettings) {
        this.leafletMarker.unbindTooltip();
        if (settings.showLabel) {
            const pattern = settings.useLabelFunction ?
                safeExecute(settings.labelFunction, [this.data, this.dataSources, this.data.dsIndex]) : settings.label;
            settings.labelText = parseWithTranslation.parseTemplate(pattern, this.data, true);
            this.leafletMarker.bindTooltip(`<div style="color: ${settings.labelColor};"><b>${settings.labelText}</b></div>`,
                { className: 'tb-marker-label', permanent: true, direction: 'top', offset: this.tooltipOffset });
        }
    }

    updateMarkerColor(color) {
        this.createDefaultMarkerIcon(color, (iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
        });
    }

    updateMarkerIcon(settings: MarkerSettings) {
        this.createMarkerIcon((iconInfo) => {
            this.leafletMarker.setIcon(iconInfo.icon);
            this.tooltipOffset = [0, -iconInfo.size[1] * this.markerOffset[1] + 10];
            this.updateMarkerLabel(settings);
        });
    }

    createMarkerIcon(onMarkerIconReady) {
        if (this.settings.icon) {
            onMarkerIconReady({
                size: [30, 30],
                icon: this.settings.icon,
            });
            return;
        }
        const currentImage = this.settings.useMarkerImageFunction ?
            safeExecute(this.settings.markerImageFunction,
                [this.data, this.settings.markerImages, this.dataSources, this.data.dsIndex]) : this.settings.currentImage;
        const currentColor = tinycolor(this.settings.useColorFunction ? safeExecute(this.settings.colorFunction,
            [this.data, this.dataSources, this.data.dsIndex]) : this.settings.color).toHex();
        if (currentImage && currentImage.url) {
            aspectCache(currentImage.url).subscribe(
                (aspect) => {
                    if (aspect) {
                        let width;
                        let height;
                        if (aspect > 1) {
                            width = currentImage.size;
                            height = currentImage.size / aspect;
                        } else {
                            width = currentImage.size * aspect;
                            height = currentImage.size;
                        }
                        const icon = L.icon({
                            iconUrl: currentImage.url,
                            iconSize: [width, height],
                            iconAnchor: [width * this.markerOffset[0], height * this.markerOffset[1]],
                            popupAnchor: [0, -height]
                        });
                        const iconInfo = {
                            size: [width, height],
                            icon
                        };
                        onMarkerIconReady(iconInfo);
                    } else {
                        this.createDefaultMarkerIcon(currentColor, onMarkerIconReady);
                    }
                }
            );
        } else {
            this.createDefaultMarkerIcon(currentColor, onMarkerIconReady);
        }
    }

    createDefaultMarkerIcon(color, onMarkerIconReady) {
      if (!this.map.defaultMarkerIconInfo) {
        const icon = L.icon({
          iconUrl: 'https://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|' + color,
          iconSize: [21, 34],
          iconAnchor: [21 * this.markerOffset[0], 34 * this.markerOffset[1]],
          popupAnchor: [0, -34],
          shadowUrl: 'https://chart.apis.google.com/chart?chst=d_map_pin_shadow',
          shadowSize: [40, 37],
          shadowAnchor: [12, 35]
        });
        this.map.defaultMarkerIconInfo = {
          size: [21, 34],
          icon
        };
      }
      onMarkerIconReady(this.map.defaultMarkerIconInfo);
    }

    removeMarker() {
        /*     this.map$.subscribe(map =>
                 this.leafletMarker.addTo(map))*/
    }

    extendBoundsWithMarker(bounds) {
        bounds.extend(this.leafletMarker.getLatLng());
    }

    getMarkerPosition() {
        return this.leafletMarker.getLatLng();
    }

    setMarkerPosition(latLng) {
        this.leafletMarker.setLatLng(latLng);
    }
}
