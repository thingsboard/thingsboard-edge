///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SharedModule } from '@shared/shared.module';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { CommonModule } from '@angular/common';
import { entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityInfoData } from '@shared/models/entity.models';

interface EntityConflictDialogData {
  message: string;
  entity: EntityInfoData;
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
  readonly entityTypeTranslations = entityTypeTranslations;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: EntityConflictDialogData,
    private dialogRef: MatDialogRef<EntityConflictDialogComponent>,
    private importExportService: ImportExportService,
  ) {}

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
