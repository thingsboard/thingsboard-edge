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

import Timeout = NodeJS.Timeout;

export interface TbContextMenuEvent extends Event {
  clientX: number;
  clientY: number;
  pageX: number;
  pageY: number;
  ctrlKey: boolean;
  metaKey: boolean;
}

const isIOSDevice = (): boolean =>
  /iPhone|iPad|iPod/i.test(navigator.userAgent) || (navigator.userAgent.includes('Mac') && 'ontouchend' in document);

export const initCustomJQueryEvents = () => {
  $.event.special.tbcontextmenu = {
    setup(this: HTMLElement) {
      const el = $(this);
      if (isIOSDevice()) {
        let timeoutId: Timeout;

        el.on('touchstart', (e) => {
          e.stopPropagation();
          timeoutId = setTimeout(() => {
            timeoutId = null;
            const touch = e.originalEvent.changedTouches[0];
            const event = $.Event('tbcontextmenu', {
              clientX: touch.clientX,
              clientY: touch.clientY,
              pageX: touch.pageX,
              pageY: touch.pageY,
              ctrlKey: false,
              metaKey: false,
              originalEvent: e
            });
            el.trigger(event, e);
          }, 500);
        });

        el.on('touchend touchmove', () => {
          if (timeoutId) {
            clearTimeout(timeoutId);
          }
        });
      } else {
        el.on('contextmenu', (e) => {
          const event = $.Event('tbcontextmenu', {
            clientX: e.originalEvent.clientX,
            clientY: e.originalEvent.clientY,
            pageX: e.originalEvent.pageX,
            pageY: e.originalEvent.pageY,
            ctrlKey: e.originalEvent.ctrlKey,
            metaKey: e.originalEvent.metaKey,
            originalEvent: e
          });
          el.trigger(event, e);
        });
      }
    },
    teardown(this: HTMLElement) {
      const el = $(this);
      if (isIOSDevice()) {
        el.off('touchstart touchend touchmove');
      } else {
        el.off('contextmenu');
      }
    }
  };
};
