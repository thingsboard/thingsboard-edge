///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, DestroyRef, EventEmitter, inject, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { DefaultMobilePage, defaultMobilePageMap, hideDefaultMenuItems } from '@shared/models/mobile-app.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-default-mobile-page-panel',
  templateUrl: './default-mobile-page-panel.component.html',
  styleUrls: ['./default-mobile-page-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DefaultMobilePagePanelComponent implements OnInit {

  @Input()
  disabled: boolean;

  @Input()
  pageItem: DefaultMobilePage;

  @Input()
  popover: TbPopoverComponent<DefaultMobilePagePanelComponent>;

  @Output()
  defaultMobilePageApplied = new EventEmitter<DefaultMobilePage>();

  mobilePageFormGroup = this.fb.group({
    visible: [true],
    icon: [''],
    label: ['', [Validators.pattern(/\S/), Validators.maxLength(255)]],
  });

  isCleanupEnabled = false;
  defaultItemName: string;

  private defaultMobilePages: Omit<DefaultMobilePage, 'type' | 'visible'>;
  private destroyRef = inject(DestroyRef);

  constructor(private fb: FormBuilder) {
  }

  ngOnInit() {
    this.defaultMobilePages = defaultMobilePageMap.get(this.pageItem.id);
    this.defaultItemName = this.defaultMobilePages.label;

    this.mobilePageFormGroup.patchValue({
      label: this.pageItem.label,
      icon: this.pageItem.icon ? this.pageItem.icon : this.defaultMobilePages.icon,
      visible: this.pageItem.visible
    }, {emitEvent: false});

    if (this.disabled) {
      this.mobilePageFormGroup.disable({emitEvent: false});
    } else {
      this.mobilePageFormGroup.valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateModel();
      });
      this.updateCleanupState();
    }
  }

  cancel() {
    this.popover?.hide();
  }

  apply() {
    this.defaultMobilePageApplied.emit(this.pageItem);
  }

  cleanup() {
    this.mobilePageFormGroup.patchValue({
      visible: !hideDefaultMenuItems.includes(this.pageItem.id),
      icon: this.defaultMobilePages.icon,
      label: null
    }, {emitEvent: false});
    this.mobilePageFormGroup.markAsDirty();
    this.updateModel();
  }

  private updateCleanupState() {
    this.isCleanupEnabled = (hideDefaultMenuItems.includes(this.pageItem.id) ? this.pageItem.visible : !this.pageItem.visible) ||
      !!this.pageItem.label ||
      !!this.pageItem.icon;
  }

  private updateModel() {
    this.pageItem.visible = this.mobilePageFormGroup.get('visible').value;
    const label = this.mobilePageFormGroup.get('label').value;
    if (label) {
      this.pageItem.label = label;
    } else {
      delete this.pageItem.label;
    }
    const icon = this.mobilePageFormGroup.get('icon').value;
    if (icon !== this.defaultMobilePages.icon) {
      this.pageItem.icon = icon;
    } else {
      delete this.pageItem.icon;
    }
    this.updateCleanupState();
  }
}
