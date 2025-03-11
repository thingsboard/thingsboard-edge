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
  CustomMapLayerSettings,
  defaultCustomMapLayerSettings,
  defaultGoogleMapLayerSettings,
  defaultHereMapLayerSettings,
  defaultLayerTitle,
  defaultOpenStreetMapLayerSettings,
  defaultTencentMapLayerSettings,
  GoogleMapLayerSettings,
  HereMapLayerSettings,
  MapLayerSettings,
  MapProvider,
  OpenStreetMapLayerSettings,
  TencentMapLayerSettings
} from '@shared/models/widget/maps/map.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { mergeDeep } from '@core/utils';
import { Observable, of, switchMap } from 'rxjs';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import L from 'leaflet';
import { catchError, map } from 'rxjs/operators';
import { ResourcesService } from '@core/services/resources.service';

export abstract class TbMapLayer<S extends MapLayerSettings> {

  static fromSettings(ctx: WidgetContext,
                      inputSettings: DeepPartial<MapLayerSettings>) {

    switch (inputSettings.provider) {
      case MapProvider.openstreet:
        return new TbOpenStreetMapLayer(ctx, inputSettings);
      case MapProvider.google:
        return new TbGoogleMapLayer(ctx, inputSettings);
      case MapProvider.tencent:
        return new TbTencentMapLayer(ctx, inputSettings);
      case MapProvider.here:
        return new TbHereMapLayer(ctx, inputSettings);
      case MapProvider.custom:
        return new TbCustomMapLayer(ctx, inputSettings);
    }
  }

  protected settings: S;

  protected constructor(protected ctx: WidgetContext,
                        protected inputSettings: DeepPartial<MapLayerSettings>) {
    this.settings = mergeDeep({} as S, this.defaultSettings(), this.inputSettings as S);
  }

  public loadLayer(theMap: L.Map): Observable<L.TB.LayerData> {
    return this.createLayer().pipe(
      switchMap((layer) => {
        if (layer) {
          return this.createLayer().pipe(
            map((mini) => {
              if (mini) {
                const attribution = layer.getAttribution();
                const attributionPrefix = attribution ? theMap.attributionControl.options.prefix as string : null;
                return {
                  title: this.title(),
                  attributionPrefix: attributionPrefix,
                  layer,
                  mini
                };
              } else {
                return null;
              }
            })
          );
        } else {
          return of(null);
        }
      })
    );
  }

  private title(): string {
    const customTranslate = this.ctx.$injector.get(CustomTranslatePipe);
    if (this.settings.label) {
      return customTranslate.transform(this.settings.label);
    } else {
      return this.generateTitle();
    }
  }

  private generateTitle(): string {
    const translationKey = defaultLayerTitle(this.settings);
    if (translationKey) {
      return this.ctx.translate.instant(translationKey);
    } else {
      return 'Unknown';
    }
  };

  protected abstract defaultSettings(): S;

  protected abstract createLayer(): Observable<L.Layer>;

}

class TbOpenStreetMapLayer extends TbMapLayer<OpenStreetMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): OpenStreetMapLayerSettings {
    return defaultOpenStreetMapLayerSettings;
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.tileLayer.provider(this.settings.layerType);
    return of(layer);
  }

}

class TbGoogleMapLayer extends TbMapLayer<GoogleMapLayerSettings> {

  static loadedApiKeysGlobal: {[key: string]: boolean} = {};

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): GoogleMapLayerSettings {
    return defaultGoogleMapLayerSettings;
  }

  protected createLayer(): Observable<L.Layer> {
    return this.loadGoogle().pipe(
      map((loaded) => {
        if (loaded) {
          return (L.gridLayer as any).googleMutant({
            type: this.settings.layerType
          });
        } else {
          return null;
        }
      })
    );
  }

  private loadGoogle(): Observable<boolean> {
    const apiKey = this.settings.apiKey || defaultGoogleMapLayerSettings.apiKey;
    if (TbGoogleMapLayer.loadedApiKeysGlobal[apiKey]) {
      return of(true);
    } else {
      const resourceService = this.ctx.$injector.get(ResourcesService);
      return resourceService.loadResource(`https://maps.googleapis.com/maps/api/js?key=${apiKey}&loading=async`).pipe(
        map(() => {
          TbGoogleMapLayer.loadedApiKeysGlobal[apiKey] = true;
          return true;
        }),
        catchError((e) => {
          TbGoogleMapLayer.loadedApiKeysGlobal[apiKey] = false;
          console.error(`Google map api load failed!`, e);
          return of(false);
        })
      );
    }
  }
}

class TbTencentMapLayer extends TbMapLayer<TencentMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): TencentMapLayerSettings {
    return defaultTencentMapLayerSettings;
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.TB.tileLayer.chinaProvider(this.settings.layerType, {
      attribution: '&copy;2024 Tencent - GS(2023)1171号'
    });
    return of(layer);
  }

}

class TbHereMapLayer extends TbMapLayer<HereMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): HereMapLayerSettings {
    return defaultHereMapLayerSettings;
  }

  protected createLayer(): Observable<L.Layer> {
    const apiKey = this.settings.apiKey || defaultHereMapLayerSettings.apiKey;
    const layer = L.tileLayer.provider(this.settings.layerType, {useV3: true, apiKey} as any);
    return of(layer);
  }

}

class TbCustomMapLayer extends TbMapLayer<CustomMapLayerSettings> {

  constructor(protected ctx: WidgetContext,
              protected inputSettings: DeepPartial<MapLayerSettings>) {
    super(ctx, inputSettings);
  }

  protected defaultSettings(): CustomMapLayerSettings {
    return defaultCustomMapLayerSettings;
  }

  protected createLayer(): Observable<L.Layer> {
    const layer = L.tileLayer(this.settings.tileUrl);
    return of(layer);
  }

}
