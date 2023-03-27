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

import { animate, AnimationTriggerMetadata, style, transition, trigger } from '@angular/animations';
import { ConnectedOverlayPositionChange, ConnectionPositionPair } from '@angular/cdk/overlay';
import { TbPopoverComponent } from '@shared/components/popover.component';

export const popoverMotion: AnimationTriggerMetadata = trigger('popoverMotion', [
  transition('void => active', [
    style({ opacity: 0, transform: 'scale(0.8)' }),
    animate(
      '0.2s cubic-bezier(0.08, 0.82, 0.17, 1)',
      style({
        opacity: 1,
        transform: 'scale(1)'
      })
    )
  ]),
  transition('active => void', [
    style({ opacity: 1, transform: 'scale(1)' }),
    animate(
      '0.2s cubic-bezier(0.78, 0.14, 0.15, 0.86)',
      style({
        opacity: 0,
        transform: 'scale(0.8)'
      })
    )
  ])
]);

export const PopoverPlacements = ['top', 'topLeft', 'topRight', 'right', 'rightTop', 'rightBottom', 'bottom', 'bottomLeft', 'bottomRight', 'left', 'leftTop', 'leftBottom'] as const;
type PopoverPlacementTuple = typeof PopoverPlacements;
export type PopoverPlacement = PopoverPlacementTuple[number];

export const POSITION_MAP: { [key: string]: ConnectionPositionPair } = {
  top: new ConnectionPositionPair({ originX: 'center', originY: 'top' }, { overlayX: 'center', overlayY: 'bottom' }),
  topLeft: new ConnectionPositionPair({ originX: 'start', originY: 'top' }, { overlayX: 'start', overlayY: 'bottom' }),
  topRight: new ConnectionPositionPair({ originX: 'end', originY: 'top' }, { overlayX: 'end', overlayY: 'bottom' }),
  right: new ConnectionPositionPair({ originX: 'end', originY: 'center' }, { overlayX: 'start', overlayY: 'center' }),
  rightTop: new ConnectionPositionPair({ originX: 'end', originY: 'top' }, { overlayX: 'start', overlayY: 'top' }),
  rightBottom: new ConnectionPositionPair(
    { originX: 'end', originY: 'bottom' },
    { overlayX: 'start', overlayY: 'bottom' }
  ),
  bottom: new ConnectionPositionPair({ originX: 'center', originY: 'bottom' }, { overlayX: 'center', overlayY: 'top' }),
  bottomLeft: new ConnectionPositionPair(
    { originX: 'start', originY: 'bottom' },
    { overlayX: 'start', overlayY: 'top' }
  ),
  bottomRight: new ConnectionPositionPair({ originX: 'end', originY: 'bottom' }, { overlayX: 'end', overlayY: 'top' }),
  left: new ConnectionPositionPair({ originX: 'start', originY: 'center' }, { overlayX: 'end', overlayY: 'center' }),
  leftTop: new ConnectionPositionPair({ originX: 'start', originY: 'top' }, { overlayX: 'end', overlayY: 'top' }),
  leftBottom: new ConnectionPositionPair(
    { originX: 'start', originY: 'bottom' },
    { overlayX: 'end', overlayY: 'bottom' }
  )
};

export const DEFAULT_POPOVER_POSITIONS = [POSITION_MAP.top, POSITION_MAP.right, POSITION_MAP.bottom, POSITION_MAP.left];

export function getPlacementName(position: ConnectedOverlayPositionChange): PopoverPlacement | undefined {
  for (const placement in POSITION_MAP) {
    if (
      position.connectionPair.originX === POSITION_MAP[placement].originX &&
      position.connectionPair.originY === POSITION_MAP[placement].originY &&
      position.connectionPair.overlayX === POSITION_MAP[placement].overlayX &&
      position.connectionPair.overlayY === POSITION_MAP[placement].overlayY
    ) {
      return placement as PopoverPlacement;
    }
  }
  return undefined;
}

export interface PropertyMapping {
  [key: string]: [string, () => unknown];
}

export interface PopoverWithTrigger {
  trigger: Element;
  popoverComponent: TbPopoverComponent;
}
