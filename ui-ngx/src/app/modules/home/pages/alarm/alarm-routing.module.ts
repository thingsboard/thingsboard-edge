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

import { Injectable, NgModule } from '@angular/core';
import { Resolve, RouterModule, Routes } from '@angular/router';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { Observable } from 'rxjs';
import { OAuth2Service } from '@core/http/oauth2.service';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { AlarmTableComponent } from '@home/components/alarm/alarm-table.component';
import { AlarmRulesTableConfigResolver } from '@home/pages/alarm/alarm-rules-table-config.resolver';
import { AlarmsMode } from '@shared/models/alarm.models';

@Injectable()
export class OAuth2LoginProcessingUrlResolver implements Resolve<string> {

  constructor(private oauth2Service: OAuth2Service) {
  }

  resolve(): Observable<string> {
    return this.oauth2Service.getLoginProcessingUrl();
  }
}

const routes: Routes = [
  {
    path: 'alarm',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'alarm.alarms',
        icon: 'notifications'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/alarm/alarms'
        }
      },
      {
        path: 'alarms',
        component: AlarmTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'alarm.all-alarms',
          breadcrumb: {
            label: 'alarm.all-alarms',
            icon: 'notifications'
          },
          isPage: true,
          alarmsMode: AlarmsMode.ALL
        }
      },
      {
        path: 'rules',
        data: {
          breadcrumb: {
            label: 'alarm-rule.rules',
            icon: 'edit_notifications'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'alarm-rule.alarm-rules'
            },
            resolve: {
              entitiesTableConfig: AlarmRulesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'domain'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'alarm-rule.alarm-rules'
            },
            resolve: {
              entitiesTableConfig: AlarmRulesTableConfigResolver
            }
          }
        ]
      }
    ]
  },
  {
    path: 'alarms',
    component: AlarmTableComponent,
    data: {
      auth: [Authority.CUSTOMER_USER],
      title: 'alarm.alarms',
      breadcrumb: {
        label: 'alarm.alarms',
        icon: 'notifications'
      },
      isPage: true,
      alarmsMode: AlarmsMode.ALL
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AlarmRulesTableConfigResolver
  ]
})
export class AlarmRoutingModule { }
