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

import { Observable, of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { UtilsService } from '@core/services/utils.service';
import {
  EntityGroupStateConfigFactory,
  EntityGroupStateInfo,
  GroupEntityTableConfig
} from '@home/models/group/group-entities-table-config.models';
import { Inject, Injectable } from '@angular/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupParams, ShortEntityView } from '@shared/models/entity-group.models';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UserInfo } from '@shared/models/user.model';
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
import { Router, UrlTree } from '@angular/router';
import { WINDOW } from '@core/services/window.service';
import { mergeMap } from 'rxjs/operators';

@Injectable()
export class UserGroupConfigFactory implements EntityGroupStateConfigFactory<UserInfo> {

  constructor(private groupConfigTableConfigService: GroupConfigTableConfigService<UserInfo>,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService,
              private userService: UserService,
              private authService: AuthService,
              private router: Router,
              private store: Store<AppState>,
              @Inject(WINDOW) private window: Window) {
  }

  createConfig(params: EntityGroupParams, entityGroup: EntityGroupStateInfo<UserInfo>): Observable<GroupEntityTableConfig<UserInfo>> {
    const config = new GroupEntityTableConfig<UserInfo>(entityGroup, params);

    config.entityComponent = GroupUserComponent;

    config.entityTitle = (user) => user ? user.email : '';

    config.deleteEntityTitle = user => this.translate.instant('user.delete-user-title', { userEmail: user.email });
    config.deleteEntityContent = () => this.translate.instant('user.delete-user-text');
    config.deleteEntitiesTitle = count => this.translate.instant('user.delete-users-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('user.delete-users-text');

    config.loadEntity = id => this.userService.getUserInfo(id.id);
    config.saveEntity = user => this.userService.saveUser(user).pipe(
      mergeMap((savedUser) => this.userService.getUserInfo(savedUser.id.id))
    );
    config.deleteEntity = id => this.userService.deleteUser(id.id);

    config.onEntityAction = action => this.onUserAction(action, config, params);
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
          isEnabled: config.loginAsUserEnabled,
          onAction: ($event, entity) => this.loginAsUser($event, entity)
        }
      );
    }
    return of(this.groupConfigTableConfigService.prepareConfiguration(params, config));
  }

  addUser(config: GroupEntityTableConfig<UserInfo>): Observable<UserInfo> {
    return this.dialog.open<AddGroupUserDialogComponent, AddGroupUserDialogData,
      UserInfo>(AddGroupUserDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed();
  }

  private openUser($event: Event, user: UserInfo, config: GroupEntityTableConfig<UserInfo>, params: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (params.hierarchyView) {
      let url: UrlTree;
      if (params.groupType === EntityType.EDGE) {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId, params.customerId,
          'edgeGroups', params.childEntityGroupId, params.edgeId, 'userGroups', params.edgeEntitiesGroupId, user.id.id]);
      } else {
        url = this.router.createUrlTree(['customerGroups', params.entityGroupId,
          params.customerId, 'userGroups', params.childEntityGroupId, user.id.id]);
      }
      this.window.open(window.location.origin + url, '_blank');
    } else {
      const url = this.router.createUrlTree([user.id.id], {relativeTo: config.getActivatedRoute()});
      this.router.navigateByUrl(url);
    }
  }

  loginAsUser($event: Event, user: UserInfo | ShortEntityView) {
    if ($event) {
      $event.stopPropagation();
    }
    this.authService.loginAsUser(user.id.id).subscribe();
  }

  displayActivationLink($event: Event, user: UserInfo) {
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

  resendActivation($event: Event, user: UserInfo) {
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

  setUserCredentialsEnabled($event: Event, user: UserInfo, userCredentialsEnabled: boolean) {
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

  onUserAction(action: EntityAction<UserInfo>, config: GroupEntityTableConfig<UserInfo>, params: EntityGroupParams): boolean {
    switch (action.action) {
      case 'open':
        this.openUser(action.event, action.entity, config, params);
        return true;
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
