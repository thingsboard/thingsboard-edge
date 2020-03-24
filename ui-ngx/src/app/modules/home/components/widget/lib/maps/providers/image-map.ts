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

import L, { LatLngLiteral } from 'leaflet';
import LeafletMap from '../leaflet-map';
import { MapSettings, UnitedMapSettings } from '../map-models';
import { aspectCache } from '@app/core/utils';

const maxZoom = 4;// ?

export class ImageMap extends LeafletMap {

    imageOverlay;
    aspect = 0;
    width = 0;
    height = 0;

    constructor($container: HTMLElement, options: UnitedMapSettings) {
        super($container, options);
        aspectCache(options.mapUrl).subscribe(aspect => {
            this.aspect = aspect;
            this.onResize();
            super.setMap(this.map);
            super.initSettings(options);
        });
    }

    updateBounds(updateImage?, lastCenterPos?) {
        const w = this.width;
        const h = this.height;
        let southWest = this.pointToLatLng(0, h);
        let northEast = this.pointToLatLng(w, 0);
        const bounds = new L.LatLngBounds(southWest, northEast);

        if (updateImage && this.imageOverlay) {
            this.imageOverlay.remove();
            this.imageOverlay = null;
        }

        if (this.imageOverlay) {
            this.imageOverlay.setBounds(bounds);
        } else {
            this.imageOverlay = L.imageOverlay(this.options.mapUrl, bounds).addTo(this.map);

        }
        const padding = 200 * maxZoom;
        southWest = this.pointToLatLng(-padding, h + padding);
        northEast = this.pointToLatLng(w + padding, -padding);
        const maxBounds = new L.LatLngBounds(southWest, northEast);
        this.map.setMaxBounds(maxBounds);
        if (lastCenterPos) {
            lastCenterPos.x *= w;
            lastCenterPos.y *= h;
            /* this.ctx.$scope.$injector.get('$mdUtil').nextTick(() => {
                 this.map.panTo(center, { animate: false });
             });*/
        }
    }

    onResize(updateImage?) {
        let width = this.$container.clientWidth;
        if (width > 0 && this.aspect) {
            let height = width / this.aspect;
            const imageMapHeight = this.$container.clientHeight;
            if (imageMapHeight > 0 && height > imageMapHeight) {
                height = imageMapHeight;
                width = height * this.aspect;
            }
            width *= maxZoom;
            const prevWidth = this.width;
            const prevHeight = this.height;
            if (this.width !== width) {
                this.width = width;
                this.height = width / this.aspect;
                if (!this.map) {
                    this.initMap(updateImage);
                } else {
                    const lastCenterPos = this.latLngToPoint(this.map.getCenter());
                    lastCenterPos.x /= prevWidth;
                    lastCenterPos.y /= prevHeight;
                    this.updateBounds(updateImage, lastCenterPos);
                    this.map.invalidateSize(true);
                }

            }
        }
    }

    initMap(updateImage?) {
        if (!this.map && this.aspect > 0) {
            const center = this.pointToLatLng(this.width / 2, this.height / 2);
            this.map = L.map(this.$container, {
                minZoom: 1,
                maxZoom,
                scrollWheelZoom: !this.options.disableScrollZooming,
                center,
                zoom: 1,
                crs: L.CRS.Simple,
                attributionControl: false
            });
            this.updateBounds(updateImage);
        }
    }

    convertPosition(expression): L.LatLng {
        return this.pointToLatLng(
            expression[this.options.xPosKeyName] * this.width,
            expression[this.options.yPosKeyName] * this.height);
    }

    pointToLatLng(x, y): L.LatLng {
        return L.CRS.Simple.pointToLatLng({ x, y } as L.PointExpression, maxZoom - 1);
    }

    latLngToPoint(latLng: LatLngLiteral) {
        return L.CRS.Simple.latLngToPoint(latLng, maxZoom - 1);
    }

   /* convertToCustomFormat(position: L.LatLng): object {
        return {
            [this.options.xPosKeyName]: (position.lng + 180) / 360,
            [this.options.yPosKeyName]: (position.lat + 180) / 360
        }
    }*/
}