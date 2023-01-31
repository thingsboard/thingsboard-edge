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

import { Component, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { Hotkey, HotkeysService } from 'angular2-hotkeys';
import { MousetrapInstance } from 'mousetrap';
import * as Mousetrap from 'mousetrap';

@Component({
  selector : 'tb-hotkeys-cheatsheet',
  styles : [`
.tb-hotkeys-container {
  display: table !important;
  position: fixed;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  color: #333;
  font-size: 1em;
  background-color: rgba(255,255,255,0.9);
  outline: 0;
}
.tb-hotkeys-container.fade {
  z-index: -1024;
  visibility: hidden;
  opacity: 0;
  -webkit-transition: opacity 0.15s linear;
  -moz-transition: opacity 0.15s linear;
  -o-transition: opacity 0.15s linear;
  transition: opacity 0.15s linear;
}
.tb-hotkeys-container.fade.in {
  z-index: 10002;
  visibility: visible;
  opacity: 1;
}
.tb-hotkeys-title {
  font-weight: bold;
  text-align: center;
  font-size: 1.2em;
}
.tb-hotkeys {
  width: 100%;
  height: 100%;
  display: table-cell;
  vertical-align: middle;
}
.tb-hotkeys table {
  margin: auto;
  color: #333;
}
.tb-content {
  display: table-cell;
  vertical-align: middle;
}
.tb-hotkeys-keys {
  padding: 5px;
  text-align: right;
}
.tb-hotkeys-key {
  display: inline-block;
  color: #fff;
  background-color: #333;
  border: 1px solid #333;
  border-radius: 5px;
  text-align: center;
  margin-right: 5px;
  box-shadow: inset 0 1px 0 #666, 0 1px 0 #bbb;
  padding: 5px 9px;
  font-size: 1em;
}
.tb-hotkeys-text {
  padding-left: 10px;
  font-size: 1em;
}
.tb-hotkeys-close {
  position: fixed;
  top: 20px;
  right: 20px;
  font-size: 2em;
  font-weight: bold;
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  min-height: 45px;
  min-width: 45px;
  text-align: center;
}
.tb-hotkeys-close:hover {
  background-color: #fff;
  cursor: pointer;
}
@media all and (max-width: 500px) {
  .tb-hotkeys {
    font-size: 0.8em;
  }
}
@media all and (min-width: 750px) {
  .tb-hotkeys {
    font-size: 1.2em;
  }
}  `],
  template : `<div tabindex="-1" class="tb-hotkeys-container fade" [ngClass]="{'in': helpVisible}" style="display:none"><div class="tb-hotkeys">
  <h4 class="tb-hotkeys-title">{{ title }}</h4>
  <table *ngIf="helpVisible"><tbody>
    <tr *ngFor="let hotkey of hotkeysList">
      <td class="tb-hotkeys-keys">
        <span *ngFor="let key of hotkey.formatted" class="tb-hotkeys-key">{{ key }}</span>
      </td>
      <td class="tb-hotkeys-text">{{ hotkey.description }}</td>
    </tr>
  </tbody></table>
  <div class="tb-hotkeys-close" (click)="toggleCheatSheet()">&#215;</div>
</div></div>`,
})
export class TbCheatSheetComponent implements OnInit, OnDestroy {

  helpVisible = false;
  @Input() title = 'Keyboard Shortcuts:';

  @Input()
  hotkeys: Hotkey[];

  hotkeysList: Hotkey[];

  private mousetrap: MousetrapInstance;

  constructor(private elementRef: ElementRef,
              private hotkeysService: HotkeysService) {
    this.mousetrap = new Mousetrap(this.elementRef.nativeElement);
    this.mousetrap.bind('?', (event: KeyboardEvent, combo: string) => {
      this.toggleCheatSheet();
    });
  }

  public ngOnInit(): void {
    if (this.hotkeys) {
      this.hotkeysList = this.hotkeys.filter(hotkey => hotkey.description);
    }
  }

  public setHotKeys(hotkeys: Hotkey[]) {
    this.hotkeysList = hotkeys.filter(hotkey => hotkey.description);
  }

  public toggleCheatSheet(): void {
    this.helpVisible = !this.helpVisible;
  }

  ngOnDestroy() {
    this.mousetrap.unbind('?');
  }
}
