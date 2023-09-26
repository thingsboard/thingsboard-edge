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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { AuditLogService } from '@core/http/audit-log.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { AuditLogMode } from '@shared/models/audit-log.models';
import { EntityId } from '@shared/models/id/entity-id';
import { UserId } from '@shared/models/id/user-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { AuditLogTableConfig } from '@home/components/audit-log/audit-log-table-config';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { UtilsService } from '@core/services/utils.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'tb-audit-log-table',
  templateUrl: './audit-log-table.component.html',
  styleUrls: ['./audit-log-table.component.scss']
})
export class AuditLogTableComponent implements OnInit {

  @Input()
  auditLogMode: AuditLogMode;

  @Input()
  detailsMode: boolean;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;
  userIdValue: UserId;
  customerIdValue: CustomerId;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        this.entitiesTable.updateData();
      }
    }
  }

  @Input()
  set entityId(entityId: EntityId) {
    this.entityIdValue = entityId;
    if (this.auditLogTableConfig && this.auditLogTableConfig.entityId !== entityId) {
      this.auditLogTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @Input()
  set userId(userId: UserId) {
    this.userIdValue = userId;
    if (this.auditLogTableConfig && this.auditLogTableConfig.userId !== userId) {
      this.auditLogTableConfig.userId = userId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @Input()
  set customerId(customerId: CustomerId) {
    this.customerIdValue = customerId;
    if (this.auditLogTableConfig && this.auditLogTableConfig.customerId !== customerId?.id) {
      this.auditLogTableConfig.customerId = customerId?.id;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  auditLogTableConfig: AuditLogTableConfig;

  constructor(private auditLogService: AuditLogService,
              private translate: TranslateService,
              private utils: UtilsService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private route: ActivatedRoute) {
  }

  ngOnInit() {
    let updateOnInit = this.activeValue;
    this.dirtyValue = !this.activeValue;
    if (!this.auditLogMode) {
      const authUser = getCurrentAuthUser(this.store);
      if (authUser.authority === Authority.TENANT_ADMIN) {
        this.auditLogMode = AuditLogMode.TENANT;
      }
      updateOnInit = true;
    }
    const pageMode = !!this.route.snapshot.data.isPage;
    this.auditLogTableConfig = new AuditLogTableConfig(
      this.auditLogService,
      this.translate,
      this.utils,
      this.datePipe,
      this.dialog,
      this.auditLogMode,
      this.entityIdValue,
      this.userIdValue,
      this.customerIdValue?.id,
      updateOnInit,
      pageMode
    );
  }

}
