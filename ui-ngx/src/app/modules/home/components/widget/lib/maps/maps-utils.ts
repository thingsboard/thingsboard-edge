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
import {
  GenericFunction,
  ShowTooltipAction, WidgetToolipSettings
} from './map-models';
import { Datasource, FormattedData } from '@app/shared/models/widget.models';
import { fillDataPattern, processDataPattern, safeExecute } from '@core/utils';
import { parseWithTranslation } from '@home/components/widget/lib/maps/common-maps-utils';

export function createTooltip(target: L.Layer,
                              settings: Partial<WidgetToolipSettings>,
                              datasource: Datasource,
                              autoClose = false,
                              showTooltipAction = ShowTooltipAction.click,
                              content?: string | HTMLElement
): L.Popup {
    const popup = L.popup();
    popup.setContent(content);
    target.bindPopup(popup, { autoClose, closeOnClick: false });
    if (showTooltipAction === ShowTooltipAction.hover) {
        target.off('click');
        target.on('mouseover', () => {
            target.openPopup();
        });
        target.on('mousemove', (e) => {
            // @ts-ignore
            popup.setLatLng(e.latlng);
        });
        target.on('mouseout', () => {
            target.closePopup();
        });
    }
    target.on('popupopen', () => {
      bindPopupActions(popup, settings, datasource);
      (target as any)._popup._closeButton.addEventListener('click', (event: Event) => {
        event.preventDefault();
      });
    });
    return popup;
}

export function bindPopupActions(popup: L.Popup, settings: Partial<WidgetToolipSettings>,
                                 datasource: Datasource) {
  const actions = popup.getElement().getElementsByClassName('tb-custom-action');
  Array.from(actions).forEach(
    (element: HTMLElement) => {
      const actionName = element.getAttribute('data-action-name');
      if (element && settings.tooltipAction[actionName]) {
        element.onclick = ($event) =>
        {
          settings.tooltipAction[actionName]($event, datasource);
          return false;
        };
      }
    });
}

export function isCutPolygon(data): boolean {
  if (data.length > 1 && Array.isArray(data[0]) && (Array.isArray(data[0][0]) || data[0][0] instanceof L.LatLng)) {
    return true;
  }
  return false;
}

export function isJSON(data: string): boolean {
  try {
    const parseData = JSON.parse(data);
    return !Array.isArray(parseData);
  } catch (e) {
    return false;
  }
}

interface labelSettings {
  showLabel: boolean;
  useLabelFunction: boolean;
  parsedLabelFunction: GenericFunction;
  label: string;
}

export function entitiesParseName(entities: FormattedData[], labelSettings: labelSettings):  FormattedData[] {
  const div = document.createElement('div');
  for (const entity of entities) {
    if (labelSettings?.showLabel) {
      const pattern = labelSettings.useLabelFunction ? safeExecute(labelSettings.parsedLabelFunction,
        [entity, entities, entity.dsIndex]) : labelSettings.label;
      const markerLabelText = parseWithTranslation.prepareProcessPattern(pattern, true);
      const replaceInfoLabelMarker = processDataPattern(pattern, entity);
      div.innerHTML = fillDataPattern(markerLabelText, replaceInfoLabelMarker, entity);
      entity.entityParseName = div.textContent || div.innerText || '';
    } else {
      entity.entityParseName = entity.entityName;
    }
  }
  return entities;
}
