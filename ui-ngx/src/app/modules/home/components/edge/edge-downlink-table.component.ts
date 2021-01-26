///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EdgeDownlinkTableConfig } from './edge-downlink-table-config';
import { DialogService } from '@core/services/dialog.service';
import { RuleChainService } from '@core/http/rule-chain.service';
import { AttributeService } from '@core/http/attribute.service';
import { EdgeService } from '@core/http/edge.service';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';

@Component({
  selector: 'tb-edge-downlink-table',
  templateUrl: './edge-downlink-table.component.html',
  styleUrls: ['./edge-downlink-table.component.scss']
})
export class EdgeDownlinkTableComponent implements OnInit {

  @Input()
  tenantId: string;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: EntityId;

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
    if (this.edgeDownlinkTableConfig && this.edgeDownlinkTableConfig.entityId !== entityId) {
      this.edgeDownlinkTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  edgeDownlinkTableConfig: EdgeDownlinkTableConfig;

  constructor(private edgeService: EdgeService,
              private deviceService: DeviceService,
              private assetService: AssetService,
              private entityViewService: EntityViewService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private attributeService: AttributeService,
              private ruleChainService: RuleChainService,
              private datePipe: DatePipe,
              private dialog: MatDialog) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    this.edgeDownlinkTableConfig = new EdgeDownlinkTableConfig(
      this.edgeService,
      this.dialogService,
      this.translate,
      this.deviceService,
      this.assetService,
      this.entityViewService,
      this.ruleChainService,
      this.attributeService,
      this.datePipe,
      this.dialog,
      this.entityIdValue,
      this.tenantId
    );
  }

}
