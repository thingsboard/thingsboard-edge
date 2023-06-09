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
import { DocumentationLink, DocumentationLinks, QuickLinks } from '@shared/models/user-settings.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { UserSettingsService } from '@core/http/user-settings.service';
import { Observable } from 'rxjs';

export interface EditLinksDialogData {
  mode: 'docs' | 'quickLinks';
  links: DocumentationLinks | QuickLinks;
}

@Component({
  selector: 'tb-edit-links-dialog',
  templateUrl: './edit-links-dialog.component.html',
  styleUrls: ['./edit-links-dialog.component.scss']
})
export class EditLinksDialogComponent extends
  DialogComponent<EditLinksDialogComponent, boolean> implements OnInit {

  updated = false;
  addMode = false;
  editMode = false;

  links = this.data.links;
  mode = this.data.mode;
  addingLink: Partial<DocumentationLink> | string;

  editLinksFormGroup: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EditLinksDialogData,
              public dialogRef: MatDialogRef<EditLinksDialogComponent, boolean>,
              public fb: UntypedFormBuilder,
              private userSettingsService: UserSettingsService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    const linksControls: Array<AbstractControl> = [];
    for (const link of this.links.links) {
      linksControls.push(this.fb.control(link, [Validators.required]));
    }
    this.editLinksFormGroup = this.fb.group({
      links: this.fb.array(linksControls)
    });
  }

  linksFormArray(): UntypedFormArray {
    return this.editLinksFormGroup.get('links') as UntypedFormArray;
  }

  trackByLink(index: number, linkControl: AbstractControl): any {
    return linkControl;
  }

  linkDrop(event: CdkDragDrop<string[]>) {
    const linksArray = this.editLinksFormGroup.get('links') as UntypedFormArray;
    const link = linksArray.at(event.previousIndex);
    linksArray.removeAt(event.previousIndex);
    linksArray.insert(event.currentIndex, link);
    this.update();
  }

  addLink() {
    this.addingLink = this.mode === 'docs' ? { icon: 'notifications' } : null;
    this.addMode = true;
  }

  linkAdded(link: DocumentationLink | string) {
    this.addMode = false;
    const linksArray = this.editLinksFormGroup.get('links') as UntypedFormArray;
    const linkControl = this.fb.control(link, [Validators.required]);
    linksArray.push(linkControl);
    this.update();
  }

  deleteLink(index: number) {
    (this.editLinksFormGroup.get('links') as UntypedFormArray).removeAt(index);
    this.update();
  }

  update() {
    if (this.editLinksFormGroup.valid) {
      let updateObservable: Observable<void>;
      if (this.mode === 'docs') {
        updateObservable = this.userSettingsService.updateDocumentationLinks(this.editLinksFormGroup.value);
      } else {
        updateObservable = this.userSettingsService.updateQuickLinks(this.editLinksFormGroup.value);
      }
      updateObservable.subscribe(() => {
        this.updated = true;
      });
    }
  }

  close(): void {
    this.dialogRef.close(this.updated);
  }
}
