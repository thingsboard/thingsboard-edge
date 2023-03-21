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

import { ActivatedRoute, RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { Component, NgModule, OnInit } from '@angular/core';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-notification-temp-component',
  template: '<div>{{text}}</div>',
  styleUrls: []
})
class NotificationTempComponent implements OnInit {

  text: string;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    if (isDefinedAndNotNull(this.route.snapshot.data.text)) {
      this.text = this.route.snapshot.data.text;
    }
  }
}

const routes: Routes = [
  {
    path: 'notification',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'notification.notification-center',
        icon: 'mdi:message-badge'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: '/notification/inbox'
        }
      },
      {
        path: 'inbox',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'notification.inbox',
          breadcrumb: {
            label: 'notification.inbox',
            icon: 'inbox'
          },
          text: 'TODO: Implement inbox'
        }
      },
      {
        path: 'sent',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'notification.sent',
          breadcrumb: {
            label: 'notification.sent',
            icon: 'outbox'
          },
          text: 'TODO: Implement sent'
        }
      },
      {
        path: 'templates',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'notification.templates',
          breadcrumb: {
            label: 'notification.templates',
            icon: 'mdi:message-draw'
          },
          text: 'TODO: Implement templates'
        }
      },
      {
        path: 'recipients',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'notification.recipients',
          breadcrumb: {
            label: 'notification.recipients',
            icon: 'contacts'
          },
          text: 'TODO: Implement recipients'
        }
      },
      {
        path: 'rules',
        component: NotificationTempComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'notification.rules',
          breadcrumb: {
            label: 'notification.rules',
            icon: 'mdi:message-cog'
          },
          text: 'TODO: Implement rules'
        }
      }
    ]
  }
];

@NgModule({
  declarations: [NotificationTempComponent],
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class NotificationRoutingModule { }
