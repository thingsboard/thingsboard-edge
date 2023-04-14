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
