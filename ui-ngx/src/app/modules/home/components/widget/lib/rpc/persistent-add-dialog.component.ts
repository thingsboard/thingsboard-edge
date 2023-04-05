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

import { Component, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RequestData } from '@shared/models/rpc.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-persistent-add-dialog',
  templateUrl: './persistent-add-dialog.component.html',
  styleUrls: ['./persistent-add-dialog.component.scss']
})

export class PersistentAddDialogComponent extends DialogComponent<PersistentAddDialogComponent, RequestData> implements OnInit {

  public persistentFormGroup: UntypedFormGroup;
  public rpcMessageTypeText: string;

  private requestData: RequestData = null;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<PersistentAddDialogComponent, RequestData>,
              private fb: UntypedFormBuilder,
              private translate: TranslateService) {
    super(store, router, dialogRef);

    this.persistentFormGroup = this.fb.group(
      {
        method: ['', [Validators.required, Validators.pattern(/^\S+$/)]],
        oneWayElseTwoWay: [false],
        retries: [null, [Validators.pattern(/^-?[0-9]+$/), Validators.min(0)]],
        params: [null],
        additionalInfo: [null]
      }
    );
  }

  save() {
    this.requestData = this.persistentFormGroup.value;
    this.close();
  }

  ngOnInit(): void {
    this.rpcMessageTypeText = this.translate.instant('widgets.persistent-table.message-types.false');
    this.persistentFormGroup.get('oneWayElseTwoWay').valueChanges.subscribe(
      () => {
        this.rpcMessageTypeText = this.translate.instant(`widgets.persistent-table.message-types.${this.persistentFormGroup.get('oneWayElseTwoWay').value}`);
      }
    );
  }

  close(): void {
    this.dialogRef.close(this.requestData);
  }
}
