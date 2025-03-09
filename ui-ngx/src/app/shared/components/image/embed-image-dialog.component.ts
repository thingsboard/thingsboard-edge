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

import { ImageResourceInfo } from '@shared/models/resource.models';
import { Component, DestroyRef, Inject, OnInit } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { ImageService } from '@core/http/image.service';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, UntypedFormBuilder } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface EmbedImageDialogData {
  readonly: boolean;
  image: ImageResourceInfo;
}

@Component({
  selector: 'tb-embed-image-dialog',
  templateUrl: './embed-image-dialog.component.html',
  styleUrls: ['./embed-image-dialog.component.scss']
})
export class EmbedImageDialogComponent extends
  DialogComponent<EmbedImageDialogComponent, ImageResourceInfo> implements OnInit {

  image = this.data.image;

  readonly = this.data.readonly;

  imageChanged = false;

  publicStatusControl = new FormControl(this.image.public);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private imageService: ImageService,
              @Inject(MAT_DIALOG_DATA) private data: EmbedImageDialogData,
              public dialogRef: MatDialogRef<EmbedImageDialogComponent, ImageResourceInfo>,
              public fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    if (!this.readonly) {
      this.publicStatusControl.valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(
        (isPublic) => {
          this.updateImagePublicStatus(isPublic);
        }
      );
    }
  }

  cancel(): void {
    this.dialogRef.close(this.imageChanged ? this.image : null);
  }

  embedToHtmlCode(): string {
    return '```html\n' +
      '<img src="'+this.image.publicLink+'" alt="'+this.image.title.replace(/"/g, '&quot;')+'" />' +
      '{:copy-code}\n' +
      '```';
  }

  embedToAngularTemplateCode(): string {
    return '```html\n' +
      '<img [src]="\''+this.image.link+'\' | image | async" />' +
      '{:copy-code}\n' +
      '```';
  }

  private updateImagePublicStatus(isPublic: boolean): void {
    this.imageService.updateImagePublicStatus(this.image, isPublic).subscribe(
      (image) => {
        this.image = image;
        this.imageChanged = true;
      }
    );
  }

}
