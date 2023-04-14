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

import { NgModule } from '@angular/core';
import { Lwm2mDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/lwm2m/lwm2m-device-profile-transport-configuration.component';
import { Lwm2mObjectListComponent } from '@home/components/profile/device/lwm2m/lwm2m-object-list.component';
import { Lwm2mObserveAttrTelemetryComponent } from '@home/components/profile/device/lwm2m/lwm2m-observe-attr-telemetry.component';
import { Lwm2mObserveAttrTelemetryResourcesComponent } from '@home/components/profile/device/lwm2m/lwm2m-observe-attr-telemetry-resources.component';
import { Lwm2mAttributesDialogComponent } from '@home/components/profile/device/lwm2m/lwm2m-attributes-dialog.component';
import { Lwm2mAttributesComponent } from '@home/components/profile/device/lwm2m/lwm2m-attributes.component';
import { Lwm2mAttributesKeyListComponent } from '@home/components/profile/device/lwm2m/lwm2m-attributes-key-list.component';
import { Lwm2mDeviceConfigServerComponent } from '@home/components/profile/device/lwm2m/lwm2m-device-config-server.component';
import { Lwm2mObjectAddInstancesDialogComponent } from '@home/components/profile/device/lwm2m/lwm2m-object-add-instances-dialog.component';
import { Lwm2mObjectAddInstancesListComponent } from '@home/components/profile/device/lwm2m/lwm2m-object-add-instances-list.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { Lwm2mObserveAttrTelemetryInstancesComponent } from '@home/components/profile/device/lwm2m/lwm2m-observe-attr-telemetry-instances.component';
import { DeviceProfileCommonModule } from '@home/components/profile/device/common/device-profile-common.module';
import { Lwm2mBootstrapConfigServersComponent } from '@home/components/profile/device/lwm2m/lwm2m-bootstrap-config-servers.component';
import { Lwm2mBootstrapAddConfigServerDialogComponent } from '@home/components/profile/device/lwm2m/lwm2m-bootstrap-add-config-server-dialog.component';

@NgModule({
  declarations:
    [
      Lwm2mDeviceProfileTransportConfigurationComponent,
      Lwm2mObjectListComponent,
      Lwm2mObserveAttrTelemetryComponent,
      Lwm2mObserveAttrTelemetryResourcesComponent,
      Lwm2mAttributesDialogComponent,
      Lwm2mAttributesComponent,
      Lwm2mAttributesKeyListComponent,
      Lwm2mBootstrapConfigServersComponent,
      Lwm2mDeviceConfigServerComponent,
      Lwm2mBootstrapAddConfigServerDialogComponent,
      Lwm2mObjectAddInstancesDialogComponent,
      Lwm2mObjectAddInstancesListComponent,
      Lwm2mObserveAttrTelemetryInstancesComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    DeviceProfileCommonModule
   ],
  exports: [
    Lwm2mDeviceProfileTransportConfigurationComponent,
    Lwm2mObjectListComponent,
    Lwm2mObserveAttrTelemetryComponent,
    Lwm2mObserveAttrTelemetryResourcesComponent,
    Lwm2mAttributesDialogComponent,
    Lwm2mAttributesComponent,
    Lwm2mAttributesKeyListComponent,
    Lwm2mBootstrapConfigServersComponent,
    Lwm2mDeviceConfigServerComponent,
    Lwm2mBootstrapAddConfigServerDialogComponent,
    Lwm2mObjectAddInstancesDialogComponent,
    Lwm2mObjectAddInstancesListComponent,
    Lwm2mObserveAttrTelemetryInstancesComponent
  ],
  providers: [
  ]
})
export class Lwm2mProfileComponentsModule { }
