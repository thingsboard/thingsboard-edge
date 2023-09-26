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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { phoneNumberPattern, SmsProviderConfiguration, TestSmsRequest } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';

export interface SendTestSmsDialogData {
  smsProviderConfiguration: SmsProviderConfiguration;
}

@Component({
  selector: 'tb-send-test-sms-dialog',
  templateUrl: './send-test-sms-dialog.component.html',
  styleUrls: []
})
export class SendTestSmsDialogComponent extends
  DialogComponent<SendTestSmsDialogComponent> implements OnInit {

  phoneNumberPattern = phoneNumberPattern;

  sendTestSmsFormGroup: UntypedFormGroup;

  smsProviderConfiguration = this.data.smsProviderConfiguration;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SendTestSmsDialogData,
              private adminService: AdminService,
              private translate: TranslateService,
              public dialogRef: MatDialogRef<SendTestSmsDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.sendTestSmsFormGroup = this.fb.group({
      numberTo: [null, [Validators.required, Validators.pattern(phoneNumberPattern)]],
      message: [null, [Validators.required, Validators.maxLength(1600)]]
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  sendTestSms(): void {
    const request: TestSmsRequest =  {
      providerConfiguration: this.smsProviderConfiguration,
      numberTo: this.sendTestSmsFormGroup.value.numberTo,
      message: this.sendTestSmsFormGroup.value.message
    };
    this.adminService.sendTestSms(request).subscribe(
      () => {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: this.translate.instant('admin.test-sms-sent'),
            target: 'sendTestSmsDialogContent',
            verticalPosition: 'bottom',
            horizontalPosition: 'left',
            type: 'success'
          }));
      }
    );
  }
}
