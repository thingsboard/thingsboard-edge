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

import { Component } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder } from '@angular/forms';
import {
  AccountTwoFaSettings,
  BackupCodeTwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { mergeMap, tap } from 'rxjs/operators';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { deepClone } from '@core/utils';

import printTemplate from '!raw-loader!./backup-code-print-template.raw';

@Component({
  selector: 'tb-backup-code-auth-dialog',
  templateUrl: './backup-code-auth-dialog.component.html',
  styleUrls: ['./authentication-dialog.component.scss']
})
export class BackupCodeAuthDialogComponent extends DialogComponent<BackupCodeAuthDialogComponent> {

  private config: AccountTwoFaSettings;
  backupCode: BackupCodeTwoFactorAuthAccountConfig;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              private importExportService: ImportExportService,
              public dialogRef: MatDialogRef<BackupCodeAuthDialogComponent>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.twoFaService.generateTwoFaAccountConfig(TwoFactorAuthProviderType.BACKUP_CODE).pipe(
      tap((data: BackupCodeTwoFactorAuthAccountConfig) => this.backupCode = data),
      mergeMap(data => this.twoFaService.verifyAndSaveTwoFaAccountConfig(data, null, {ignoreLoading: true}))
    ).subscribe((config) => {
      this.config = config;
    });
  }

  closeDialog() {
    this.dialogRef.close(this.config);
  }

  downloadFile() {
    this.importExportService.exportText(this.backupCode.codes, 'backup-codes');
  }

  printCode() {
    const codeTemplate = deepClone(this.backupCode.codes)
      .map(code => `<div class="code-row"><input type="checkbox"><span class="code">${code}</span></div>`).join('');
    const printPage = printTemplate.replace('${codesBlock}', codeTemplate);
    const newWindow = window.open('', 'Print backup code');

    newWindow.document.open();
    newWindow.document.write(printPage);

    setTimeout(() => {
      newWindow.print();

      newWindow.document.close();

      setTimeout(() => {
        newWindow.close();
      }, 10);
    }, 0);
  }
}
