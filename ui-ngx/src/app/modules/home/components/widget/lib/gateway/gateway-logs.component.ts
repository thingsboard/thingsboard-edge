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

import { AfterViewInit,  Component, ElementRef, Input, ViewChild } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { TranslateService } from '@ngx-translate/core';
import { AttributeData, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { PageComponent } from '@shared/components/page.component';
import { PageLink } from '@shared/models/page/page-link';
import { AttributeDatasource } from "@home/models/datasource/attribute-datasource";
import { Direction, SortOrder } from "@shared/models/page/sort-order";
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { GatewayLogLevel } from '@home/components/widget/lib/gateway/gateway-configuration.component';
import { DialogService } from '@core/services/dialog.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { MatPaginator } from '@angular/material/paginator';


export interface GatewayConnector {
  name: string;
  type: string;
  configuration?: string;
  configurationJson: string;
  logLevel: string;
  key?: string;
}

export interface LogLink {
  name: string;
  key: string;
  filterFn?: Function;
}

@Component({
  selector: 'tb-gateway-logs',
  templateUrl: './gateway-logs.component.html',
  styleUrls: ['./gateway-logs.component.scss']
})
export class GatewayLogsComponent extends PageComponent implements AfterViewInit {

  pageLink: PageLink;

  attributeDataSource: AttributeDatasource;

  dataSource: MatTableDataSource<any>

  displayedColumns = ['ts', 'status', 'message'];

  @Input()
  ctx: WidgetContext;

  @Input()
  dialogRef: MatDialogRef<any>;

  @ViewChild('searchInput') searchInputField: ElementRef;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatPaginator) paginator: MatPaginator;

  connectorForm: FormGroup;

  viewsInited = false;

  textSearchMode: boolean;

  activeConnectors: Array<string>;

  inactiveConnectors: Array<string>;

  InitialActiveConnectors: Array<string>;

  gatewayLogLevel = Object.values(GatewayLogLevel);

  logLinks: Array<LogLink>;

  initialConnector: GatewayConnector;

  activeLink: LogLink;

  gatewayLogLinks: Array<LogLink> = [
    {
      name: "General",
      key: "LOGS"
    }, {
      name: "Service",
      key: "SERVICE_LOGS"
    },
    {
      name: "Connection",
      key: "CONNECTION_LOGS"
    }, {
      name: "Storage",
      key: "STORAGE_LOGS"
    },
    {
      key: 'EXTENSIONS_LOGS',
      name: "Extension"
    }]


  constructor(protected router: Router,
              protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected translate: TranslateService,
              protected attributeService: AttributeService,
              protected deviceService: DeviceService,
              protected dialogService: DialogService,
              public dialog: MatDialog) {
    super(store);
    const sortOrder: SortOrder = {property: 'ts', direction: Direction.DESC};
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new MatTableDataSource<AttributeData>([]);

  }


  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
    this.ctx.defaultSubscription.onTimewindowChangeFunction = timewindow => {
      this.ctx.defaultSubscription.options.timeWindowConfig = timewindow;
      this.ctx.defaultSubscription.updateDataSubscriptions();
      return timewindow;
    }
    if (this.ctx.settings.isConnectorLog && this.ctx.settings.connectorLogState) {
      const connector = this.ctx.stateController.getStateParams()[this.ctx.settings.connectorLogState];
      this.logLinks = [{
        key: `${connector.key}_LOGS`,
        name: "Connector",
        filterFn: (attrData)=>{
          return !attrData.message.includes(`_converter.py`)
        }
      },{
        key: `${connector.key}_LOGS`,
        name: "Converter",
        filterFn: (attrData)=>{
          return attrData.message.includes(`_converter.py`)
        }
      }]
    } else {
      this.logLinks = this.gatewayLogLinks;
    }
    this.activeLink = this.logLinks[0];
    this.changeSubscription();
  }


  updateData(sort?) {
    if (this.ctx.defaultSubscription.data.length) {
      let attrData = this.ctx.defaultSubscription.data[0].data.map(data => {
        let result =  {
          ts: data[0],
          key: this.activeLink.key,
          message: /\[(.*)/.exec(data[1])[0],
          status: 'INVALID LOG FORMAT'
        };

        try {
          result.status= data[1].match(/\|(\w+)\|/)[1];
        } catch (e) {
          result.status = 'INVALID LOG FORMAT'
        }

        return result;
      });
      if (this.activeLink.filterFn) {
        attrData = attrData.filter(data => this.activeLink.filterFn(data));
      }
      this.dataSource.data = attrData;
      if (sort) {
        this.dataSource.sortData(this.dataSource.data, this.sort);
      }
    }
  }

  onTabChanged(link) {
    this.activeLink = link;
    this.changeSubscription();
  }

  statusClass(status) {
    switch (status) {
      case GatewayLogLevel.debug:
        return "status status-debug";
      case GatewayLogLevel.warning:
        return "status status-warning";
      case GatewayLogLevel.error:
      case "EXCEPTION":
        return "status status-error";
      case GatewayLogLevel.info:
      default:
        return "status status-info";
    }
  }

  statusClassMsg(status) {
    if (status === "EXCEPTION") {
      return 'msg-status-exception';
    }
  }

  changeSubscription() {
    if (this.ctx.datasources[0].entity) {
      this.ctx.defaultSubscription.options.datasources[0].dataKeys = [{
        name: this.activeLink.key,
        type: DataKeyType.timeseries,
        settings: {}
      }];
      this.ctx.defaultSubscription.unsubscribe();
      this.ctx.defaultSubscription.updateDataSubscriptions();
      this.ctx.defaultSubscription.callbacks.onDataUpdated = () => {
        this.updateData();
      }

    }
  }

}
