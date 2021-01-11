///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Observable, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Injectable } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Operation, Resource } from '@shared/models/security.models';
import { User } from '@shared/models/user.model';
import { GroupUserComponent } from '@home/pages/user/group-user.component';
import { AddGroupUserDialogComponent, AddGroupUserDialogData } from '@home/pages/user/add-group-user-dialog.component';
import { UserService } from '@core/http/user.service';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@home/pages/user/activation-link-dialog.component';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AuthService } from '@core/auth/auth.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';

@Injectable()
export class UserGroupConfigFactory implements EntityGroupStateConfigFactory<User> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<User>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private userService: UserService,
              private authService: AuthService,
              private store: Store<AppState>) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<User>): Observable<GroupEntityTableConfig<User>> {
    const config = new GroupEntityTableConfig<User>(entityGroup, params);

    config.entityComponent = GroupUserComponent;

    config.entityTitle = (user) => user ? user.email : '';

    config.deleteEntityTitle = user => this.translate.instant('user.delete-user-title', { userEmail: user.email });
    config.deleteEntityContent = () => this.translate.instant('user.delete-user-text');
    config.deleteEntitiesTitle = count => this.translate.instant('user.delete-users-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('user.delete-users-text');

    config.loadEntity = id => this.userService.getUser(id.id);
    config.saveEntity = user => this.userService.saveUser(user);
    config.deleteEntity = id => this.userService.deleteUser(id.id);

    config.onEntityAction = action => this.onUserAction(action);
    config.addEntity = () => this.addUser(config);

    const auth = getCurrentAuthState(this.store);
    if (config.settings.enableDelete) {
      config.deleteEnabled = user => user && user.id && user.id.id !== auth.userDetails.id.id;
    }
    if (auth.userTokenAccessEnabled && this.userPermissionsService.hasGenericPermission(Resource.USER, Operation.IMPERSONATE)) {
      const isTenantAdmins = entityGroup.ownerId.entityType === EntityType.TENANT;
      config.cellActionDescriptors.push(
        {
          name: isTenantAdmins ?
            this.translate.instant('user.login-as-tenant-admin') :
            this.translate.instant('user.login-as-customer-user'),
          mdiIcon: 'mdi:login',
          isEnabled: () => true,
          onAction: ($event, entity) => this.loginAsUser($event, entity)
        }
      );
    }
    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  addUser(config: GroupEntityTableConfig<User>): Observable<User> {
    return this.dialog.open<AddGroupUserDialogComponent, AddGroupUserDialogData,
      User>(AddGroupUserDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed();
  }

  loginAsUser($event: Event, user: User | ShortEntityView) {
    if ($event) {
      $event.stopPropagation();
    }
    this.authService.loginAsUser(user.id.id).subscribe();
  }

  displayActivationLink($event: Event, user: User) {
    if ($event) {
      $event.stopPropagation();
    }
    this.userService.getActivationLink(user.id.id).subscribe(
      (activationLink) => {
        this.dialog.open<ActivationLinkDialogComponent, ActivationLinkDialogData,
          void>(ActivationLinkDialogComponent, {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
          data: {
            activationLink
          }
        });
      }
    );
  }

  resendActivation($event: Event, user: User) {
    if ($event) {
      $event.stopPropagation();
    }
    this.userService.sendActivationEmail(user.email).subscribe(() => {
      this.store.dispatch(new ActionNotificationShow(
        {
          message: this.translate.instant('user.activation-email-sent-message'),
          type: 'success'
        }));
    });
  }

  setUserCredentialsEnabled($event: Event, user: User, userCredentialsEnabled: boolean) {
    if ($event) {
      $event.stopPropagation();
    }
    this.userService.setUserCredentialsEnabled(user.id.id, userCredentialsEnabled).subscribe(() => {
      if (!user.additionalInfo) {
        user.additionalInfo = {};
      }
      user.additionalInfo.userCredentialsEnabled = userCredentialsEnabled;
      this.store.dispatch(new ActionNotificationShow(
        {
          message: this.translate.instant(userCredentialsEnabled ? 'user.enable-account-message' : 'user.disable-account-message'),
          type: 'success'
        }));
    });
  }

  onUserAction(action: EntityAction<User>): boolean {
    switch (action.action) {
      case 'loginAsUser':
        this.loginAsUser(action.event, action.entity);
        return true;
      case 'displayActivationLink':
        this.displayActivationLink(action.event, action.entity);
        return true;
      case 'resendActivation':
        this.resendActivation(action.event, action.entity);
        return true;
      case 'disableAccount':
        this.setUserCredentialsEnabled(action.event, action.entity, false);
        return true;
      case 'enableAccount':
        this.setUserCredentialsEnabled(action.event, action.entity, true);
        return true;
    }
    return false;
  }

}
