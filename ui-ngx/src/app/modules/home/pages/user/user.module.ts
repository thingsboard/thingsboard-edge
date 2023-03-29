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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { UserComponent } from '@modules/home/pages/user/user.component';
import { UserRoutingModule } from '@modules/home/pages/user/user-routing.module';
import { AddUserDialogComponent } from '@modules/home/pages/user/add-user-dialog.component';
import { ActivationLinkDialogComponent } from '@modules/home/pages/user/activation-link-dialog.component';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { UserTabsComponent } from '@home/pages/user/user-tabs.component';
import { AddGroupUserDialogComponent } from '@home/pages/user/add-group-user-dialog.component';
import { USER_GROUP_CONFIG_FACTORY } from '@home/models/group/group-entities-table-config.models';
import { UserGroupConfigFactory } from '@home/pages/user/user-group-config.factory';
import { UserTableHeaderComponent } from '@home/pages/user/user-table-header.component';

@NgModule({
  declarations: [
    UserComponent,
    UserTableHeaderComponent,
    UserTabsComponent,
    AddUserDialogComponent,
    AddGroupUserDialogComponent,
    ActivationLinkDialogComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    UserRoutingModule
  ],
  providers: [
    {
      provide: USER_GROUP_CONFIG_FACTORY,
      useClass: UserGroupConfigFactory
    }
  ]
})
export class UserModule { }
