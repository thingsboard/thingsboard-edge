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

import { BaseData } from '@shared/models/base-data';
import { Injectable } from '@angular/core';
import { EntityGroupService } from '@core/http/entity-group.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { defaultEntityTablePermissions, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { Operation } from '@shared/models/security.models';
import { TranslateService } from '@ngx-translate/core';
import { EntityId } from '@shared/models/id/entity-id';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, mergeMap } from 'rxjs/operators';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';
import { MatDialog } from '@angular/material/dialog';

@Injectable()
export class AllEntitiesTableConfigService<T extends BaseData<EntityId>> {

  constructor(private entityGroupService: EntityGroupService,
              private userPermissionsService: UserPermissionsService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private dialog: MatDialog) {
  }

  prepareConfiguration(config: EntityTableConfig<T>): EntityTableConfig<T> {
    defaultEntityTablePermissions(this.userPermissionsService, config);
    if (this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.CHANGE_OWNER, config.entityType)) {
      config.groupActionDescriptors.push(
        {
          name: this.translate.instant('entity-group.change-owner'),
          icon: 'assignment_ind',
          isEnabled: true,
          onAction: ($event, entities) => {
            this.changeEntitiesOwner($event, entities, config);
          }
        }
      );
    }
    if (!config.addEntity) {
      config.addEntity = () => this.addGroupEntity(config);
    }
    return config;
  }

  private addGroupEntity(config: EntityTableConfig<T>): Observable<T> {
    return this.dialog.open<AddGroupEntityDialogComponent, AddGroupEntityDialogData<T>,
      T>(AddGroupEntityDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed();
  }

  private changeEntitiesOwner($event: MouseEvent, entities: T[],
                              config: EntityTableConfig<T>) {
    const ignoreErrors = entities.length > 1;
    const onOwnerSelected = (targetOwnerId: EntityId) => this.homeDialogs.confirm(
      this.translate.instant('entity-group.confirm-change-owner-title', {count: entities.length}),
      this.translate.instant('entity-group.confirm-change-owner-text')).pipe(
      mergeMap((res) => {
        if (res) {
          const changeOwnerObservables: Observable<any>[] = [];
          entities.forEach((entity) => {
            changeOwnerObservables.push(
              this.entityGroupService.changeEntityOwner(targetOwnerId, entity.id, null, {ignoreErrors}).pipe(
                catchError((err) => {
                  if (ignoreErrors) {
                    return of(null);
                  } else {
                    throw err;
                  }
                })
              )
            );
          });
          return forkJoin(changeOwnerObservables).pipe(
            mergeMap(() => of(true)),
            catchError((err) => {
              if (ignoreErrors) {
                return of(true);
              } else {
                throw err;
              }
            })
          );
        } else {
          return of(false);
        }
      })
    );
    let excludeOwnerIds;
    const uniqueOwnerIds = [...new Set(entities.map(e => e.ownerId?.id))];
    if (uniqueOwnerIds.length > 1) {
      excludeOwnerIds = [];
    } else {
      excludeOwnerIds = uniqueOwnerIds;
    }
    this.homeDialogs.selectOwner($event, 'entity-group.change-owner', 'entity-group.change-owner',
      'entity-group.select-target-owner',
      'entity-group.no-owners-matching',
      'entity-group.target-owner-required', onOwnerSelected,
      excludeOwnerIds).subscribe(
      (targetOwnerId) => {
        if (targetOwnerId) {
          config.updateData();
        }
      }
    );
  }
}
