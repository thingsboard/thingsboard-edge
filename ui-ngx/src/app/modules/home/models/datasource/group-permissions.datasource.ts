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

import { CollectionViewer, DataSource, SelectionModel } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { PageLink } from '@shared/models/page/page-link';
import { catchError, map, publishReplay, refCount, take, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import {
  GroupPermission,
  GroupPermissionFullInfo,
  isGroupPermissionsEqual
} from '@shared/models/group-permission.models';
import { RoleService } from '@core/http/role.service';
import { EntityGroupInfo } from '@shared/models/entity-group.models';

export class GroupPermissionsDatasource implements DataSource<GroupPermissionFullInfo> {

  private groupPermissionsSubject = new BehaviorSubject<GroupPermissionFullInfo[]>([]);
  private pageDataSubject = new BehaviorSubject<PageData<GroupPermissionFullInfo>>(emptyPageData<GroupPermissionFullInfo>());

  public pageData$ = this.pageDataSubject.asObservable();

  public selection = new SelectionModel<GroupPermissionFullInfo>(true, []);

  private allGroupPermissions: Observable<Array<GroupPermissionFullInfo>>;

  constructor(private roleService: RoleService,
              private translate: TranslateService) {}

  connect(collectionViewer: CollectionViewer): Observable<GroupPermissionFullInfo[] | ReadonlyArray<GroupPermissionFullInfo>> {
    return this.groupPermissionsSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.groupPermissionsSubject.complete();
    this.pageDataSubject.complete();
  }

  loadGroupPermissions(entityGroup: EntityGroupInfo,
                       registrationPermissions: Array<GroupPermission>,
                       pageLink: PageLink, reload: boolean = false): Observable<PageData<GroupPermissionFullInfo>> {
    if (reload) {
      this.allGroupPermissions = null;
    }
    const result = new ReplaySubject<PageData<GroupPermissionFullInfo>>();
    this.fetchGroupPermissions(entityGroup, registrationPermissions, pageLink).pipe(
      tap(() => {
        this.selection.clear();
      }),
      catchError(() => of(emptyPageData<GroupPermissionFullInfo>())),
    ).subscribe(
      (pageData) => {
        this.groupPermissionsSubject.next(pageData.data);
        this.pageDataSubject.next(pageData);
        result.next(pageData);
      }
    );
    return result;
  }

  fetchGroupPermissions(entityGroup: EntityGroupInfo,
                        registrationPermissions: Array<GroupPermission>,
                        pageLink: PageLink): Observable<PageData<GroupPermissionFullInfo>> {
    return this.getAllGroupPermissions(entityGroup, registrationPermissions).pipe(
      map((data) => pageLink.filterData(data))
    );
  }

  getAllGroupPermissions(entityGroup: EntityGroupInfo,
                         registrationPermissions: Array<GroupPermission>): Observable<Array<GroupPermissionFullInfo>> {
    if (!this.allGroupPermissions) {
      let groupPermissionsObservable: Observable<Array<GroupPermissionFullInfo>>;
      let isUserGroup: boolean;
      if (entityGroup) {
        isUserGroup = entityGroup.type === EntityType.USER;
        if (isUserGroup) {
          groupPermissionsObservable = this.roleService.getUserGroupPermissions(entityGroup.id.id);
        } else {
          groupPermissionsObservable = this.roleService.getEntityGroupPermissions(entityGroup.id.id);
        }
      } else {
        isUserGroup = true;
        if (registrationPermissions && registrationPermissions.length) {
          groupPermissionsObservable = this.roleService.loadUserGroupPermissionInfos(registrationPermissions);
        } else {
          groupPermissionsObservable = of([]);
        }
      }
      this.allGroupPermissions = groupPermissionsObservable.pipe(
        map(groupPermissions => {
          for (const groupPermission of  groupPermissions) {
            if (registrationPermissions) {
              groupPermission.sourceGroupPermission = registrationPermissions.find(rp => isGroupPermissionsEqual(groupPermission, rp));
            }
            groupPermission.roleName = groupPermission.role.name;
            if (isUserGroup) {
              groupPermission.roleTypeName = this.translate.instant(`role.display-type.${groupPermission.role.type}`);
              if (groupPermission.entityGroupType) {
                groupPermission.entityGroupTypeName =
                  this.translate.instant(entityTypeTranslations.get(groupPermission.entityGroupType).typePlural);
              } else {
                groupPermission.entityGroupTypeName = '';
              }
              if (groupPermission.entityGroupOwnerId) {
                let ownerName = groupPermission.entityGroupOwnerName;
                const ownerType = groupPermission.entityGroupOwnerId.entityType;
                ownerName += ` (${this.translate.instant(entityTypeTranslations.get(ownerType).type)})`;
                groupPermission.entityGroupOwnerFullName = ownerName;
              } else {
                groupPermission.entityGroupOwnerFullName = '';
              }
            } else {
              let userGroupOwnerFullName = groupPermission.userGroupOwnerName;
              const userGroupOwnerType = groupPermission.userGroupOwnerId.entityType;
              userGroupOwnerFullName += ` (${this.translate.instant(entityTypeTranslations.get(userGroupOwnerType).type)})`;
              groupPermission.userGroupOwnerFullName = userGroupOwnerFullName;
            }
          }
          return groupPermissions;
        }),
        publishReplay(1),
        refCount()
      );
    }
    return this.allGroupPermissions;
  }

  isAllSelected(): Observable<boolean> {
    const numSelected = this.selection.selected.length;
    return this.groupPermissionsSubject.pipe(
      map((groupPermissions) => numSelected === this.selectableGroupPermissionsCount(groupPermissions))
    );
  }

  isEmpty(): Observable<boolean> {
    return this.groupPermissionsSubject.pipe(
      map((groupPermissions) => !groupPermissions.length)
    );
  }

  total(): Observable<number> {
    return this.pageDataSubject.pipe(
      map((pageData) => pageData.totalElements)
    );
  }

  masterToggle() {
    this.groupPermissionsSubject.pipe(
      tap((groupPermissions) => {
        const numSelected = this.selection.selected.length;
        if (numSelected === this.selectableGroupPermissionsCount(groupPermissions)) {
          this.selection.clear();
        } else {
          groupPermissions.forEach(row => {
            if (!row.readOnly) {
              this.selection.select(row);
            }
          });
        }
      }),
      take(1)
    ).subscribe();
  }

  private selectableGroupPermissionsCount(groupPermissions: Array<GroupPermissionFullInfo>): number {
    return groupPermissions.filter((groupPermission) => !groupPermission.readOnly).length;
  }
}
