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

import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AuthUser } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetTypeInfo } from '@shared/models/widget.models';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { WidgetService } from '@core/http/widget.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';
import { FormControl, Validators } from '@angular/forms';

@Component({
  selector: 'tb-widgets-bundle-widget',
  templateUrl: './widgets-bundle-widgets.component.html',
  styleUrls: ['./widgets-bundle-widgets.component.scss']
})
export class WidgetsBundleWidgetsComponent extends PageComponent implements OnInit {

  authUser: AuthUser;

  isReadOnly: boolean;
  editMode = false;
  addMode = false;
  isDirty = false;

  widgetsBundle: WidgetsBundle;
  widgets: Array<WidgetTypeInfo>;
  excludeWidgetTypeIds: Array<string>;

  addWidgetFormControl = new FormControl(null, [Validators.required]);

  constructor(protected store: Store<AppState>,
              private router: Router,
              private route: ActivatedRoute,
              private widgetsService: WidgetService,
              private importExport: ImportExportService,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef) {
    super(store);
    this.authUser = getCurrentAuthUser(this.store);
    this.widgetsBundle = this.route.snapshot.data.widgetsBundle;
    this.widgets = [...this.route.snapshot.data.widgets];
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      this.isReadOnly = !this.widgetsBundle || this.widgetsBundle.tenantId.id === NULL_UUID;
    } else {
      this.isReadOnly = this.authUser.authority !== Authority.SYS_ADMIN;
    }
    if (!this.isReadOnly && !this.widgets.length) {
      this.editMode = true;
    }
    this.addWidgetFormControl.valueChanges.subscribe((newWidget) => {
      if (newWidget) {
        this.addWidget(newWidget);
      }
    });
  }

  ngOnInit(): void {
  }

  getPreviewImage(imageUrl: string | null): SafeUrl | string {
    if (isDefinedAndNotNull(imageUrl)) {
      return this.sanitizer.bypassSecurityTrustUrl(imageUrl);
    }
    return '/assets/widget-preview-empty.svg';
  }

  trackByWidget(index: number, widget: WidgetTypeInfo): any {
    return widget;
  }

  widgetDrop(event: CdkDragDrop<string[]>) {
    const widget = this.widgets[event.previousIndex];
    this.widgets.splice(event.previousIndex, 1);
    this.widgets.splice(event.currentIndex, 0, widget);
    this.isDirty = true;
  }

  addWidgetMode() {
    this.addWidgetFormControl.patchValue(null, {emitEvent: false});
    this.excludeWidgetTypeIds = this.widgets.map(w => w.id.id);
    this.addMode = true;
  }

  cancelAddWidgetMode() {
    this.addMode = false;
  }

  private addWidget(newWidget: WidgetTypeInfo) {
    this.widgets.push(newWidget);
    this.isDirty = true;
    this.addMode = false;
  }

  openWidgetEditor($event: Event, widgetType: WidgetTypeInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`resources/widgets-library/widget-types/${widgetType.id.id}`);
  }

  exportWidgetType($event: Event, widgetType: WidgetTypeInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetType(widgetType.id.id);
  }

  removeWidgetType($event: Event, widgetType: WidgetTypeInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const index = this.widgets.indexOf(widgetType);
    this.widgets.splice(index, 1);
    this.isDirty = true;
  }

  goBack() {
    this.router.navigate(['..'], { relativeTo: this.route });
  }

  exportWidgetsBundle() {
    this.importExport.exportWidgetsBundle(this.widgetsBundle.id.id);
  }

  edit() {
    this.editMode = true;
  }

  cancel() {
    if (this.isDirty) {
      this.widgetsService.getBundleWidgetTypeInfos(this.widgetsBundle.id.id).subscribe(
        (widgets) => {
          this.widgets = [...widgets];
          this.isDirty = false;
          this.addMode = false;
          this.editMode = !this.widgets.length;
          this.cd.markForCheck();
        }
      );
    } else {
      this.addMode = false;
      this.editMode = !this.widgets.length;
    }
  }

  save() {
    const widgetTypeIds = this.widgets.map(w => w.id.id);
    this.widgetsService.updateWidgetsBundleWidgetTypes(this.widgetsBundle.id.id, widgetTypeIds).subscribe(() => {
      this.isDirty = false;
      this.editMode = false;
    });
  }

}
