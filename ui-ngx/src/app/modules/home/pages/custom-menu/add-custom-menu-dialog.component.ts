///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import {
  CMAssigneeType, cmAssigneeTypeTranslations,
  CMScope,
  cmScopeTranslations,
  CustomMenu,
  CustomMenuInfo
} from '@shared/models/custom-menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import {
  scadaSymbolBehaviorTypes,
  scadaSymbolBehaviorTypeTranslations
} from '@home/components/widget/lib/scada/scada-symbol.models';

@Component({
  selector: 'tb-add-custom-menu-dialog',
  templateUrl: './add-custom-menu-dialog.component.html',
  styleUrls: ['./add-custom-menu-dialog.component.scss']
})
export class AddCustomMenuDialogComponent extends DialogComponent<AddCustomMenuDialogComponent, CustomMenu> implements OnInit, OnDestroy {

  Authority = Authority;

  cmScopeTranslations = cmScopeTranslations;

  cmAssigneeTypeTranslations = cmAssigneeTypeTranslations;

  authUser = getCurrentAuthUser(this.store);

  customMenuFormGroup: UntypedFormGroup;

  customMenuScopes: CMScope[] = this.authUser.authority === Authority.TENANT_ADMIN ?
    [CMScope.TENANT, CMScope.CUSTOMER] : [CMScope.CUSTOMER];

  assigneeTypes: CMAssigneeType[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<AddCustomMenuDialogComponent, CustomMenu>,
              private customMenuService: CustomMenuService,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.customMenuFormGroup = this.fb.group({
      name: [null, [Validators.required]],
      scope: [this.authUser.authority === Authority.TENANT_ADMIN ? CMScope.TENANT : CMScope.CUSTOMER],
      assigneeType: [CMAssigneeType.NO_ASSIGN],
      assignToList: [null, Validators.required]
    });
    this.updateAssigneeTypes();
    this.updateAssignToList();
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      this.customMenuFormGroup.get('scope').valueChanges.subscribe(() => {
        this.updateAssigneeTypes();
      });
    } else {
      this.customMenuFormGroup.get('scope').disable({emitEvent: false});
    }
    this.customMenuFormGroup.get('assigneeType').valueChanges.subscribe(() => {
      this.updateAssignToList();
    });
  }

  private updateAssigneeTypes() {
    const scope: CMScope = this.customMenuFormGroup.get('scope').value;
    if (scope === CMScope.TENANT) {
      this.assigneeTypes = [CMAssigneeType.NO_ASSIGN, CMAssigneeType.ALL, CMAssigneeType.USERS];
    } else {
      this.assigneeTypes = [CMAssigneeType.NO_ASSIGN, CMAssigneeType.ALL, CMAssigneeType.USERS, CMAssigneeType.CUSTOMERS];
    }
  }

  private updateAssignToList() {
    const assigneeType: CMAssigneeType = this.customMenuFormGroup.get('assigneeType').value;
    this.customMenuFormGroup.get('assignToList').patchValue(null, {emitEvent: false});
    switch (assigneeType) {
      case CMAssigneeType.NO_ASSIGN:
      case CMAssigneeType.ALL:
        this.customMenuFormGroup.get('assignToList').disable({emitEvent: false});
        break;
      case CMAssigneeType.CUSTOMERS:
      case CMAssigneeType.USERS:
        this.customMenuFormGroup.get('assignToList').enable({emitEvent: false});
        break;
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.customMenuFormGroup.valid) {
      const customMenuInfo: CustomMenuInfo = {
        name: this.customMenuFormGroup.get('name').value,
        scope: this.customMenuFormGroup.get('scope').value,
        assigneeType: this.customMenuFormGroup.get('assigneeType').value
      };
      const assignToList: string[] = this.customMenuFormGroup.get('assignToList').value;
      this.customMenuService.saveCustomMenu(customMenuInfo, assignToList).subscribe(customMenu => {
        this.dialogRef.close(customMenu);
      });
    }
  }

  protected readonly scadaSymbolBehaviorTypes = scadaSymbolBehaviorTypes;
  protected readonly scadaSymbolBehaviorTypeTranslations = scadaSymbolBehaviorTypeTranslations;
}
