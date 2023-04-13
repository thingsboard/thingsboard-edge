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

import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DocumentationLink, DocumentationLinks } from '@shared/models/user-settings.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { UserSettingsService } from '@core/http/user-settings.service';

export interface EditDocLinksDialogData {
  docLinks: DocumentationLinks;
}

@Component({
  selector: 'tb-edit-doc-links-dialog',
  templateUrl: './edit-doc-links-dialog.component.html',
  styleUrls: ['./edit-doc-links-dialog.component.scss']
})
export class EditDocLinksDialogComponent extends
  DialogComponent<EditDocLinksDialogComponent, boolean> implements OnInit {

  updated = false;
  addMode = false;
  editMode = false;

  docLinks = this.data.docLinks;
  addingDocLink: Partial<DocumentationLink>;

  editDocLinksFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EditDocLinksDialogData,
              public dialogRef: MatDialogRef<EditDocLinksDialogComponent, boolean>,
              public fb: UntypedFormBuilder,
              private userSettingsService: UserSettingsService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    const docLinksControls: Array<AbstractControl> = [];
    for (const docLink of this.docLinks.links) {
      docLinksControls.push(this.fb.control(docLink, [Validators.required]));
    }
    this.editDocLinksFormGroup = this.fb.group({
      links: this.fb.array(docLinksControls)
    });
  }

  docLinksFormArray(): UntypedFormArray {
    return this.editDocLinksFormGroup.get('links') as UntypedFormArray;
  }

  trackByDocLink(index: number, docLinkControl: AbstractControl): any {
    return docLinkControl;
  }

  docLinkDrop(event: CdkDragDrop<string[]>) {
    const docLinksArray = this.editDocLinksFormGroup.get('links') as UntypedFormArray;
    const docLink = docLinksArray.at(event.previousIndex);
    docLinksArray.removeAt(event.previousIndex);
    docLinksArray.insert(event.currentIndex, docLink);
    this.update();
  }

  addLink() {
    this.addingDocLink = { icon: 'notifications' };
    this.addMode = true;
  }

  linkAdded(docLink: DocumentationLink) {
    this.addMode = false;
    const docLinksArray = this.editDocLinksFormGroup.get('links') as UntypedFormArray;
    const docLinkControl = this.fb.control(docLink, [Validators.required]);
    docLinksArray.push(docLinkControl);
    this.update();
  }

  deleteLink(index: number) {
    (this.editDocLinksFormGroup.get('links') as UntypedFormArray).removeAt(index);
    this.update();
  }

  update() {
    if (this.editDocLinksFormGroup.valid) {
      const docLinks: DocumentationLinks = this.editDocLinksFormGroup.value;
      this.userSettingsService.updateDocumentationLinks(docLinks).subscribe(() => {
        this.updated = true;
      });
    }
  }

  close(): void {
    this.dialogRef.close(this.updated);
  }
}
