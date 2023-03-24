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

import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormGroup } from '@angular/forms';
import { ActivationMethod, activationMethodTranslations, User } from '@shared/models/user.model';
import { CustomerId } from '@shared/models/id/customer-id';
import { UserService } from '@core/http/user.service';
import { Observable } from 'rxjs';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@modules/home/pages/user/activation-link-dialog.component';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Authority } from '@shared/models/authority.enum';
import { GroupUserComponent } from '@home/pages/user/group-user.component';

export interface AddGroupUserDialogData {
  entitiesTableConfig: GroupEntityTableConfig<User>;
}

@Component({
  selector: 'tb-add-group-user-dialog',
  templateUrl: './add-group-user-dialog.component.html',
  styleUrls: ['./add-user-dialog.component.scss']
})
export class AddGroupUserDialogComponent extends DialogComponent<AddGroupUserDialogComponent, User> implements OnInit {

  detailsForm: UntypedFormGroup;
  user: User;

  activationMethods = Object.keys(ActivationMethod);
  activationMethodEnum = ActivationMethod;

  activationMethodTranslations = activationMethodTranslations;

  activationMethod = ActivationMethod.DISPLAY_ACTIVATION_LINK;

  entitiesTableConfig: GroupEntityTableConfig<User>;

  @ViewChild(GroupUserComponent, {static: true}) userComponent: GroupUserComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddGroupUserDialogData,
              public dialogRef: MatDialogRef<AddGroupUserDialogComponent, User>,
              private userService: UserService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);
    this.entitiesTableConfig = data.entitiesTableConfig;
  }

  ngOnInit(): void {
    this.user = {} as User;
    this.userComponent.entitiesTableConfig = this.entitiesTableConfig;
    this.userComponent.isEdit = true;
    this.userComponent.entity = this.user;
    this.detailsForm = this.userComponent.entityForm;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.detailsForm.valid) {
      const sendActivationEmail = this.activationMethod === ActivationMethod.SEND_ACTIVATION_MAIL;
      this.user = {...this.user, ...this.userComponent.entityForm.value};
      const entityGroup = this.entitiesTableConfig.entityGroup;
      if (entityGroup.ownerId.entityType === EntityType.TENANT) {
        this.user.authority = Authority.TENANT_ADMIN;
      } else if (entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
        this.user.authority = Authority.CUSTOMER_USER;
        this.user.customerId = entityGroup.ownerId as CustomerId;
      }
      const entityGroupId = !entityGroup.groupAll ? entityGroup.id.id : null;
      this.userService.saveUser(this.user, sendActivationEmail, entityGroupId).subscribe(
        (user) => {
          if (this.activationMethod === ActivationMethod.DISPLAY_ACTIVATION_LINK) {
            this.userService.getActivationLink(user.id.id).subscribe(
              (activationLink) => {
                this.displayActivationLink(activationLink).subscribe(
                  () => {
                    this.dialogRef.close(user);
                  }
                );
              }
            );
          } else {
            this.dialogRef.close(user);
          }
        }
      );
    }
  }

  displayActivationLink(activationLink: string): Observable<void> {
    return this.dialog.open<ActivationLinkDialogComponent, ActivationLinkDialogData,
      void>(ActivationLinkDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        activationLink
      }
    }).afterClosed();
  }
}
