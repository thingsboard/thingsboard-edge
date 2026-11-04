// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, HostBinding, Inject, ViewEncapsulation } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { WINDOW } from '@core/services/window.service';

// @dynamic
@Component({
  selector: 'tb-evaluation-license-dialog',
  templateUrl: './evaluation-license-dialog.component.html',
  styleUrls: ['./evaluation-license-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class EvaluationLicenseDialogComponent extends DialogComponent<EvaluationLicenseDialogComponent> {

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(WINDOW) private window: Window,
              public dialogRef: MatDialogRef<EvaluationLicenseDialogComponent>) {
    super(store, router, dialogRef);
  }

  public getPersonalLicense() {
    this.dialogRef.close();
    this.window.open('https://thingsboard.io/pricing/?active=thingsboard-edge', '_blank');
  }

  public close() {
    this.dialogRef.close();
  }
}
