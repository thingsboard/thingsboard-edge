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

import { DataLayerColorSettings, ShapeDataLayerSettings, TbMapDatasource } from '@shared/models/widget/maps/map.models';
import L from 'leaflet';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { forkJoin, Observable } from 'rxjs';
import { FormattedData } from '@shared/models/widget.models';
import { TbLatestMapDataLayer } from '@home/components/widget/lib/maps/data-layer/latest-map-data-layer';
import { DataLayerColorProcessor } from './map-data-layer';

export abstract class TbShapesDataLayer<S extends ShapeDataLayerSettings, L extends TbLatestMapDataLayer<S,L>> extends TbLatestMapDataLayer<S, L> {

  public fillColorProcessor: DataLayerColorProcessor;
  public strokeColorProcessor: DataLayerColorProcessor;

  protected constructor(protected map: TbMap<any>,
                        inputSettings: S) {
    super(map, inputSettings);
  }

  public getShapeStyle(data: FormattedData<TbMapDatasource>, dsData: FormattedData<TbMapDatasource>[]): L.PathOptions {
    const fill = this.fillColorProcessor.processColor(data, dsData);
    const stroke = this.strokeColorProcessor.processColor(data, dsData);
    return {
      fill: true,
      fillColor: fill,
      color: stroke,
      weight: this.settings.strokeWeight,
      fillOpacity: 1,
      opacity: 1
    };
  }

  protected allColorSettings(): DataLayerColorSettings[] {
    return [this.settings.fillColor, this.settings.strokeColor];
  }

  protected doSetup(): Observable<any> {
    this.fillColorProcessor = new DataLayerColorProcessor(this, this.settings.fillColor);
    this.strokeColorProcessor = new DataLayerColorProcessor(this, this.settings.strokeColor);
    return forkJoin([this.fillColorProcessor.setup(), this.strokeColorProcessor.setup()]);
  }

}
