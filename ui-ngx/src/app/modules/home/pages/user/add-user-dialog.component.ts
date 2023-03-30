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

import { Component, HostBinding, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivationMethod, activationMethodTranslations, User } from '@shared/models/user.model';
import { CustomerId } from '@shared/models/id/customer-id';
import { UserService } from '@core/http/user.service';
import { Observable, Subscription } from 'rxjs';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@modules/home/pages/user/activation-link-dialog.component';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Authority } from '@shared/models/authority.enum';
import { UserComponent } from '@home/pages/user/user.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { MatStepper } from '@angular/material/stepper';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityInfoData } from '@shared/models/entity.models';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { OwnerAndGroupsData } from '@shared/components/group/owner-and-groups.component';
import { MediaBreakpoints } from '@shared/models/constants';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { TenantId } from '@shared/models/id/tenant-id';

export interface AddUserDialogData {
  entitiesTableConfig: EntityTableConfig<User> | GroupEntityTableConfig<User>;
  tenantId?: string;
}

@Component({
  selector: 'tb-add-user-dialog',
  templateUrl: './add-user-dialog.component.html',
  styleUrls: ['./add-user-dialog.component.scss']
})
export class AddUserDialogComponent extends DialogComponent<AddUserDialogComponent, User> implements OnInit, OnDestroy {

  @ViewChild('addUserWizardStepper', {static: true}) addUserWizardStepper: MatStepper;
  @ViewChild(UserComponent, {static: true}) userComponent: UserComponent;

  isSysAdmin = getCurrentAuthUser(this.store).authority === Authority.SYS_ADMIN;

  @HostBinding('class')
  clazz = this.isSysAdmin ? 'no-stepper' : '';

  entityType = EntityType;

  selectedIndex = 0;

  showNext = true;

  labelPosition = 'end';

  detailsForm: UntypedFormGroup;
  ownerAndGroupsFormGroup: UntypedFormGroup;
  user: User;

  activationMethods = Object.keys(ActivationMethod);
  activationMethodEnum = ActivationMethod;

  activationMethodTranslations = activationMethodTranslations;

  activationMethod = ActivationMethod.DISPLAY_ACTIVATION_LINK;

  entitiesTableConfig: EntityTableConfig<User> | GroupEntityTableConfig<User>;
  tenantId: string;
  customerId: string;
  entityGroup: EntityGroupInfo;
  groupMode = false;
  initialOwnerId: EntityId;
  initialGroups: EntityInfoData[];

  private subscriptions: Subscription[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddUserDialogData,
              public dialogRef: MatDialogRef<AddUserDialogComponent, User>,
              private userService: UserService,
              private userPermissionsService: UserPermissionsService,
              private breakpointObserver: BreakpointObserver,
              private fb: UntypedFormBuilder,
              private dialog: MatDialog) {
    super(store, router, dialogRef);
    this.entitiesTableConfig = data.entitiesTableConfig;
    this.tenantId = data.tenantId;
    this.customerId = this.entitiesTableConfig.customerId;
    this.groupMode = this.entitiesTableConfig instanceof GroupEntityTableConfig;
    this.entityGroup = this.groupMode ? (this.entitiesTableConfig as GroupEntityTableConfig<BaseData<HasId>>).entityGroup : null;
  }

  ngOnInit(): void {
    this.user = {} as User;
    this.userComponent.entitiesTableConfig = this.entitiesTableConfig;
    this.userComponent.isEdit = true;
    this.userComponent.entity = this.user;
    this.detailsForm = this.userComponent.entityForm;
    this.initialGroups = [];
    if (this.groupMode) {
      this.initialOwnerId = this.entityGroup.ownerId;
      if (!this.entityGroup.groupAll) {
        this.initialGroups = [{id: this.entityGroup.id, name: this.entityGroup.name}];
      }
    } else {
      if (this.customerId) {
        this.initialOwnerId = new CustomerId(this.customerId);
      } else {
        if (this.isSysAdmin && this.tenantId) {
          this.initialOwnerId = new TenantId(this.tenantId);
        } else {
          this.initialOwnerId = this.userPermissionsService.getUserOwnerId();
        }
      }
    }
    const ownerAndGroups: OwnerAndGroupsData = {
      owner: this.initialOwnerId,
      groups: this.initialGroups
    };
    this.ownerAndGroupsFormGroup = this.fb.group({
      ownerAndGroups: [ownerAndGroups, [Validators.required]]
    });

    this.labelPosition = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']) ? 'end' : 'bottom';

    this.subscriptions.push(this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm'])
      .subscribe((state: BreakpointState) => {
          if (state.matches) {
            this.labelPosition = 'end';
          } else {
            this.labelPosition = 'bottom';
          }
        }
      ));
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.addUserWizardStepper.previous();
  }

  nextStep(): void {
    this.addUserWizardStepper.next();
  }

  getFormLabel(index: number): string {
    switch (index) {
      case 0:
        return 'user.user-details';
      case 1:
        return 'entity-group.owner-and-groups';
    }
  }

  get maxStepperIndex(): number {
    return this.addUserWizardStepper?._steps?.length - 1;
  }

  add(): void {
    if (this.isSysAdmin && this.detailsForm.valid || this.allValid()) {
      const sendActivationEmail = this.activationMethod === ActivationMethod.SEND_ACTIVATION_MAIL;
      this.user = {...this.user, ...this.userComponent.entityForm.value};

      const targetOwnerAndGroups: OwnerAndGroupsData = this.ownerAndGroupsFormGroup.get('ownerAndGroups').value;
      const targetOwner = targetOwnerAndGroups.owner;
      let targetOwnerId: EntityId;
      if ((targetOwner as EntityInfoData).name) {
        targetOwnerId = (targetOwner as EntityInfoData).id;
      } else {
        targetOwnerId = targetOwner as EntityId;
      }
      if (targetOwnerId.entityType === EntityType.TENANT) {
        this.user.authority = Authority.TENANT_ADMIN;
        this.user.tenantId = targetOwnerId as TenantId;
      } else if (targetOwnerId.entityType === EntityType.CUSTOMER) {
        this.user.authority = Authority.CUSTOMER_USER;
        this.user.customerId = targetOwnerId as CustomerId;
      }
      const entityGroupIds = targetOwnerAndGroups.groups.map(group => group.id.id);
      this.userService.saveUser(this.user, sendActivationEmail, entityGroupIds).subscribe(
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

  allValid(): boolean {
    return !this.addUserWizardStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addUserWizardStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    if (this.selectedIndex === this.maxStepperIndex) {
      this.showNext = false;
    } else {
      this.showNext = true;
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
