///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SharedModule } from '@shared/shared.module';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { CommonModule } from '@angular/common';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityInfoData, VersionedEntity } from '@shared/models/entity.models';
import { EntityId } from '@shared/models/id/entity-id';
import { RuleChainMetaData } from '@shared/models/rule-chain.models';

interface EntityConflictDialogData {
  message: string;
  entity: VersionedEntity;
}

@Component({
  selector: 'tb-entity-conflict-dialog',
  templateUrl: 'entity-conflict-dialog.component.html',
  styleUrls: ['./entity-conflict-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
  ],
})
export class EntityConflictDialogComponent {

  entityId: EntityId;
  entityTypeLabel: string;

  readonly entityTypeTranslations = entityTypeTranslations;
  private readonly defaultEntityLabel = 'entity.entity';

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: EntityConflictDialogData,
    private dialogRef: MatDialogRef<EntityConflictDialogComponent>,
    private importExportService: ImportExportService,
  ) {
    this.entityId = (data.entity as EntityInfoData).id ?? (data.entity as RuleChainMetaData).ruleChainId;
    this.entityTypeLabel = entityTypeTranslations.has(this.entityId.entityType)
      ? (entityTypeTranslations.get(this.entityId.entityType).type)
      : this.defaultEntityLabel;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onDiscard(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onLinkClick(event: MouseEvent): void {
    event.preventDefault();
    this.importExportService.exportEntity(this.data.entity);
  }
}
