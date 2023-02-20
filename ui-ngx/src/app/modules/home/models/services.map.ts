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

import { Type } from '@angular/core';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { AttributeService } from '@core/http/attribute.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { EntityService } from '@core/http/entity.service';
import { DialogService } from '@core/services/dialog.service';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { EntityViewService } from '@core/http/entity-view.service';
import { CustomerService } from '@core/http/customer.service';
import { DashboardService } from '@core/http/dashboard.service';
import { UserService } from '@core/http/user.service';
import { EntityGroupService } from '@core/http/entity-group.service';
import { RoleService } from '@core/http/role.service';
import { AlarmService } from '@core/http/alarm.service';
import { Router } from '@angular/router';
import { BroadcastService } from '@core/services/broadcast.service';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { EdgeService } from '@core/http/edge.service';
import { SchedulerEventService } from '@core/http/scheduler-event.service';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { OtaPackageService } from '@core/http/ota-package.service';
import { RuleEngineService } from '@core/http/rule-engine.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { AuthService } from '@core/auth/auth.service';
import { ResourceService } from '@core/http/resource.service';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { NotificationService } from '@core/http/notification.service';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';

export const ServicesMap = new Map<string, Type<any>>(
  [
   ['broadcastService', BroadcastService],
   ['deviceService', DeviceService],
   ['alarmService', AlarmService],
   ['assetService', AssetService],
   ['entityViewService', EntityViewService],
   ['edgeService', EdgeService],
   ['customerService', CustomerService],
   ['dashboardService', DashboardService],
   ['userService', UserService],
   ['attributeService', AttributeService],
   ['entityRelationService', EntityRelationService],
   ['entityService', EntityService],
   ['entityGroupService', EntityGroupService],
   ['roleService', RoleService],
   ['dialogs', DialogService],
   ['customDialog', CustomDialogService],
   ['date', DatePipe],
   ['milliSecondsToTimeString', MillisecondsToTimeStringPipe],
   ['utils', UtilsService],
   ['translate', TranslateService],
   ['http', HttpClient],
   ['router', Router],
   ['importExport', ImportExportService],
   ['schedulerEventService', SchedulerEventService],
   ['deviceProfileService', DeviceProfileService],
   ['otaPackageService', OtaPackageService],
   ['ruleEngineService', RuleEngineService],
   ['userPermissionsService', UserPermissionsService],
   ['authService', AuthService],
   ['resourceService', ResourceService],
   ['twoFactorAuthenticationService', TwoFactorAuthenticationService],
   ['telemetryWsService', TelemetryWebsocketService],
   ['notificationService', NotificationService]
  ]
);
