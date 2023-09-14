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

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ActionPreferencesPutUserSettings } from '@core/auth/auth.actions';
import { EdgeInfo, EdgeInstructionsMethod } from '@shared/models/edge.models';
import { EdgeService } from '@core/http/edge.service';

export interface EdgeInstructionsDialogData {
  edge: EdgeInfo;
  afterAdd: boolean;
}

@Component({
  selector: 'tb-edge-installation-dialog',
  templateUrl: './edge-instructions-dialog.component.html',
  styleUrls: ['./edge-instructions-dialog.component.scss']
})
export class EdgeInstructionsDialogComponent extends DialogComponent<EdgeInstructionsDialogComponent> implements OnInit, OnDestroy {

  dialogTitle: string;
  showDontShowAgain: boolean;

  loadedInstructions = false;
  notShowAgain = false;
  tabIndex = 0;
  instructionsMethod = EdgeInstructionsMethod;
  contentData: any = {};

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) private data: EdgeInstructionsDialogData,
              public dialogRef: MatDialogRef<EdgeInstructionsDialogComponent>,
              private edgeService: EdgeService) {
    super(store, router, dialogRef);

    if (this.data.afterAdd) {
      this.dialogTitle = 'edge.install-connect-instructions-edge-created';
      this.showDontShowAgain = true;
    } else {
      this.dialogTitle = 'edge.install-connect-instructions';
      this.showDontShowAgain = false;
    }
  }

  ngOnInit() {
    this.getInstructions(this.instructionsMethod[this.tabIndex]);
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  close(): void {
    if (this.notShowAgain && this.showDontShowAgain) {
      this.store.dispatch(new ActionPreferencesPutUserSettings({notDisplayInstructionsAfterAddEdge: true}));
      this.dialogRef.close(null);
    } else {
      this.dialogRef.close(null);
    }
  }

  selectedTabChange(index: number) {
    this.getInstructions(this.instructionsMethod[index]);
  }

  getInstructions(method: string) {
    if (!this.contentData[method]) {
      this.loadedInstructions = false;
      this.edgeService.getEdgeInstallInstructions(this.data.edge.id.id, method).subscribe(
        res => {
          this.contentData[method] = res.installInstructions;
          this.loadedInstructions = true;
        }
      );
    }
  }
}
