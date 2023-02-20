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
import { ActivatedRouteSnapshot, Resolve, RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { SolutionTemplatesComponent } from '@home/pages/solution-template/solution-templates.component';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { Observable } from 'rxjs';
import {
  SolutionTemplateDetails,
  SolutionTemplateInfo, TenantSolutionTemplateDetails,
  TenantSolutionTemplateInfo
} from '@shared/models/solution-template.models';
import { SolutionsService } from '@core/http/solutions.service';
import { SolutionTemplateDetailsComponent } from '@home/pages/solution-template/solution-template-details.component';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';

@Injectable()
export class SolutionTemplateInfosResolver implements Resolve<Array<TenantSolutionTemplateInfo>> {

  constructor(private solutionService: SolutionsService) {
  }

  resolve(): Observable<Array<TenantSolutionTemplateInfo>> {
    return this.solutionService.getSolutionTemplateInfos();
  }
}

@Injectable()
export class SolutionTemplateDetailsResolver implements Resolve<TenantSolutionTemplateDetails> {

  constructor(private solutionService: SolutionsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<TenantSolutionTemplateDetails> {
    const solutionTemplateId = route.params.solutionTemplateId;
    return this.solutionService.getSolutionTemplateDetails(solutionTemplateId);
  }
}

export const solutionTemplateBreadcumbLabelFunction: BreadCrumbLabelFunction<SolutionTemplateDetailsComponent>
  = ((route, translate, component) => {
  let label: string = component.solutionTemplateDetails.title;
  return label;
});

const routes: Routes = [
  {
    path: 'solutionTemplates',
    data: {
      breadcrumb: {
        label: 'solution-template.solution-templates',
        icon: 'apps'
      }
    },
    children: [
      {
        path: '',
        component: SolutionTemplatesComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          canActivate: (userPermissionsService: UserPermissionsService): boolean => {
            return userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL);
          },
          title: 'solution-template.solution-templates'
        },
        resolve: {
          solutionTemplates: SolutionTemplateInfosResolver
        }
      },
      {
        path: ':solutionTemplateId',
        component: SolutionTemplateDetailsComponent,
        data: {
          breadcrumb: {
            labelFunction: solutionTemplateBreadcumbLabelFunction,
            icon: 'apps'
          } as BreadCrumbConfig<SolutionTemplateDetailsComponent>,
          auth: [Authority.TENANT_ADMIN],
          canActivate: (userPermissionsService: UserPermissionsService): boolean => {
            return userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL);
          },
          title: 'solution-template.solution-template',
        },
        resolve: {
          solutionTemplateDetails: SolutionTemplateDetailsResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    SolutionTemplateInfosResolver,
    SolutionTemplateDetailsResolver
  ]
})
export class SolutionTemplatesRoutingModule { }
