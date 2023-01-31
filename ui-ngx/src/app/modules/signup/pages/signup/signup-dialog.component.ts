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

import { Component, HostBinding, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { SelfRegistrationService } from '@core/http/self-register.service';
import { Observable } from 'rxjs/internal/Observable';

export interface SignupDialogData {
  title: string;
  content$: Observable<string>;
}

@Component({
  selector: 'tb-signup-dialog',
  templateUrl: './signup-dialog.component.html',
  styleUrls: []
})
export class SignupDialogComponent extends DialogComponent<SignupDialogComponent, boolean> implements OnInit {

  @HostBinding('class') class = 'tb-custom-css';

  title: string;
  dialogText: SafeHtml;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SignupDialogData,
              private selfRegistrationService: SelfRegistrationService,
              private domSanitizer: DomSanitizer,
              public dialogRef: MatDialogRef<SignupDialogComponent, boolean>) {
    super(store, router, dialogRef);
    this.title = this.data.title;
  }

  ngOnInit(): void {
    this.data.content$.subscribe((content) => {
      this.dialogText = this.domSanitizer.bypassSecurityTrustHtml(content);
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  accept(): void {
    this.dialogRef.close(true);
  }
}
