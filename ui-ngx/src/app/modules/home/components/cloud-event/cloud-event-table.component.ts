///
/// Copyright © 2016-2023 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
