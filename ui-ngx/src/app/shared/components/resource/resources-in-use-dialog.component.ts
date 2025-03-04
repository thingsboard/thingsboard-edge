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

import { Component, Inject, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  ResourceReferences,
  ResourceInfoWithReferences,
  ResourceInfo
} from '@shared/models/resource.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ImageReferencesComponent } from '@shared/components/image/image-references.component';
import { TranslateService } from '@ngx-translate/core';
import { Datasource } from "@shared/models/widget.models";

interface ResourcesInUseDialogDataConfiguration {
  title: string;
  message: string;
  columns: string[];
  deleteText: string;
  selectedText: string;
  datasource?: Datasource;
}

export interface ResourcesInUseDialogData {
  multiple: boolean;
  resources: ResourceInfoWithReferences[];
  configuration: ResourcesInUseDialogDataConfiguration;
}

@Component({
  selector: 'tb-resources-in-use-dialog',
  templateUrl: './resources-in-use-dialog.component.html',
  styleUrls: ['./resources-in-use-dialog.component.scss']
})
export class ResourcesInUseDialogComponent extends
  DialogComponent<ResourcesInUseDialogComponent, ResourceInfo[]> implements OnInit {

  displayPreview: boolean;
  configuration: ResourcesInUseDialogDataConfiguration;
  references: ResourceReferences;

  dataSource: Datasource;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ResourcesInUseDialogData,
              public dialogRef: MatDialogRef<ResourcesInUseDialogComponent, ResourceInfo[]>,
              public translate: TranslateService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private popoverService: TbPopoverService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.configuration = this.data.configuration;
    this.displayPreview = this.data.configuration.columns.includes('preview');
    if (this.data.multiple) {
      this.dataSource = this.data.configuration.datasource;
    } else {
      this.references = this.data.resources[0].references;
    }
  }

  cancel() {
    this.dialogRef.close(null);
  }

  delete() {
    if (this.data.multiple) {
      this.dialogRef.close(this.dataSource.selection.selected);
    } else {
      this.dialogRef.close(this.data.resources);
    }
  }

  toggleShowReferences($event: Event, resource: ResourceInfoWithReferences, referencesButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = referencesButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const referencesPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, ImageReferencesComponent, 'top', true, null,
        {
          references: resource.references
        }, {}, {}, {},
        false,
        visible => {
          const addClasses =
            visible ? 'mdc-button--unelevated mat-mdc-unelevated-button' : 'mdc-button--outlined mat-mdc-outlined-button';
          const removeClasses =
            visible ? 'mdc-button--outlined mat-mdc-outlined-button' : 'mdc-button--unelevated mat-mdc-unelevated-button';
          addClasses.split(' ').forEach(clazz => {
            this.renderer.addClass(trigger, clazz);
          });
          removeClasses.split(' ').forEach(clazz => {
            this.renderer.removeClass(trigger, clazz);
          });
        });
      referencesPopover.tbComponentRef.instance.popoverComponent = referencesPopover;
    }
  }
}
