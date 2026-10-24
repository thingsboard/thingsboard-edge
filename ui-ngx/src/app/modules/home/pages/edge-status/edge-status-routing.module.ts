// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
import { MenuId } from '@core/services/menu.models';

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
        menuId: MenuId.edge
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
            menuId: MenuId.edge_status
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
            menuId: MenuId.cloud_events
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
