///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { PageLink } from '@shared/models/page/page-link';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { forkJoin, fromEvent, merge, Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityType } from '@shared/models/entity-type.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { GroupPermissionsDatasource } from '@home/models/datasource/group-permissions.datasource';
import { RoleService } from '@core/http/role.service';
import { GroupPermissionFullInfo } from '@shared/models/group-permission.models';
import { deepClone } from '@core/utils';
import {
  GroupPermissionDialogComponent,
  GroupPermissionDialogData
} from '@home/components/role/group-permission-dialog.component';
import { ViewRoleDialogComponent, ViewRoleDialogData } from '@home/components/role/view-role-dialog.component';

@Component({
  selector: 'tb-group-permissions',
  templateUrl: './group-permissions.component.html',
  styleUrls: ['./group-permissions.component.scss']
})
export class GroupPermissionsComponent extends PageComponent implements AfterViewInit, OnInit {

  displayedColumns: string[] = [];

  editEnabled = false;
  addEnabled = false;
  deleteEnabled = false;

  isUserGroup = false;

  pageLink: PageLink;
  textSearchMode = false;
  dataSource: GroupPermissionsDatasource;

  activeValue = false;
  dirtyValue = false;
  entityGroupValue: EntityGroupInfo;

  viewsInited = false;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        if (this.viewsInited) {
          this.updateData(true);
        }
      }
    }
  }

  @Input()
  set entityGroup(entityGroup: EntityGroupInfo) {
    if (this.entityGroupValue !== entityGroup) {
      this.entityGroupValue = entityGroup;
      this.entityGroupUpdated(entityGroup);
      if (this.viewsInited) {
        this.resetSortAndFilter(this.activeValue);
        if (!this.activeValue) {
          this.dirtyValue = true;
        }
      }
    }
  }

  get entityGroup(): EntityGroupInfo {
    return this.entityGroupValue;
  }

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }

  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(protected store: Store<AppState>,
              private roleService: RoleService,
              public translate: TranslateService,
              public dialog: MatDialog,
              private userPermissionsService: UserPermissionsService,
              private dialogService: DialogService) {
    super(store);
    this.dirtyValue = !this.activeValue;
    const sortOrder: SortOrder = { property: 'roleName', direction: Direction.ASC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new GroupPermissionsDatasource(this.roleService, this.translate);
  }

  ngOnInit() {
  }

  private entityGroupUpdated(entityGroup: EntityGroupInfo) {
    this.editEnabled = false;
    this.addEnabled = false;
    this.deleteEnabled = false;
    this.isUserGroup = entityGroup.type === EntityType.USER;
    const readOnlyGroup = !this.userPermissionsService.hasEntityGroupPermission(Operation.WRITE, entityGroup);
    if (!readOnlyGroup) {
      this.editEnabled = this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.WRITE);
      this.addEnabled = this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.CREATE);
      this.deleteEnabled = this.userPermissionsService.hasGenericPermission(Resource.GROUP_PERMISSION, Operation.DELETE);
    }
    this.updateColumns();
  }

  updateColumns() {
    this.displayedColumns = [];
    if (this.deleteEnabled) {
      this.displayedColumns.push('select');
    }
    this.displayedColumns.push('roleName');
    if (this.isUserGroup) {
      this.displayedColumns.push('roleTypeName', 'entityGroupTypeName', 'entityGroupName', 'entityGroupOwnerFullName');
    } else {
      this.displayedColumns.push('userGroupName', 'userGroupOwnerFullName');
    }
    this.displayedColumns.push('actions');
  }

  ngAfterViewInit() {

    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.paginator.pageIndex = 0;
          this.updateData();
        })
      )
      .subscribe();

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();

    this.viewsInited = true;
    if (this.activeValue && this.entityGroupValue) {
      this.updateData(true);
    }
  }

  updateData(reload: boolean = false) {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadGroupPermissions(this.entityGroupValue, this.pageLink, reload);
  }

  enterFilterMode() {
    this.textSearchMode = true;
    this.pageLink.textSearch = '';
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    this.updateData();
  }

  resetSortAndFilter(update: boolean = true) {
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    const sortable = this.sort.sortables.get('roleName');
    this.sort.active = sortable.id;
    this.sort.direction = 'asc';
    if (update) {
      this.updateData(true);
    }
  }

  reloadGroupPermissions() {
    this.updateData(true);
  }

  addGroupPermission($event: Event) {
    this.openGroupPermissionDialog($event);
  }

  editGroupPermission($event: Event, groupPermission: GroupPermissionFullInfo) {
    this.openGroupPermissionDialog($event, groupPermission);
  }

  deleteGroupPermission($event: Event, groupPermission: GroupPermissionFullInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('group-permission.delete-group-permission-title', {roleName: groupPermission.roleName});
    const content = this.translate.instant('group-permission.delete-group-permission-text');

    this.dialogService.confirm(
      title,
      content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.roleService.deleteGroupPermission(groupPermission.id.id).subscribe(
          () => {
            this.reloadGroupPermissions();
          }
        );
      }
    });
  }

  deleteGroupPermissions($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.dataSource.selection.selected.length > 0) {
      const title = this.translate.instant('group-permission.delete-group-permissions-title',
        {count: this.dataSource.selection.selected.length});
      const content = this.translate.instant('group-permission.delete-group-permissions-text');

      this.dialogService.confirm(
        title,
        content,
        this.translate.instant('action.no'),
        this.translate.instant('action.yes'),
        true
      ).subscribe((result) => {
        if (result) {
          const tasks: Observable<any>[] = [];
          this.dataSource.selection.selected.forEach((groupPermission) => {
            tasks.push(this.roleService.deleteGroupPermission(groupPermission.id.id));
          });
          forkJoin(tasks).subscribe(
            () => {
              this.reloadGroupPermissions();
            }
          );
        }
      });
    }
  }

  openGroupPermissionDialog($event: Event, groupPermission: GroupPermissionFullInfo = null) {
    if ($event) {
      $event.stopPropagation();
    }
    let isAdd = false;
    if (!groupPermission) {
      isAdd = true;
      groupPermission = {} as GroupPermissionFullInfo;
      if (this.isUserGroup) {
        groupPermission.userGroupId = {
          entityType: EntityType.ENTITY_GROUP,
          id: this.entityGroup.id.id
        };
      } else {
        groupPermission.entityGroupId = {
          entityType: EntityType.ENTITY_GROUP,
          id: this.entityGroup.id.id
        };
        groupPermission.entityGroupType = this.entityGroup.type;
      }
    } else {
      groupPermission = deepClone(groupPermission);
    }
    this.dialog.open<GroupPermissionDialogComponent, GroupPermissionDialogData, boolean>(GroupPermissionDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        isUserGroup: this.isUserGroup,
        groupPermission
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.reloadGroupPermissions();
        }
      }
    );
  }

  viewRole($event: Event, groupPermission: GroupPermissionFullInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ViewRoleDialogComponent, ViewRoleDialogData>(ViewRoleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        role: groupPermission.role
      }
    })
  }

}
