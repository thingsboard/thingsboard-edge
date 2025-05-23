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

import { AfterViewInit, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  getSecretHelpLink,
  parseSecret,
  secretStorageCreateTitleTranslationMap,
  SecretStorageType,
  secretStorageTypeDialogTitleTranslationMap
} from '@shared/models/secret-storage.models';
import { SecretStorageComponent } from '@home/components/secret-storage/secret-storage.component';
import { FormControl } from '@angular/forms';
import { SecretStorageService } from '@core/http/secret-storage.service';

export interface SecretStorageData  {
  type: SecretStorageType;
  value: string;
  fileName?: string;
}

@Component({
  selector: 'tb-secret-storage-dialog',
  templateUrl: './secret-storage-dialog.component.html',
  styleUrls: ['./secret-storage-dialog.component.scss']
})
export class SecretStorageDialogComponent extends DialogComponent<SecretStorageDialogComponent, string>
  implements OnInit, AfterViewInit {

  dialogTitle: string;
  createNewLabel: string;

  createNew = true;

  secretType = SecretStorageType.TEXT;

  fileName: string;

  secret = new FormControl(null);

  @ViewChild('secretComponent', {static: true}) secretComponent: SecretStorageComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: SecretStorageData,
              public dialogRef: MatDialogRef<SecretStorageDialogComponent, string>,
              private secretStorageService: SecretStorageService) {
    super(store, router, dialogRef);
  }

  ngOnInit() {
    this.dialogTitle = secretStorageTypeDialogTitleTranslationMap.get(this.data.type);
    this.createNewLabel = secretStorageCreateTitleTranslationMap.get(this.data.type);
    this.secretComponent.entityForm.get('type').patchValue(this.data.type, {emitEvent: false});
    this.secretType = this.data.type;
    this.fileName = this.data.fileName;
  }

  onChange(value: boolean) {
    if (value) {
      this.secretComponent.entityForm.enable({emitEvent: false});
      this.secret.disable({emitEvent: false})
    } else {
      this.secretComponent.entityForm.disable({emitEvent: false});
      this.secret.enable({emitEvent: false})
    }
  }

  ngAfterViewInit() {
    setTimeout(() => {
      const secret = parseSecret(this.data.value);
      if (secret) {
        this.createNew = false;
        this.secret.enable({emitEvent: false});
        this.secret.patchValue(secret, {emitEvent: false});
        this.secretComponent.entityForm.disable({emitEvent: false});
      } else {
        this.secret.disable({emitEvent: false});
        this.secretComponent.entityForm.enable({emitEvent: false});
        this.createNew = true;
        this.secretComponent.entityForm.get('value').patchValue(this.data.value);
      }
    }, 0);
  }

  helpLinkId(): string {
    return getSecretHelpLink(this.data.type);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  private prepareOutputSecret(secret: string, type: SecretStorageType) {
    return '${secret:'+secret+';type:'+type+'}';
  }

  add(): void {
    if (this.createNew) {
      this.secretStorageService.saveSecret(this.secretComponent.entityFormValue()).subscribe(
        (secret) => {
          this.dialogRef.close(this.prepareOutputSecret(secret.name, this.data.type));
        }
      );
    } else {
      this.dialogRef.close(this.prepareOutputSecret(this.secret.value, this.data.type));
    }
  }
}
