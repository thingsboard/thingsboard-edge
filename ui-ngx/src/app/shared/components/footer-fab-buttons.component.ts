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

import { Component, HostListener, Input } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { speedDialFabAnimations } from '@shared/animations/speed-dial-fab.animations';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

export interface FooterFabButton {
  name: string;
  icon: string;
  onAction: ($event: Event) => void;
}

export interface FooterFabButtons {
  fabTogglerName: string;
  fabTogglerIcon: string;
  buttons: Array<FooterFabButton>;
}

@Component({
  selector: 'tb-footer-fab-buttons',
  templateUrl: './footer-fab-buttons.component.html',
  styleUrls: ['./footer-fab-buttons.component.scss'],
  animations: speedDialFabAnimations
})
export class FooterFabButtonsComponent extends PageComponent {

  @Input()
  footerFabButtons: FooterFabButtons;

  private relativeValue: boolean;
  get relative(): boolean {
    return this.relativeValue;
  }
  @Input()
  set relative(value: boolean) {
    this.relativeValue = coerceBooleanProperty(value);
  }

  buttons: Array<FooterFabButton> = [];
  fabTogglerState = 'inactive';

  closeTimeout = null;

  @HostListener('focusout', ['$event'])
  onFocusOut($event) {
    if (!this.closeTimeout) {
      this.closeTimeout = setTimeout(() => {
        this.hideItems();
      }, 100);
    }
  }

  @HostListener('focusin', ['$event'])
  onFocusIn($event) {
    if (this.closeTimeout) {
      clearTimeout(this.closeTimeout);
      this.closeTimeout = null;
    }
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  showItems() {
    this.fabTogglerState = 'active';
    this.buttons = this.footerFabButtons.buttons;
  }

  hideItems() {
    this.fabTogglerState = 'inactive';
    this.buttons = [];
  }

  onToggleFab() {
    this.buttons.length ? this.hideItems() : this.showItems();
  }
}
