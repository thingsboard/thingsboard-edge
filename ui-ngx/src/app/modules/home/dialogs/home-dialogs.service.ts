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
import { MatDialog } from '@angular/material/dialog';
import { EntityType } from '@shared/models/entity-type.models';
import { forkJoin, Observable, of } from 'rxjs';
import {
  ImportDialogCsvComponent,
  ImportDialogCsvData
} from '@home/components/import-export/import-dialog-csv.component';
import { CustomerId } from '@shared/models/id/customer-id';
import { DialogService } from '@core/services/dialog.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { EntityGroup, EntityGroupInfo } from '@shared/models/entity-group.models';
import { TranslateService } from '@ngx-translate/core';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { EntityId } from '@shared/models/id/entity-id';
import { SelectOwnerDialogComponent, SelectOwnerDialogData } from '@home/dialogs/select-owner-dialog.component';
import {
  SelectEntityGroupDialogComponent,
  SelectEntityGroupDialogData,
  SelectEntityGroupDialogResult
} from '@home/dialogs/select-entity-group-dialog.component';
import {
  ShareEntityGroupDialogComponent,
  ShareEntityGroupDialogData
} from '@home/dialogs/share-entity-group-dialog.component';
import { GroupEntityInfo } from '@shared/models/base-data';
import {
  ManageOwnerAndGroupsDialogComponent,
  ManageOwnerAndGroupsDialogData
} from '@home/components/group/manage-owner-and-groups-dialog.component';
import {
  EntityGroupWizardDialogComponent,
  EntityGroupWizardDialogData,
  EntityGroupWizardDialogResult
} from '@home/components/wizard/entity-group-wizard-dialog.component';

@Injectable()
export class HomeDialogsService {
  constructor(
    private dialog: MatDialog,
    private translate: TranslateService,
    private dialogService: DialogService,
    private entityGroupService: EntityGroupService
  ) {
  }

  confirm(title: string, message: string,
          cancel: string = this.translate.instant('action.no'),
          ok: string = this.translate.instant('action.yes'),
          fullscreen: boolean = true): Observable<boolean> {
    return this.dialogService.confirm(
      title,
      message,
      cancel,
      ok,
      fullscreen
    );
  }

  public importEntities(customerId: CustomerId, entityType: EntityType, entityGroupId: string): Observable<boolean> {
    switch (entityType) {
      case EntityType.DEVICE:
        return this.openImportDialogCSV(customerId, entityType, entityGroupId, 'device.import', 'device.device-file');
      case EntityType.ASSET:
        return this.openImportDialogCSV(customerId, entityType, entityGroupId, 'asset.import', 'asset.asset-file');
      case EntityType.EDGE:
        return this.openImportDialogCSV(customerId, entityType, entityGroupId, 'edge.import', 'edge.edge-file');
    }
  }

  public shareEntityGroup($event: Event, entityGroup: EntityGroupInfo): Observable<boolean> {
    if ($event) {
      $event.stopPropagation();
    }
    return this.dialog.open<ShareEntityGroupDialogComponent, ShareEntityGroupDialogData,
      boolean>(ShareEntityGroupDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityGroupId: entityGroup.id
      }
    }).afterClosed();
  }

  public makeEntityGroupPublic($event: Event, entityGroup: EntityGroupInfo): Observable<boolean> {
    const title = this.translate.instant('entity-group.make-public-entity-group-title',
      {entityGroupName: entityGroup.name});
    const content = this.translate.instant('entity-group.make-public-entity-group-text');
    return this.confirm(
      title,
      content).pipe(
      mergeMap((res) => {
        if (res) {
          return this.entityGroupService.makeEntityGroupPublic(entityGroup.id.id)
            .pipe(map(() => res));
        } else {
          return of(res);
        }
      }
    ));
  }

  public makeEntityGroupPrivate($event: Event, entityGroup: EntityGroupInfo): Observable<boolean> {
    const title = this.translate.instant('entity-group.make-private-entity-group-title',
      {entityGroupName: entityGroup.name});
    const content = this.translate.instant('entity-group.make-private-entity-group-text');
    return this.dialogService.confirm(
      title,
      content).pipe(
      mergeMap((res) => {
          if (res) {
            return this.entityGroupService.makeEntityGroupPrivate(entityGroup.id.id)
              .pipe(map(() => res));
          } else {
            return of(res);
          }
        }
      ));
  }

  public manageOwnerAndGroups($event: Event, groupEntity: GroupEntityInfo<EntityId>): Observable<boolean> {
    if ($event) {
      $event.stopPropagation();
    }
    return this.dialog.open<ManageOwnerAndGroupsDialogComponent, ManageOwnerAndGroupsDialogData,
      boolean>(ManageOwnerAndGroupsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        groupEntity
      }
    }).afterClosed();
  }

  public selectOwner($event: Event, selectOwnerTitle: string, confirmSelectTitle: string,
                     placeholderText: string, notFoundText: string,  requiredText: string,
                     onOwnerSelected?: (targetOwnerId: EntityId) => Observable<boolean>,
                     excludeOwnerIds?: Array<string>): Observable<EntityId> {
    if ($event) {
      $event.stopPropagation();
    }
    return this.dialog.open<SelectOwnerDialogComponent, SelectOwnerDialogData,
      EntityId>(SelectOwnerDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        selectOwnerTitle,
        confirmSelectTitle,
        placeholderText,
        notFoundText,
        requiredText,
        onOwnerSelected,
        excludeOwnerIds
      }
    }).afterClosed();
  }

  public selectEntityGroup($event: Event, ownerId: EntityId, targetGroupType: EntityType,
                           selectEntityGroupTitle: string, confirmSelectTitle: string,
                           placeholderText: string, notFoundText: string, requiredText: string,
                           onEntityGroupSelected?: (result: SelectEntityGroupDialogResult) => Observable<boolean>,
                           excludeGroupIds?: Array<string>): Observable<SelectEntityGroupDialogResult> {
    if ($event) {
      $event.stopPropagation();
    }
    return this.dialog.open<SelectEntityGroupDialogComponent, SelectEntityGroupDialogData,
      SelectEntityGroupDialogResult>(SelectEntityGroupDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        ownerId,
        targetGroupType,
        selectEntityGroupTitle,
        confirmSelectTitle,
        placeholderText,
        notFoundText,
        requiredText,
        onEntityGroupSelected,
        excludeGroupIds
      }
    }).afterClosed();
  }

  public createEntityGroup(groupType: EntityType, groupName?: string, ownerId?: EntityId): Observable<EntityGroupWizardDialogResult> {
    return this.dialog.open<EntityGroupWizardDialogComponent, EntityGroupWizardDialogData,
      EntityGroupWizardDialogResult>(EntityGroupWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        ownerId,
        groupName,
        groupType
      }
    }).afterClosed();
  }

  private openImportDialogCSV(customerId: CustomerId, entityType: EntityType,
                              entityGroupId: string, importTitle: string, importFileLabel: string): Observable<boolean> {
    return this.dialog.open<ImportDialogCsvComponent, ImportDialogCsvData,
      any>(ImportDialogCsvComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entityType,
        importTitle,
        importFileLabel,
        customerId,
        entityGroupId
      }
    }).afterClosed();
  }

  public unassignEntityGroupFromEdge($event: Event, entityGroup: EntityGroup, edgeId: string): Observable<boolean> {
    const title = this.translate.instant('edge.unassign-entity-group-from-edge-title',
      {entityGroupName: entityGroup.name});
    const content = this.translate.instant('edge.unassign-entity-group-from-edge-text');
    return this.dialogService.confirm(
      title,
      content).pipe(
      mergeMap((res) => {
          if (res) {
            return this.entityGroupService.unassignEntityGroupFromEdge(edgeId, entityGroup.id.id, entityGroup.type).pipe(
              map(() => res)
            );
          } else {
            return of(res);
          }
        }
      ));
  }

  public unassignEntityGroupsFromEdge($event: Event, entityGroups: Array<EntityGroup>, edgeId: string): Observable<boolean> {
    const title = this.translate.instant('edge.unassign-entity-groups-from-edge-title', { count: entityGroups.length });
    const content = this.translate.instant('edge.unassign-entity-groups-from-edge-text');
    const tasks: Array<Observable<EntityGroup>> = [];
    entityGroups.forEach((entityGroup) => {
      tasks.push(this.entityGroupService.unassignEntityGroupFromEdge(edgeId, entityGroup.id.id, entityGroup.type).pipe(
        map((e) => e),
        catchError(() => of(null)
        )));
    });
    return this.dialogService.confirm(
      title,
      content).pipe(
      mergeMap((res) => {
          if (res) {
            return forkJoin(tasks).pipe(
              map(() => res)
            );
          } else {
            return of(res);
          }
        }
      ));
  }

  // const tasks: Observable<Array<EntityGroup>>[] = [];
  // entities.forEach((entity) => {
  //   tasks.push(this.entityGroupService.unassignEntityGroupFromEdge(this.edgeId, entity.id.id, entity.type));
  // });
  // forkJoin(tasks).subscribe(
  //   () => {
  //     this.table.updateData();
  //   }
  // );

}
