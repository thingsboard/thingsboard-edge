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

import { Injectable } from '@angular/core';

import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  GroupActionDescriptor,
  HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AuthUser, User, UserInfo } from '@shared/models/user.model';
import { UserService } from '@core/http/user.service';
import { UserComponent } from '@modules/home/pages/user/user.component';
import { CustomerService } from '@core/http/customer.service';
import { map } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
import { Authority } from '@shared/models/authority.enum';
import { CustomerId } from '@shared/models/id/customer-id';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { AddUserDialogComponent, AddUserDialogData } from '@modules/home/pages/user/add-user-dialog.component';
import { AuthState } from '@core/auth/auth.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthService } from '@core/auth/auth.service';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@modules/home/pages/user/activation-link-dialog.component';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TenantService } from '@app/core/http/tenant.service';
import { TenantId } from '@app/shared/models/id/tenant-id';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { resolveGroupParams } from '@shared/models/entity-group.models';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { UserTabsComponent } from '@home/pages/user/user-tabs.component';
import { UserTableHeaderComponent } from '@home/pages/user/user-table-header.component';
import { Customer } from '@shared/models/customer.model';
import { NULL_UUID } from '@shared/models/id/has-uuid';

export interface UsersTableRouteData {
  authority: Authority;
}

@Injectable()
export class UsersTableConfigResolver implements Resolve<EntityTableConfig<UserInfo | User>> {

  constructor(private allEntitiesTableConfigService: AllEntitiesTableConfigService<UserInfo | User>,
              private store: Store<AppState>,
              private userService: UserService,
              private authService: AuthService,
              private tenantService: TenantService,
              private customerService: CustomerService,
              private userPermissionsService: UserPermissionsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityTableConfig<UserInfo | User>> {
    const groupParams = resolveGroupParams(route);
    const tenantId = route.params.tenantId;
    const config = new EntityTableConfig<UserInfo | User>(groupParams);
    const authState = getCurrentAuthState(this.store);
    const authUser = authState.authUser;
    this.configDefaults(config, authUser);
    config.componentsData = {
      includeCustomers: true,
      displayIncludeCustomers: authUser.authority !== Authority.SYS_ADMIN,
      includeCustomersChanged: (includeCustomers: boolean) => {
        config.componentsData.includeCustomers = includeCustomers;
        config.columns = this.configureColumns(authUser, config);
        config.getTable().columnsUpdated();
        config.getTable().resetSortAndFilter(true);
      }
    };
    let titleObservable: Observable<string>;
    if (tenantId && authUser.authority === Authority.SYS_ADMIN) {
      titleObservable = this.tenantService.getTenant(tenantId).pipe(
        map((tenant) => tenant.title + ': ' + this.translate.instant('user.tenant-admins'))
      );
    } else {
      titleObservable = (config.customerId ?
        this.customerService.getCustomer(config.customerId) : of(null as Customer)).pipe(
          map((parentCustomer) => {
            if (parentCustomer) {
              return parentCustomer.title + ': ' + this.translate.instant('user.users');
            } else {
              return this.translate.instant('user.users');
            }
          }
        ));
    }
    return titleObservable.pipe(
      map((title) => {
        config.tableTitle = title;
        config.columns = this.configureColumns(authUser, config);
        this.configureEntityFunctions(authUser, config, tenantId);
        config.cellActionDescriptors = this.configureCellActions(authState, config);
        config.groupActionDescriptors = this.configureGroupActions(config);
        config.addActionDescriptors = this.configureAddActions(config);
        return this.allEntitiesTableConfigService.prepareConfiguration(config);
      })
    );
  }

  configDefaults(config: EntityTableConfig<UserInfo | User>, authUser: AuthUser, tenantId?: string) {
    config.entityType = EntityType.USER;
    config.entityComponent = UserComponent;
    config.entityTabsComponent = authUser.authority === Authority.SYS_ADMIN ? UserTabsComponent : GroupEntityTabsComponent<User>;
    config.entityTranslations = entityTypeTranslations.get(EntityType.USER);
    config.entityResources = entityTypeResources.get(EntityType.USER);

    config.entityTitle = (user) => user ? user.email : '';

    config.rowPointer = true;

    config.deleteEnabled = user => user && user.id && user.id.id !== authUser.userId;

    config.deleteEntityTitle = user => this.translate.instant('user.delete-user-title', { userEmail: user.email });
    config.deleteEntityContent = () => this.translate.instant('user.delete-user-text');
    config.deleteEntitiesTitle = count => this.translate.instant('user.delete-users-title', {count});
    config.deleteEntitiesContent = () => this.translate.instant('user.delete-users-text');

    config.loadEntity = id => this.userService.getUser(id.id);
    config.saveEntity = user => this.saveUser(authUser, config, user, tenantId);
    config.onEntityAction = action => this.onUserAction(action, config);
    config.addEntity = () => this.addUser(authUser, config, tenantId);
    config.headerComponent = UserTableHeaderComponent;
  }

  configureColumns(authUser: AuthUser, config: EntityTableConfig<UserInfo | User>): Array<EntityTableColumn<UserInfo>> {
    const columns: Array<EntityTableColumn<UserInfo>> = [
      new DateEntityTableColumn<UserInfo>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<User>('firstName', 'user.first-name', '25%'),
      new EntityTableColumn<User>('lastName', 'user.last-name', '25%'),
      new EntityTableColumn<User>('email', 'user.email', '25%')
    ];
    if (config.componentsData.includeCustomers) {
      const title = (authUser.authority === Authority.CUSTOMER_USER || config.customerId)
        ? 'entity.sub-customer-name' : 'entity.customer-name';
      columns.push(new EntityTableColumn<UserInfo>('ownerName', title, '25%'));
    }
    return columns;
  }

  configureEntityFunctions(authUser: AuthUser, config: EntityTableConfig<UserInfo | User>, tenantId?: string): void {
    if (tenantId && authUser.authority === Authority.SYS_ADMIN) {
      config.entitiesFetchFunction = pageLink =>
        this.userService.getTenantAdmins(tenantId, pageLink);
    } else {
      if (config.customerId) {
        config.entitiesFetchFunction = pageLink =>
          this.userService.getCustomerUserInfos(config.componentsData.includeCustomers,
            config.customerId, pageLink);
      } else {
        config.entitiesFetchFunction = pageLink =>
          this.userService.getAllUserInfos(config.componentsData.includeCustomers, pageLink);
      }
    }
    config.deleteEntity = id => this.userService.deleteUser(id.id);
  }

  configureCellActions(auth: AuthState, config: EntityTableConfig<UserInfo | User>): Array<CellActionDescriptor<UserInfo>> {
    const actions: Array<CellActionDescriptor<UserInfo>> = [];
    if (auth.userTokenAccessEnabled && this.userPermissionsService.hasGenericPermission(Resource.USER, Operation.IMPERSONATE)) {
      actions.push(
        {
          name: '',
          nameFunction: (user) => user.authority === Authority.TENANT_ADMIN ?
            this.translate.instant('user.login-as-tenant-admin') :
            this.translate.instant('user.login-as-customer-user'),
          mdiIcon: 'mdi:login',
          isEnabled: () => true,
          onAction: ($event, entity) => this.loginAsUser($event, entity)
        }
      );
    }
    return actions;
  }

  configureGroupActions(config: EntityTableConfig<UserInfo | User>): Array<GroupActionDescriptor<UserInfo>> {
    const actions: Array<GroupActionDescriptor<UserInfo>> = [];
    return actions;
  }

  configureAddActions(config: EntityTableConfig<UserInfo | User>): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    return actions;
  }

  private saveUser(authUser: AuthUser, config: EntityTableConfig<UserInfo | User>, user: User, tenantId?: string): Observable<User> {
    if (authUser.authority === Authority.SYS_ADMIN && tenantId) {
      user.tenantId = new TenantId(tenantId);
      user.customerId = new CustomerId(NULL_UUID);
      user.authority = Authority.TENANT_ADMIN;
    } else {
      user.tenantId = new TenantId(authUser.tenantId);
      if (authUser.authority === Authority.TENANT_ADMIN) {
        user.customerId = config.customerId ? new CustomerId(config.customerId) : new CustomerId(NULL_UUID);
        user.authority = config.customerId ? Authority.CUSTOMER_USER : Authority.TENANT_ADMIN;
      } else {
        user.customerId = config.customerId ? new CustomerId(config.customerId) : new CustomerId(authUser.customerId);
        user.authority = Authority.CUSTOMER_USER;
      }
    }
    return this.userService.saveUser(user);
  }

  private addUser(authUser: AuthUser, config: EntityTableConfig<UserInfo | User>, tenantId?: string): Observable<User> {
    if (authUser.authority !== Authority.SYS_ADMIN || !tenantId) {
      tenantId = authUser.tenantId;
    }
    let customerId: string = NULL_UUID;
    let authority = Authority.TENANT_ADMIN;
    if (authUser.authority === Authority.TENANT_ADMIN) {
      customerId = config.customerId ? config.customerId : NULL_UUID;
      authority = config.customerId ? Authority.CUSTOMER_USER : Authority.TENANT_ADMIN;
    } else if (authUser.authority === Authority.CUSTOMER_USER) {
      customerId = config.customerId ? config.customerId : authUser.customerId;
      authority = Authority.CUSTOMER_USER;
    }
    return this.dialog.open<AddUserDialogComponent, AddUserDialogData,
      User>(AddUserDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        tenantId,
        customerId,
        authority
      }
    }).afterClosed();
  }

  private openUser($event: Event, user: UserInfo, config: EntityTableConfig<UserInfo | User>) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree([user.id.id], {relativeTo: config.getActivatedRoute()});
    this.router.navigateByUrl(url);
  }

  private loginAsUser($event: Event, user: UserInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.authService.loginAsUser(user.id.id).subscribe();
  }

  private displayActivationLink($event: Event, user: UserInfo) {
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

  private resendActivation($event: Event, user: UserInfo) {
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

  private setUserCredentialsEnabled($event: Event, user: UserInfo, userCredentialsEnabled: boolean) {
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

  onUserAction(action: EntityAction<User>, config: EntityTableConfig<User>): boolean {
    switch (action.action) {
      case 'open':
        this.openUser(action.event, action.entity, config);
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
