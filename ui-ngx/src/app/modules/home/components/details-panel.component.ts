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

import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroup } from '@angular/forms';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-details-panel',
  templateUrl: './details-panel.component.html',
  styleUrls: ['./details-panel.component.scss']
})
export class DetailsPanelComponent extends PageComponent implements OnDestroy {

  @Input() headerHeightPx = 100;
  @Input() headerTitle = '';
  @Input() headerSubtitle = '';
  @Input() isReadOnly = false;
  @Input() isAlwaysEdit = false;
  @Input() isShowSearch = false;
  @Input() backgroundColor = '#FFF';

  private theFormValue: FormGroup;
  private formSubscription: Subscription = null;

  @Input()
  set theForm(value: FormGroup) {
    if (this.theFormValue !== value) {
      if (this.formSubscription !== null) {
        this.formSubscription.unsubscribe();
        this.formSubscription = null;
      }
      this.theFormValue = value;
      if (this.theFormValue !== null) {
        this.formSubscription = this.theFormValue.valueChanges.subscribe(() => this.cd.detectChanges());
      }
    }
  }

  get theForm(): FormGroup {
    return this.theFormValue;
  }

  @Output()
  closeDetails = new EventEmitter<void>();
  @Output()
  toggleDetailsEditMode = new EventEmitter<boolean>();
  @Output()
  applyDetails = new EventEmitter<void>();
  @Output()
  closeSearch = new EventEmitter<void>();

  isEditValue = false;
  showSearchPane = false;

  @Output()
  isEditChange = new EventEmitter<boolean>();

  @Input()
  get isEdit() {
    return this.isAlwaysEdit || this.isEditValue;
  }

  set isEdit(val: boolean) {
    this.isEditValue = val;
    this.isEditChange.emit(this.isEditValue);
  }


  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnDestroy() {
    if (this.formSubscription !== null) {
      this.formSubscription.unsubscribe();
    }
    super.ngOnDestroy();
  }

  onCloseDetails() {
    this.closeDetails.emit();
  }

  onToggleDetailsEditMode() {
    if (!this.isAlwaysEdit) {
      this.isEdit = !this.isEdit;
    }
    this.toggleDetailsEditMode.emit(this.isEditValue);
  }

  onApplyDetails() {
    if (this.theForm && this.theForm.valid) {
      this.applyDetails.emit();
    }
  }

  onToggleSearch() {
    this.showSearchPane = !this.showSearchPane;
    if (!this.showSearchPane) {
      this.closeSearch.emit();
    }
  }
}
