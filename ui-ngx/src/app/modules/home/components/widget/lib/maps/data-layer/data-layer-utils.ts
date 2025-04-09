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
  DataLayerTooltipSettings,
  DataLayerTooltipTrigger, processTooltipTemplate,
  TbMapDatasource
} from '@shared/models/widget/maps/map.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { FormattedData } from '@shared/models/widget.models';
import L from 'leaflet';
import { DataLayerPatternProcessor } from '@home/components/widget/lib/maps/data-layer/map-data-layer';

export const createTooltip = (map: TbMap<any>,
                              layer: L.Layer,
                              settings: DataLayerTooltipSettings,
                              data: FormattedData<TbMapDatasource>,
                              canOpen: () => boolean): L.Popup => {
  const tooltip = L.popup({autoClose: settings.autoclose, closeOnClick: false});
  (tooltip as any)._source = layer;
  layer.on('move', (e) => {
    tooltip.setLatLng((e as any).latlng);
  });
  layer.on('remove', () => {
    tooltip.close();
  });
  if (settings.trigger === DataLayerTooltipTrigger.click) {
    layer.on('click', (e) => {
      L.DomEvent.stop(e);
      if (tooltip.isOpen()) {
        tooltip.close();
      } else if (canOpen()) {
        if ((tooltip as any)._prepareOpen((layer as any)._latlng)) {
          tooltip.openOn(map.getMap());
        }
      }
    });
  } else if (settings.trigger === DataLayerTooltipTrigger.hover) {
    layer.on('mouseover', () => {
      if (canOpen()) {
        if ((tooltip as any)._prepareOpen((layer as any)._latlng)) {
          tooltip.openOn(map.getMap());
        }
      }
    });
    layer.on('mouseout', () => {
      tooltip.close();
    });
  }
  layer.on('popupopen', () => {
    bindTooltipActions(map, tooltip, settings, data);
  });
  return tooltip;
}

export const updateTooltip = (map: TbMap<any>,
                              tooltip: L.Popup,
                              settings: DataLayerTooltipSettings,
                              processor: DataLayerPatternProcessor,
                              data: FormattedData<TbMapDatasource>,
                              dsData: FormattedData<TbMapDatasource>[]): void => {
  let tooltipTemplate = processor.processPattern(data, dsData);
  tooltipTemplate = processTooltipTemplate(tooltipTemplate);
  tooltip.setContent(tooltipTemplate);
  if (tooltip.isOpen() && tooltip.getElement()) {
    bindTooltipActions(map, tooltip, settings, data);
  }
}

const bindTooltipActions = (map: TbMap<any>, tooltip: L.Popup, settings: DataLayerTooltipSettings, data: FormattedData<TbMapDatasource>): void => {
  const actions = tooltip.getElement().getElementsByClassName('tb-custom-action');
  Array.from(actions).forEach(
    (element: HTMLElement) => {
      const actionName = element.getAttribute('data-action-name');
      if (settings?.tagActions) {
        const action = settings.tagActions.find(action => action.name === actionName);
        if (action) {
          element.onclick = ($event) =>
          {
            map.dataItemClick($event, action, data);
            return false;
          };
        }
      }
    }
  );
}
