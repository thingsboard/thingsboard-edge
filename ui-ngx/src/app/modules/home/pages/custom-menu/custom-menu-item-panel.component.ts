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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { CMScope, CustomMenuItem } from '@shared/models/custom-menu.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormControl } from '@angular/forms';

@Component({
  selector: 'tb-custom-menu-item-panel',
  templateUrl: './custom-menu-item-panel.component.html',
  styleUrls: ['./custom-menu-item-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CustomMenuItemPanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  scope: CMScope;

  @Input()
  subItem: boolean;

  @Input()
  menuItem: CustomMenuItem;

  @Input()
  popover: TbPopoverComponent<CustomMenuItemPanelComponent>;

  @Output()
  customMenuItemApplied = new EventEmitter<CustomMenuItem>();

  title: string;

  customMenuItemControl: UntypedFormControl;

  constructor(private fb: UntypedFormBuilder) {
  }

  ngOnInit(): void {
    this.customMenuItemControl = this.fb.control(this.menuItem);
    this.title = this.subItem ? 'custom-menu.edit-custom-menu-subitem' : 'custom-menu.edit-custom-menu-item';
    if (this.disabled) {
      this.customMenuItemControl.disable({emitEvent: false});
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    if (this.customMenuItemControl.valid) {
      const menuItem: CustomMenuItem = this.customMenuItemControl.value;
      this.customMenuItemApplied.emit(menuItem);
    }
  }
}
