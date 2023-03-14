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

import {
  NotificationTarget,
  NotificationTargetConfigType,
  NotificationTargetConfigTypeTranslateMap,
  NotificationTargetType,
  NotificationTargetTypeTranslationMap,
  SlackChanelType,
  SlackChanelTypesTranslateMap
} from '@shared/models/notification.models';
import { Component, Inject, OnDestroy } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from '@core/http/notification.service';
import { EntityType } from '@shared/models/entity-type.models';
import { deepTrim, isDefined } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';

export interface TargetsNotificationDialogData {
  target?: NotificationTarget;
  isAdd?: boolean;
}

@Component({
  selector: 'tb-target-notification-dialog',
  templateUrl: './target-notification-dialog.component.html',
  styleUrls: ['target-notification-dialog.componet.scss']
})
export class TargetNotificationDialogComponent extends
  DialogComponent<TargetNotificationDialogComponent, NotificationTarget> implements OnDestroy {

  private authState: AuthState = getCurrentAuthState(this.store);
  private authUser: AuthUser = this.authState.authUser;

  targetNotificationForm: FormGroup;
  notificationTargetType = NotificationTargetType;
  notificationTargetTypes: NotificationTargetType[] = Object.values(NotificationTargetType);
  notificationTargetTypeTranslationMap = NotificationTargetTypeTranslationMap;
  notificationTargetConfigType = NotificationTargetConfigType;
  notificationTargetConfigTypes: NotificationTargetConfigType[] = Object.values(NotificationTargetConfigType);
  notificationTargetConfigTypeTranslateMap = NotificationTargetConfigTypeTranslateMap;
  slackChanelTypes = Object.keys(SlackChanelType) as SlackChanelType[];
  slackChanelTypesTranslateMap = SlackChanelTypesTranslateMap;

  entityType = EntityType;
  isAdd = true;

  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<TargetNotificationDialogComponent, NotificationTarget>,
              @Inject(MAT_DIALOG_DATA) public data: TargetsNotificationDialogData,
              private fb: FormBuilder,
              private notificationService: NotificationService) {
    super(store, router, dialogRef);

    if (isDefined(data.isAdd)) {
      this.isAdd = data.isAdd;
    }

    this.targetNotificationForm = this.fb.group({
      name: [null, Validators.required],
      configuration: this.fb.group({
        type: [NotificationTargetType.PLATFORM_USERS],
        usersFilter: this.fb.group({
          type: [{value: NotificationTargetConfigType.ALL_USERS, disabled: !this.isTenantAdmin()}],
          usersIds: [{value: null, disabled: true}, Validators.required],
          customerId: [{value: null, disabled: true}, Validators.required],
          groupsIds: [{value: null, disabled: true}, Validators.required],
          rolesIds: [{value: null, disabled: true}, Validators.required]
        }),
        conversationType: [{value: SlackChanelType.PUBLIC_CHANNEL, disabled: true}],
        conversation: [{value: '', disabled: true}, Validators.required],
        description: [null]
      })
    });

    this.targetNotificationForm.get('configuration.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type: NotificationTargetType) => {
      this.targetNotificationForm.get('configuration').disable({emitEvent: false});
      switch (type) {
        case NotificationTargetType.PLATFORM_USERS:
          if (this.isTenantAdmin()) {
            this.targetNotificationForm.get('configuration.usersFilter').enable({emitEvent: false});
            this.targetNotificationForm.get('configuration.usersFilter.type').updateValueAndValidity({onlySelf: true});
          }
          break;
        case NotificationTargetType.SLACK:
          this.targetNotificationForm.get('configuration.conversationType').enable({emitEvent: false});
          this.targetNotificationForm.get('configuration.conversation').enable({emitEvent: false});
          break;
      }
      this.targetNotificationForm.get('configuration.type').enable({emitEvent: false});
      this.targetNotificationForm.get('configuration.description').enable({emitEvent: false});
    });

    this.targetNotificationForm.get('configuration.usersFilter.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((type: NotificationTargetConfigType) => {
      this.targetNotificationForm.get('configuration.usersFilter').disable({emitEvent: false});
      switch (type) {
        case NotificationTargetConfigType.USER_LIST:
          this.targetNotificationForm.get('configuration.usersFilter.usersIds').enable({emitEvent: false});
          break;
        case NotificationTargetConfigType.CUSTOMER_USERS:
          this.targetNotificationForm.get('configuration.usersFilter.customerId').enable({emitEvent: false});
          break;
        case NotificationTargetConfigType.USER_GROUP_LIST:
          this.targetNotificationForm.get('configuration.usersFilter.groupsIds').enable({emitEvent: false});
          break;
        case NotificationTargetConfigType.USER_ROLE:
          this.targetNotificationForm.get('configuration.usersFilter.rolesIds').enable({emitEvent: false});
          break;
      }
      this.targetNotificationForm.get('configuration.usersFilter.type').enable({emitEvent: false});
    });

    if (isDefined(data.target)) {
      this.targetNotificationForm.patchValue(data.target, {emitEvent: false});
      this.targetNotificationForm.get('configuration.type').updateValueAndValidity({onlySelf: true});
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    let formValue: NotificationTarget = deepTrim(this.targetNotificationForm.value);
    if (isDefined(this.data.target)) {
      formValue = Object.assign({}, this.data.target, formValue);
    }
    if (formValue.configuration.type === NotificationTargetType.PLATFORM_USERS && !formValue.configuration.usersFilter) {
      formValue.configuration.usersFilter = {
        type: NotificationTargetConfigType.ALL_USERS
      }
    }
    this.notificationService.saveNotificationTarget(formValue).subscribe(
      (target) => this.dialogRef.close(target)
    );
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }
}
