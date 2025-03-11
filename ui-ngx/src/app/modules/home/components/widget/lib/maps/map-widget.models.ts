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

import { defaultMapSettings, MapSetting, MapType } from '@shared/models/widget/maps/map.models';
import { BackgroundSettings, BackgroundType } from '@shared/models/widget-settings.models';
import { mergeDeep } from '@core/utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { DeepPartial } from '@shared/models/common';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { TbGeoMap } from '@home/components/widget/lib/maps/geo-map';
import { TbImageMap } from '@home/components/widget/lib/maps/image-map';

export interface MapWidgetSettings extends MapSetting {
  background: BackgroundSettings;
  padding: string;
}

export const mapWidgetDefaultSettings: MapWidgetSettings =
  mergeDeep({} as MapWidgetSettings, defaultMapSettings as MapWidgetSettings, {
    background: {
      type: BackgroundType.color,
      color: '#fff',
      overlay: {
        enabled: false,
        color: 'rgba(255,255,255,0.72)',
        blur: 3
      }
    },
    padding: '8px'
} as MapWidgetSettings);

export const createMap = (ctx: WidgetContext,
                          inputSettings: DeepPartial<MapSetting>,
                          mapElement: HTMLElement): TbMap<MapSetting> => {
  switch (inputSettings.mapType) {
    case MapType.geoMap:
      return new TbGeoMap(ctx, inputSettings, mapElement);
    case MapType.image:
      return new TbImageMap(ctx, inputSettings, mapElement);
  }
}
