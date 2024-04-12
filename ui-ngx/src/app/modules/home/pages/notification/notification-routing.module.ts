///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { InboxTableConfigResolver } from '@home/pages/notification/inbox/inbox-table-config.resolver';

const routes: Routes = [
  {
    path: 'notification',
    /** edge-only: Edge's notification center contains only Inbox
    component: RouterTabsComponent,
     */
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER, Authority.SYS_ADMIN],
      breadcrumb: {
        label: 'notification.notification-center',
        icon: 'mdi:message-badge'
      }
      /** edge-only: Edge's notification center contains only Inbox
      routerTabsHeaderComponent: SendNotificationButtonComponent
       */
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER, Authority.SYS_ADMIN],
          redirectTo: '/notification/inbox'
        }
      },
      {
        path: 'inbox',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER, Authority.SYS_ADMIN],
          title: 'notification.inbox',
          breadcrumb: {
            label: 'notification.inbox',
            icon: 'inbox'
          }
        },
        resolve: {
          entitiesTableConfig: InboxTableConfigResolver
        }
      }
      /** edge-only: Edge's notification center contains only Inbox
      {
        path: 'sent',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
          title: 'notification.sent',
          breadcrumb: {
            label: 'notification.sent',
            icon: 'outbox'
          }
        },
        resolve: {
          entitiesTableConfig: SentTableConfigResolver
        }
      },
      {
        path: 'templates',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
          title: 'notification.templates',
          breadcrumb: {
            label: 'notification.templates',
            icon: 'mdi:message-draw'
          }
        },
        resolve: {
          entitiesTableConfig: TemplateTableConfigResolver
        }
      },
      {
        path: 'recipients',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
          title: 'notification.recipients',
          breadcrumb: {
            label: 'notification.recipients',
            icon: 'contacts'
          },
        },
        resolve: {
          entitiesTableConfig: RecipientTableConfigResolver
        }
      },
      {
        path: 'rules',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
          title: 'notification.rules',
          breadcrumb: {
            label: 'notification.rules',
            icon: 'mdi:message-cog'
          }
        },
        resolve: {
          entitiesTableConfig: RuleTableConfigResolver
        }
      }
       */
    ]
  }
];

@NgModule({
  providers: [
    InboxTableConfigResolver
    /** edge-only: Edge's notification center contains only Inbox
    SentTableConfigResolver,
    RecipientTableConfigResolver,
    TemplateTableConfigResolver,
    RuleTableConfigResolver
     */
  ],
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class NotificationRoutingModule { }
