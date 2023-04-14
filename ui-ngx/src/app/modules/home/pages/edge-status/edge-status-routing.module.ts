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

import { Injectable, NgModule } from '@angular/core';
import { Resolve, RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { EdgeStatusComponent } from "@home/pages/edge-status/edge-status.component";
import { CloudEventTableComponent } from "@home/components/cloud-event/cloud-event-table.component";
import { EdgeService } from '@core/http/edge.service';
import { Observable } from 'rxjs';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { AttributeData, AttributeScope } from '@shared/models/telemetry/telemetry.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AttributeService } from '@core/http/attribute.service';

@Injectable()
export class EdgeAttributesResolver implements Resolve<Array<AttributeData>> {

  constructor(private edgeService: EdgeService,
              private store: Store<AppState>,
              private attributeService: AttributeService) {
  }

  resolve(): Observable<Array<AttributeData>> {
    const authUser = getCurrentAuthUser(this.store);
    const currentTenant: EntityId = {
      id: authUser.tenantId,
      entityType: EntityType.TENANT
    };
    return this.attributeService.getEntityAttributes(currentTenant, AttributeScope.SERVER_SCOPE)
  }

}

const routes: Routes = [
  {
    path: 'edge',
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'edge.edge',
      breadcrumb: {
        label: 'edge.edge',
        icon: 'router'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.TENANT_ADMIN],
          redirectTo: '/edge/status'
        }
      },
      {
        path: 'status',
        component: EdgeStatusComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'edge.status',
          breadcrumb: {
            label: 'edge.status',
            icon: 'info'
          }
        },
        resolve: {
          edgeAttributes: EdgeAttributesResolver
        }
      },
      {
        path: 'cloudEvents',
        component: CloudEventTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'edge.cloud-events',
          breadcrumb: {
            label: 'edge.cloud-events',
            icon: 'date_range'
          }
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    EdgeAttributesResolver
  ]
})
export class EdgeStatusRoutingModule { }
