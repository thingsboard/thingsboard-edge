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

import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { DeviceService } from '@core/http/device.service';
import { helpBaseUrl } from '@shared/models/constants';
import { getOS } from '@core/utils';
import { PublishLaunchCommand } from '@shared/models/device.models';

@Component({
  selector: 'tb-gateway-command',
  templateUrl: './device-gateway-command.component.html',
  styleUrls: ['./device-gateway-command.component.scss']
})

export class DeviceGatewayCommandComponent implements OnInit {

  @Input()
  token: string;

  @Input()
  deviceId: string;

  commands: PublishLaunchCommand;

  helpLink: string = helpBaseUrl + '/docs/iot-gateway/install/docker-installation/';

  tabIndex = 0;

  constructor(private cd: ChangeDetectorRef,
              private deviceService: DeviceService) {
  }


  ngOnInit(): void {
    if (this.deviceId) {
      this.deviceService.getDevicePublishLaunchCommands(this.deviceId).subscribe(commands => {
        this.commands = commands;
        this.cd.detectChanges();
      });
    }
    const currentOS = getOS();
    switch (currentOS) {
      case 'linux':
      case 'android':
        this.tabIndex = 1;
        break;
      case 'macos':
      case 'ios':
        this.tabIndex = 2;
        break;
      case 'windows':
        this.tabIndex = 0;
        break;
      default:
        this.tabIndex = 1;
    }
  }
}
