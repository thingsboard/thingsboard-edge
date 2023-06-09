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

import { Component, OnInit, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { ActivatedRoute, Router } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { of } from 'rxjs';
import { Widget, widgetType } from '@app/shared/models/widget.models';
import { WidgetService } from '@core/http/widget.service';
import { map, mergeMap } from 'rxjs/operators';
import { DialogService } from '@core/services/dialog.service';
import { FooterFabButtons } from '@app/shared/components/footer-fab-buttons.component';
import { DashboardCallbacks, IDashboardComponent, WidgetsData } from '@home/models/dashboard-component.models';
import { IAliasController, IStateController, StateParams } from '@app/core/api/widget-api.models';
import { AliasController } from '@core/api/alias-controller';
import { MatDialog } from '@angular/material/dialog';
import { SelectWidgetTypeDialogComponent } from '@home/pages/widget/select-widget-type-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { UtilsService } from '@core/services/utils.service';
import { EntityService } from '@core/http/entity.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';

@Component({
  selector: 'tb-widget-library',
  templateUrl: './widget-library.component.html',
  styleUrls: ['./widget-library.component.scss']
})
export class WidgetLibraryComponent extends PageComponent implements OnInit {

  authUser: AuthUser;

  isReadOnly: boolean;
  editEnabled = this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.WRITE);
  addEnabled = this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.CREATE);
  deleteEnabled = this.userPermissionsService.hasGenericPermission(Resource.WIDGET_TYPE, Operation.DELETE);

  widgetsBundle: WidgetsBundle;

  widgetsData: WidgetsData;

  footerFabButtons: FooterFabButtons = {
    fabTogglerName: 'widget.add-widget-type',
    fabTogglerIcon: 'add',
    buttons: [
      {
        name: 'widget-type.create-new-widget-type',
        icon: 'insert_drive_file',
        onAction: ($event) => {
          this.addWidgetType($event);
        }
      },
      {
        name: 'widget-type.import',
        icon: 'file_upload',
        onAction: ($event) => {
          this.importWidgetType($event);
        }
      }
    ]
  };

  dashboardCallbacks: DashboardCallbacks = {
    onEditWidget: this.openWidgetType.bind(this),
    onWidgetClicked: this.openWidgetType.bind(this),
    onExportWidget: this.exportWidgetType.bind(this),
    onRemoveWidget: this.removeWidgetType.bind(this)
  };

  aliasController: IAliasController = new AliasController(this.utils,
    this.entityService,
    this.translate,
    () => { return {
      getStateParams(): StateParams {
        return {};
      }
    } as IStateController;
    },
    {},
    {});

  @ViewChild('dashboard', {static: true}) dashboard: IDashboardComponent;

  constructor(protected store: Store<AppState>,
              private userPermissionsService: UserPermissionsService,
              private route: ActivatedRoute,
              private router: Router,
              private widgetService: WidgetService,
              private dialogService: DialogService,
              private importExport: ImportExportService,
              private dialog: MatDialog,
              private translate: TranslateService,
              private utils: UtilsService,
              private entityService: EntityService) {
    super(store);

    this.authUser = getCurrentAuthUser(this.store);
    this.widgetsBundle = this.route.snapshot.data.widgetsBundle;
    this.widgetsData = this.route.snapshot.data.widgetsData;
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      this.isReadOnly = !this.widgetsBundle || this.widgetsBundle.tenantId.id === NULL_UUID;
    } else {
      this.isReadOnly = this.authUser.authority !== Authority.SYS_ADMIN;
    }
  }

  ngOnInit(): void {
  }

  addWidgetType($event: Event): void {
    this.openWidgetType($event);
  }

  importWidgetType($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.importWidgetType(this.widgetsBundle.alias).subscribe(
      (widgetTypeInstance) => {
        if (widgetTypeInstance) {
          this.reload();
        }
      }
    );
  }

  private reload() {
    const bundleAlias = this.widgetsBundle.alias;
    const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
    this.widgetService.loadBundleLibraryWidgets(bundleAlias, isSystem).subscribe(
      (widgets) => {
        this.widgetsData = {widgets};
      }
    );
  }

  openWidgetType($event: Event, widget?: Widget): void {
    if ($event) {
      $event.stopPropagation();
    }
    if (widget) {
      this.router.navigate([widget.typeId.id], {relativeTo: this.route});
    } else {
      this.dialog.open<SelectWidgetTypeDialogComponent, any,
        widgetType>(SelectWidgetTypeDialogComponent, {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
      }).afterClosed().subscribe(
        (type) => {
          if (type) {
            this.router.navigate(['add', type], {relativeTo: this.route});
          }
        }
      );
    }
  }

  exportWidgetType($event: Event, widget: Widget): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportWidgetType(widget.typeId.id);
  }

  removeWidgetType($event: Event, widget: Widget): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('widget.remove-widget-type-title', {widgetName: widget.config.title}),
      this.translate.instant('widget.remove-widget-type-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
    ).pipe(
      mergeMap((result) => {
        if (result) {
          return this.widgetService.deleteWidgetType(widget.bundleAlias, widget.typeAlias, widget.isSystemType);
        } else {
          return of(false);
        }
      }),
      map((result) => {
        if (result !== false) {
          this.reload();
          return true;
        } else {
          return false;
        }
      }
    )).subscribe();
  }

}
