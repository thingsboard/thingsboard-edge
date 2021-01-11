///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
