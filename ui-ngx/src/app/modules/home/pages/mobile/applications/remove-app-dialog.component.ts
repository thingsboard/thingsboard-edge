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

import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { FormBuilder } from '@angular/forms';
import { MobileAppService } from '@core/http/mobile-app.service';
import { mergeMap } from 'rxjs';
import { MobileAppStatus } from '@shared/models/mobile-app.models';

export interface MobileAppDeleteDialogData {
  id: string;
}

@Component({
  selector: 'tb-remove-app-dialog',
  templateUrl: './remove-app-dialog.component.html',
  styleUrls: ['./remove-app-dialog.component.scss']
})
export class RemoveAppDialogComponent extends DialogComponent<RemoveAppDialogComponent, boolean> {

  readonly deleteApplicationText: SafeHtml;
  readonly deleteVerificationText: string;

  deleteVerification = this.fb.control('');

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<RemoveAppDialogComponent, boolean>,
              @Inject(MAT_DIALOG_DATA) private data: MobileAppDeleteDialogData,
              private translate: TranslateService,
              private sanitizer: DomSanitizer,
              private fb: FormBuilder,
              private mobileAppService: MobileAppService,) {
    super(store, router, dialogRef);
    this.deleteVerificationText = this.translate.instant('mobile.delete-application-phrase');
    this.deleteApplicationText = this.sanitizer.bypassSecurityTrustHtml(
      this.translate.instant('mobile.delete-application-text', {phrase: this.deleteVerificationText})
    )
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  suspend(): void {
    this.mobileAppService.getMobileAppInfoById(this.data.id).pipe(
      mergeMap(value => {
        value.status = MobileAppStatus.SUSPENDED;
        return this.mobileAppService.saveMobileApp(value)
      })
    ).subscribe(() => {
      this.dialogRef.close(true);
    });
  }

  delete(): void {
    this.mobileAppService.deleteMobileApp(this.data.id).subscribe(() => {
      this.dialogRef.close(true);
    });
  }
}
