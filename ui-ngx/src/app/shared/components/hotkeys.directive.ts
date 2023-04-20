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

import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { Hotkey } from 'angular2-hotkeys';
import { MousetrapInstance } from 'mousetrap';
import * as Mousetrap from 'mousetrap';
import { TbCheatSheetComponent } from '@shared/components/cheatsheet.component';

@Directive({
  selector : '[tb-hotkeys]'
})
export class TbHotkeysDirective implements OnInit, OnDestroy {
  @Input() hotkeys: Hotkey[] = [];
  @Input() cheatSheet: TbCheatSheetComponent;

  private mousetrap: MousetrapInstance;
  private hotkeysList: Hotkey[] = [];

  private preventIn = ['INPUT', 'SELECT', 'TEXTAREA'];

  constructor(private elementRef: ElementRef) {
    this.mousetrap = new Mousetrap(this.elementRef.nativeElement);
    (this.elementRef.nativeElement as HTMLElement).tabIndex = -1;
    (this.elementRef.nativeElement as HTMLElement).style.outline = '0';
  }

  ngOnInit() {
    for (const hotkey of this.hotkeys) {
      this.hotkeysList.push(hotkey);
      this.bindEvent(hotkey);
    }
    if (this.cheatSheet) {
      const hotkeyObj: Hotkey = new Hotkey(
        '?',
        (event: KeyboardEvent) => {
          this.cheatSheet.toggleCheatSheet();
          return false;
        },
        [],
        'Show / hide this help menu',
      );
      this.hotkeysList.unshift(hotkeyObj);
      this.bindEvent(hotkeyObj);
      this.cheatSheet.setHotKeys(this.hotkeysList);
    }
  }

  private bindEvent(hotkey: Hotkey): void {
    this.mousetrap.bind((hotkey as Hotkey).combo, (event: KeyboardEvent, combo: string) => {
      let shouldExecute = true;
      if (event) {
        const target: HTMLElement = (event.target || event.srcElement) as HTMLElement;
        const nodeName: string = target.nodeName.toUpperCase();
        if ((' ' + target.className + ' ').indexOf(' mousetrap ') > -1) {
          shouldExecute = true;
        } else if (this.preventIn.indexOf(nodeName) > -1 && (hotkey as Hotkey).
                   allowIn.map(allow => allow.toUpperCase()).indexOf(nodeName) === -1) {
          shouldExecute = false;
        }
      }

      if (shouldExecute) {
        return (hotkey as Hotkey).callback.apply(this, [event, combo]);
      }
    });
  }

  ngOnDestroy() {
    for (const hotkey of this.hotkeysList) {
      this.mousetrap.unbind(hotkey.combo);
    }
  }

}
