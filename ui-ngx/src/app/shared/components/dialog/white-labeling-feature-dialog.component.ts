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
    selector: 'tb-white-labeling-feature-dialog',
    templateUrl: './white-labeling-feature-dialog.component.html',
    styleUrls: ['./white-labeling-feature-dialog.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class WhiteLabelingFeatureDialogComponent extends DialogComponent<WhiteLabelingFeatureDialogComponent> {

  @HostBinding('style.width') width = '100%';
  @HostBinding('style.height') height = '100%';

  whiteLabelingSvg = 'assets/white-labeling.svg';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(WINDOW) private window: Window,
              public dialogRef: MatDialogRef<WhiteLabelingFeatureDialogComponent>) {
    super(store, router, dialogRef);
  }

  public gotoWhiteLabelingDoc() {
    this.window.open('https://thingsboard.io/docs/user-guide/white-labeling/', '_blank');
  }

  public upgrade() {
    this.dialogRef.close();
    this.window.open('https://thingsboard.io/pricing/', '_blank');
  }

  public close() {
    this.dialogRef.close();
  }
}
