///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeDialogsModule } from '../../dialogs/home-dialogs.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { IntegrationComponent } from '@home/pages/integration/integration.component';
import { IntegrationTabsComponent } from '@home/pages/integration/integration-tabs.component';
import { IntegrationRoutingModule } from '@home/pages/integration/integration-routing.module';
import { integrations } from './configurations';
import { IntegrationComponentModule } from '@home/components/integration/integration-component.module';
import {
    MqttIntegrationFormComponent
} from '@home/pages/integration/configurations/mqtt-integration-form/mqtt-integration-form.component';
import {
    HttpIntegrationFormComponent
} from '@home/pages/integration/configurations/http-integration-form/http-integration-form.component';
import { MqttTopicFiltersComponent } from '@home/pages/integration/configurations/mqtt-topic-filters/mqtt-topic-filters.component';

@NgModule({
    declarations: [
        IntegrationComponent,
        IntegrationTabsComponent,
        ...integrations
    ],
  exports: [
    MqttIntegrationFormComponent,
    HttpIntegrationFormComponent,
    MqttTopicFiltersComponent
  ],
    imports: [
        CommonModule,
        SharedModule,
        HomeComponentsModule,
        HomeDialogsModule,
        IntegrationRoutingModule,
        IntegrationComponentModule
    ]
})
export class IntegrationModule { }
