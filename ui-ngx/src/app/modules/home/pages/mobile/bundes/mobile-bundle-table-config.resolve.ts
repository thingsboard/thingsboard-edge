///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityChipsEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { MobileAppBundleInfo } from '@shared/models/mobile-app.models';
import { ActivatedRouteSnapshot } from '@angular/router';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { MobileBundleTableHeaderComponent } from '@home/pages/mobile/bundes/mobile-bundle-table-header.component';
import { DatePipe } from '@angular/common';
import { MobileAppService } from '@core/http/mobile-app.service';
import { map } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import {
  MobileBundleDialogComponent,
  MobileBundleDialogData
} from '@home/pages/mobile/bundes/mobile-bundle-dialog.component';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';

@Injectable()
export class MobileBundleTableConfigResolver {

  private readonly config: EntityTableConfig<MobileAppBundleInfo> = new EntityTableConfig<MobileAppBundleInfo>();

  constructor(
    private datePipe: DatePipe,
    private mobileAppService: MobileAppService,
    private translate : TranslateService,
    private dialog: MatDialog,
    private store: Store<AppState>,
  ) {
    this.config.selectionEnabled = false;
    this.config.entityType = EntityType.MOBILE_APP_BUNDLE;
    this.config.addEnabled = false;
    this.config.rowPointer = true;
    this.config.detailsPanelEnabled = false;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.MOBILE_APP_BUNDLE);
    this.config.entityResources = entityTypeResources.get(EntityType.MOBILE_APP_BUNDLE);
    this.config.headerComponent = MobileBundleTableHeaderComponent;
    this.config.onEntityAction = action => this.onBundleAction(action);
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.deleteEnabled = bundle => !(bundle.iosAppId || bundle.androidAppId);
    this.config.deleteEntityTitle = (bundle) => this.translate.instant('mobile.delete-applications-bundle-title', {bundleName: bundle.name});
    this.config.deleteEntityContent = () => this.translate.instant('mobile.delete-applications-bundle-text');
    this.config.deleteEntity = id => this.mobileAppService.deleteMobileAppBundle(id.id);

    this.config.entitiesFetchFunction = pageLink => this.mobileAppService.getTenantMobileAppBundleInfos(pageLink).pipe(
      map(bundles => {
        bundles.data.map(data => {
          if (data.androidPkgName) {
            data.androidPkg = {
              id: data.androidAppId,
              name: data.androidPkgName
            }
          }
          if (data.iosPkgName) {
            data.iosPkg = {
              id: data.iosAppId,
              name: data.iosPkgName
            }
          }
        })
        return bundles;
      })
    );

    this.config.handleRowClick = ($event, bundle) => {
      $event?.stopPropagation();
      this.mobileAppService.getMobileAppBundleInfoById(bundle.id.id).subscribe(appBundleInfo => {
        this.editBundle($event, appBundleInfo);
      })
      return true;
    };
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<MobileAppBundleInfo> {
    const authUser = getCurrentAuthUser(this.store);

    this.config.columns = [
      new DateEntityTableColumn<MobileAppBundleInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<MobileAppBundleInfo>('title', 'mobile.title', '25%'),
      new EntityChipsEntityTableColumn<MobileAppBundleInfo>('oauth2ClientInfos', 'mobile.oauth-clients', '35%'),
      new EntityChipsEntityTableColumn<MobileAppBundleInfo>('androidPkg', 'mobile.android-app', '20%'),
      new EntityChipsEntityTableColumn<MobileAppBundleInfo>('iosPkg', 'mobile.ios-app', '20%'),
      new EntityTableColumn<MobileAppBundleInfo>('oauth2Enabled', 'mobile.enable-oauth', '140px',
        entity => checkBoxCell(entity.oauth2Enabled))
    ];

    if (authUser.authority !== Authority.SYS_ADMIN) {
      this.config.columns.push(
        new EntityTableColumn<MobileAppBundleInfo>('selfRegistrationParams.enabled', 'mobile.enable-self-registration', '140px',
          entity => checkBoxCell(entity.selfRegistrationParams?.enabled)),
      )
    }

    return this.config;
  }

  private editBundle($event: Event, bundle: MobileAppBundleInfo, isAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<MobileBundleDialogComponent, MobileBundleDialogData,
      MobileAppBundleInfo>(MobileBundleDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        bundle
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  private onBundleAction(action: EntityAction<MobileAppBundleInfo>): boolean {
    switch (action.action) {
      case 'add':
        this.editBundle(action.event, action.entity, true);
        return true;
    }
    return false;
  }
}
