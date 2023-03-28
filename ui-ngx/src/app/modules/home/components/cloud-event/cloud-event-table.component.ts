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
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { UtilsService } from '@core/services/utils.service';
import { CloudEventTableConfig } from "@home/components/cloud-event/cloud-event-table-config";
import { EdgeService } from "@core/http/edge.service";
import { Store } from "@ngrx/store";
import { AppState } from "@core/core.state";
import { AttributeService } from "@core/http/attribute.service";
import { EntityService } from '@core/http/entity.service';

@Component({
  selector: 'tb-cloud-event-table',
  templateUrl: './cloud-event-table.component.html',
  styleUrls: ['./cloud-event-table.component.scss']
})
export class CloudEventTableComponent implements OnInit {

  @Input()
  detailsMode: boolean;

  activeValue = false;
  dirtyValue = false;

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

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  cloudEventTableConfig: CloudEventTableConfig;

  constructor(private translate: TranslateService,
              private utils: UtilsService,
              private datePipe: DatePipe,
              private dialog: MatDialog,
              private edgeService: EdgeService,
              private store: Store<AppState>,
              private entityService: EntityService,
              private attributeService: AttributeService) {
  }

  ngOnInit() {
    this.dirtyValue = !this.activeValue;
    this.cloudEventTableConfig = new CloudEventTableConfig(
      this.translate,
      this.utils,
      this.datePipe,
      this.dialog,
      this.edgeService,
      this.store,
      this.attributeService,
      this.entityService
    );
  }

}
