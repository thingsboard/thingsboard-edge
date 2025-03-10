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
  DataLayerColorSettings, DataLayerColorType,
  DataLayerPatternSettings, DataLayerPatternType,
  MapDataLayerSettings, MapDataLayerType, mapDataSourceSettingsToDatasource,
  MapStringFunction, MapType,
  TbMapDatasource
} from '@shared/models/widget/maps/map.models';
import {
  createLabelFromPattern,
  guid, isDefined,
  mergeDeepIgnoreArray,
  parseTbFunction,
  safeExecuteTbFunction
} from '@core/utils';
import L from 'leaflet';
import { CompiledTbFunction } from '@shared/models/js-function.models';
import { forkJoin, Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { FormattedData } from '@shared/models/widget.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { WidgetContext } from '@home/models/widget-component.models';

export class DataLayerPatternProcessor {

  private patternFunction: CompiledTbFunction<MapStringFunction>;
  private pattern: string;

  constructor(private dataLayer: TbMapDataLayer,
              private settings: DataLayerPatternSettings) {}

  public setup(): Observable<void> {
    if (this.settings.type === DataLayerPatternType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.patternFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.patternFunction = parsed;
          return null;
        })
      );
    } else {
      this.pattern = this.settings.pattern;
      return of(null)
    }
  }

  public processPattern(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): string {
    let pattern: string;
    if (this.settings.type === DataLayerPatternType.function) {
      pattern = safeExecuteTbFunction(this.patternFunction, [data, dsData]);
    } else {
      pattern = this.pattern;
    }
    const text = createLabelFromPattern(pattern, data);
    const customTranslate = this.dataLayer.getCtx().$injector.get(CustomTranslatePipe);
    return customTranslate.transform(text);
  }

}

export class DataLayerColorProcessor {

  private colorFunction: CompiledTbFunction<MapStringFunction>;
  private color: string;

  constructor(private dataLayer: TbMapDataLayer,
              private settings: DataLayerColorSettings) {}

  public setup(): Observable<void> {
    this.color = this.settings.color;
    if (this.settings.type === DataLayerColorType.function) {
      return parseTbFunction<MapStringFunction>(this.dataLayer.getCtx().http, this.settings.colorFunction, ['data', 'dsData']).pipe(
        map((parsed) => {
          this.colorFunction = parsed;
          return null;
        })
      );
    } else {
      return of(null)
    }
  }

  public processColor(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): string {
    let color: string;
    if (this.settings.type === DataLayerColorType.function) {
      color = safeExecuteTbFunction(this.colorFunction, [data, dsData]);
      if (!color) {
        color = this.color;
      }
    } else {
      color = this.color;
    }
    return color;
  }

}

export abstract class TbDataLayerItem<S extends MapDataLayerSettings = MapDataLayerSettings, D extends TbMapDataLayer = TbMapDataLayer, L extends L.Layer = L.Layer> {

  protected layer: L;

  protected constructor(protected settings: S,
                        protected dataLayer: D) {}

  public getLayer(): L {
    return this.layer;
  }

  public getDataLayer(): D {
    return this.dataLayer;
  }

  public abstract remove(): void;

  public abstract invalidateCoordinates(): void;

}

export abstract class TbMapDataLayer<S extends MapDataLayerSettings = MapDataLayerSettings, I extends TbDataLayerItem = any> {

  protected settings: S;

  protected datasource: TbMapDatasource;

  protected mapDataId = guid();

  protected dataLayerContainer: L.FeatureGroup;

  protected layerItems = new Map<string, I>();

  protected groupsState: {[group: string]: boolean} = {};

  protected enabled = true;

  protected snappable = false;

  public dataLayerLabelProcessor: DataLayerPatternProcessor;
  public dataLayerTooltipProcessor: DataLayerPatternProcessor;

  protected constructor(protected map: TbMap<any>,
                        inputSettings: S) {
    this.settings = mergeDeepIgnoreArray({} as S, this.defaultBaseSettings(map) as S, inputSettings);
    if (this.settings.groups?.length) {
      this.settings.groups.forEach((group) => {
        this.groupsState[group] = true;
      });
    }
    this.dataLayerContainer = this.createDataLayerContainer();
    this.dataLayerLabelProcessor = this.settings.label.show ? new DataLayerPatternProcessor(this, this.settings.label) : null;
    this.dataLayerTooltipProcessor = this.settings.tooltip.show ? new DataLayerPatternProcessor(this, this.settings.tooltip): null;
    this.map.getMap().addLayer(this.dataLayerContainer);
  }

  public setup(): Observable<any> {
    this.datasource = mapDataSourceSettingsToDatasource(this.settings);
    this.datasource.dataKeys = this.settings.additionalDataKeys ? [...this.settings.additionalDataKeys] : [];
    this.mapDataId = this.datasource.mapDataIds[0];
    this.datasource = this.setupDatasource(this.datasource);
    return forkJoin(
      [
        this.dataLayerLabelProcessor ? this.dataLayerLabelProcessor.setup() : of(null),
        this.dataLayerTooltipProcessor ? this.dataLayerTooltipProcessor.setup() : of(null),
        this.doSetup()
      ]);
  }

  public removeItem(key: string): void {
    const item = this.layerItems.get(key);
    if (item) {
      item.remove();
      this.layerItems.delete(key);
    }
  }

  public invalidateCoordinates(): void {
    this.layerItems.forEach(item => item.invalidateCoordinates());
  }

  public getCtx(): WidgetContext {
    return this.map.getCtx();
  }

  public getMap(): TbMap<any> {
    return this.map;
  }

  public mapType(): MapType {
    return this.map.type();
  }

  public getDatasource(): TbMapDatasource {
    return this.datasource;
  }

  public getDataLayerContainer(): L.FeatureGroup {
    return this.dataLayerContainer;
  }

  public getBounds(): L.LatLngBounds {
    return this.dataLayerContainer.getBounds();
  }

  public isEnabled(): boolean {
    return this.enabled;
  }

  public getGroups(): string[] {
    return this.settings.groups || [];
  }

  public toggleGroup(group: string): boolean {
    if (isDefined(this.groupsState[group])) {
      this.groupsState[group] = !this.groupsState[group];
      const enabled = Object.values(this.groupsState).some(v => v);
      if (this.enabled !== enabled) {
        this.enabled = enabled;
        if (this.enabled) {
          this.map.getMap().addLayer(this.dataLayerContainer);
          this.onDataLayerEnabled();
        } else {
          this.onDataLayerDisabled();
          this.map.getMap().removeLayer(this.dataLayerContainer);
        }
        this.map.enabledDataLayersUpdated();
        return true;
      }
    }
    return false;
  }

  protected createDataLayerContainer(): L.FeatureGroup {
    return L.featureGroup([], {snapIgnore: true});
  }

  protected setupDatasource(datasource: TbMapDatasource): TbMapDatasource {
    return datasource;
  }

  protected onDataLayerEnabled(): void {}

  protected onDataLayerDisabled(): void {}

  public abstract dataLayerType(): MapDataLayerType;

  protected abstract defaultBaseSettings(map: TbMap<any>): Partial<S>;

  protected abstract doSetup(): Observable<any>;

}

