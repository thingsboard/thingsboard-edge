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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { ImageService } from '@core/http/image.service';
import { ImageResourceInfo, imageResourceType } from '@shared/models/resource.models';
import {
  UploadImageDialogComponent,
  UploadImageDialogData
} from '@shared/components/image/upload-image-dialog.component';
import { UrlHolder } from '@shared/pipe/image.pipe';
import { ImportExportService } from '@shared/import-export/import-export.service';

export interface ImageDialogData {
  readonly: boolean;
  image: ImageResourceInfo;
}

@Component({
  selector: 'tb-image-dialog',
  templateUrl: './image-dialog.component.html',
  styleUrls: ['./image-dialog.component.scss']
})
export class ImageDialogComponent extends
  DialogComponent<ImageDialogComponent, ImageResourceInfo> implements OnInit {

  image: ImageResourceInfo;

  readonly: boolean;

  imageFormGroup: UntypedFormGroup;

  imageChanged = false;

  imagePreviewData: UrlHolder;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private imageService: ImageService,
              private dialog: MatDialog,
              private importExportService: ImportExportService,
              @Inject(MAT_DIALOG_DATA) private data: ImageDialogData,
              public dialogRef: MatDialogRef<ImageDialogComponent, ImageResourceInfo>,
              public fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.image = data.image;
    this.readonly = data.readonly;
    this.imagePreviewData = {
      url: this.image.link
    };
  }

  ngOnInit(): void {
    this.imageFormGroup = this.fb.group({
      title: [this.image.title, [Validators.required]],
      link: [this.image.link, []],
    });
    if (this.data.readonly) {
      this.imageFormGroup.disable();
    } else {
      this.imageFormGroup.get('link').disable();
    }
  }

  cancel(): void {
    this.dialogRef.close(this.imageChanged ? this.image : null);
  }

  revertInfo(): void {
    this.imageFormGroup.get('title').setValue(this.image.title);
    this.imageFormGroup.markAsPristine();
  }

  saveInfo(): void {
    const title: string = this.imageFormGroup.get('title').value;
    const image = {...this.image, ...{title}};
    this.imageService.updateImageInfo(image).subscribe(
      (saved) => {
        this.image = saved;
        this.imageChanged = true;
        this.imageFormGroup.markAsPristine();
      }
    );
  }

  downloadImage($event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.imageService.downloadImage(imageResourceType(this.image), this.image.resourceKey).subscribe();
  }

  exportImage($event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExportService.exportImage(imageResourceType(this.image), this.image.resourceKey);
  }

  updateImage($event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<UploadImageDialogComponent, UploadImageDialogData,
      ImageResourceInfo>(UploadImageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        image: this.image
      }
    }).afterClosed().subscribe((result) => {
      if (result) {
        this.imageChanged = true;
        this.image = result;
        this.imagePreviewData = {
          url: this.image.link
        };
      }
    });
  }

}
