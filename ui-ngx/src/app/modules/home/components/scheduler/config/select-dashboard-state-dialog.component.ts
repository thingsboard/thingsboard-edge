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

import {
  AfterViewInit,
  Component,
  ElementRef, HostBinding,
  Inject,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@shared/components/dialog.component';
import { WINDOW } from '@core/services/window.service';
import { WindowMessage } from '@shared/models/window-message.model';

export interface SelectDashboardStateDialogData {
  dashboardId: string;
  state: string;
}

// @dynamic
@Component({
  selector: 'tb-select-dashboard-state-dialog',
  templateUrl: './select-dashboard-state-dialog.component.html',
  styleUrls: ['./select-dashboard-state-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class SelectDashboardStateDialogComponent extends DialogComponent<SelectDashboardStateDialogComponent, string>
  implements OnInit, OnDestroy, AfterViewInit {

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  @ViewChild('selectDashboardFrame', {static: true})
  selectDashboardFrameElmRef: ElementRef<HTMLIFrameElement>;

  dashboardId: string;
  currentState: string;

  onWindowMessageListener = this.onWindowMessage.bind(this);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(WINDOW) private window: Window,
              @Inject(MAT_DIALOG_DATA) public data: SelectDashboardStateDialogData,
              public dialogRef: MatDialogRef<SelectDashboardStateDialogComponent, string>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.dashboardId = data.dashboardId;
    this.currentState = data.state;
  }

  ngOnInit(): void {
    this.window.addEventListener('message', this.onWindowMessageListener);
  }

  ngOnDestroy(): void {
    this.window.removeEventListener('message', this.onWindowMessageListener);
  }

  ngAfterViewInit(): void {
    this.loadDashboard();
  }

  private loadDashboard() {
    const iframe = $(this.selectDashboardFrameElmRef.nativeElement);
    iframe.attr('state-select-view', 'true');
    let path = `/dashboard/${this.dashboardId}`;
    if (this.currentState) {
      path += `?state=${this.currentState}`;
    }
    iframe.attr('src', path);
  }

  private onWindowMessage(event: MessageEvent) {
    let message: WindowMessage;
    if (event.data) {
      try {
        message = JSON.parse(event.data);
      } catch (e) {}
    }
    if (message && message.type === 'dashboardStateSelected') {
      this.currentState = message.data;
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  select(): void {
    this.dialogRef.close(this.currentState);
  }
}
