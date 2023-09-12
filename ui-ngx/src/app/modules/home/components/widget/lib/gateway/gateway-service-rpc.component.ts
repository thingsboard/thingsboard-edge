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

import { AfterViewInit, Component,  Input } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AttributeService } from '@core/http/attribute.service';
import { DeviceService } from '@core/http/device.service';
import { TranslateService } from '@ngx-translate/core';
import { PageComponent } from "@shared/components/page.component";
import { DialogService } from '@core/services/dialog.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { ContentType } from '@shared/models/constants';
import {
  JsonObjectEditDialogComponent,
  JsonObjectEditDialogData
} from '@shared/components/dialog/json-object-edit-dialog.component';
import { jsonRequired } from '@shared/components/json-object-edit.component';


@Component({
  selector: 'tb-gateway-service-rpc',
  templateUrl: './gateway-service-rpc.component.html',
  styleUrls: ['./gateway-service-rpc.component.scss']
})
export class GatewayServiceRPCComponent extends PageComponent implements AfterViewInit {

  @Input()
  ctx: WidgetContext;

  contentTypes = ContentType;

  @Input()
  dialogRef: MatDialogRef<any>;

  commandForm: FormGroup;

  isConnector: boolean;

  connectorType: string;

  RPCCommands: Array<string> = [
    "Ping",
    "Stats",
    "Devices",
    "Update",
    "Version",
    "Restart",
    "Reboot"
  ]


  constructor(protected router: Router,
              protected store: Store<AppState>,
              protected fb: FormBuilder,
              protected translate: TranslateService,
              protected attributeService: AttributeService,
              protected deviceService: DeviceService,
              protected dialogService: DialogService,
              public dialog: MatDialog) {
    super(store);
    this.commandForm = this.fb.group({
      command: [null,[Validators.required]],
      time: [60, [Validators.required, Validators.min(1)]],
      params: [{}, [jsonRequired]],
      result: [null]
    })


  }


  ngAfterViewInit() {
    this.isConnector = this.ctx.settings.isConnector;
    if (!this.isConnector) {
      this.commandForm.get('command').setValue(this.RPCCommands[0]);
    } else {
      this.connectorType = this.ctx.stateController.getStateParams().connector_rpc.value.type;
    }
  }


  sendCommand() {
    const formValues = this.commandForm.value;
    const commandPrefix = this.isConnector ? `${this.connectorType}_` : 'gateway_';
    this.ctx.controlApi.sendTwoWayCommand(commandPrefix+formValues.command.toLowerCase(), {},formValues.time).subscribe(resp=>{
      this.commandForm.get('result').setValue(JSON.stringify(resp));
    },error => {
      console.log(error);
      this.commandForm.get('result').setValue(JSON.stringify(error.error));
    })
  }

  openEditJSONDialog($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<JsonObjectEditDialogComponent, JsonObjectEditDialogData, object>(JsonObjectEditDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        jsonValue: JSON.parse(this.commandForm.get('params').value),
        required: true
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.commandForm.get('params').setValue(JSON.stringify(res));
        }
      }
    );
  }

}
