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

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { WidgetsBundlesTableConfigResolver } from '@modules/home/pages/widget/widgets-bundles-table-config.resolver';
import { WidgetLibraryComponent } from '@home/pages/widget/widget-library.component';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { Observable } from 'rxjs';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@core/http/widget.service';
import { WidgetEditorComponent } from '@home/pages/widget/widget-editor.component';
import { map } from 'rxjs/operators';
import { detailsToWidgetInfo, toWidgetInfo, WidgetInfo } from '@home/models/widget-component.models';
import { widgetType, WidgetType, WidgetTypeDetails } from '@app/shared/models/widget.models';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { WidgetsData } from '@home/models/dashboard-component.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';

export interface WidgetEditorData {
  widgetTypeDetails: WidgetTypeDetails;
  widget: WidgetInfo;
}

@Injectable()
export class WidgetsBundleResolver implements Resolve<WidgetsBundle> {

  constructor(private widgetsService: WidgetService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<WidgetsBundle> {
    let widgetsBundleId = route.params.widgetsBundleId;
    if (!widgetsBundleId) {
      widgetsBundleId = route.parent.params.widgetsBundleId;
    }
    return this.widgetsService.getWidgetsBundle(widgetsBundleId);
  }
}

@Injectable()
export class WidgetsTypesDataResolver implements Resolve<WidgetsData> {

  constructor(private widgetsService: WidgetService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<WidgetsData> {
    const widgetsBundle: WidgetsBundle = route.parent.data.widgetsBundle;
    const bundleAlias = widgetsBundle.alias;
    const isSystem = widgetsBundle.tenantId.id === NULL_UUID;
    return this.widgetsService.loadBundleLibraryWidgets(bundleAlias,
      isSystem).pipe(
      map((widgets) => {
          return { widgets };
        }
      ));
  }
}

@Injectable()
export class WidgetEditorDataResolver implements Resolve<WidgetEditorData> {

  constructor(private widgetsService: WidgetService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<WidgetEditorData> {
    const widgetTypeId = route.params.widgetTypeId;
    return this.widgetsService.getWidgetTypeById(widgetTypeId).pipe(
      map((result) => {
        return {
          widgetTypeDetails: result,
          widget: detailsToWidgetInfo(result)
        };
      })
    );
  }
}

@Injectable()
export class WidgetEditorAddDataResolver implements Resolve<WidgetEditorData> {

  constructor(private widgetsService: WidgetService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<WidgetEditorData> {
    let widgetTypeParam = route.params.widgetType as widgetType;
    if (!widgetTypeParam) {
      widgetTypeParam = widgetType.timeseries;
    }
    return this.widgetsService.getWidgetTemplate(widgetTypeParam).pipe(
      map((widget) => {
        widget.widgetName = null;
        return {
          widgetTypeDetails: null,
          widget
        };
      })
    );
  }
}

export const widgetTypesBreadcumbLabelFunction: BreadCrumbLabelFunction<any> = ((route, translate) =>
  route.data.widgetsBundle.title);

export const widgetEditorBreadcumbLabelFunction: BreadCrumbLabelFunction<WidgetEditorComponent> =
  ((route, translate, component) => component ? component.widget.widgetName : '');

export const routes: Routes = [
  {
    path: 'widgets-bundles',
    data: {
      breadcrumb: {
        label: 'widgets-bundle.widgets-bundles',
        icon: 'now_widgets'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'widgets-bundle.widgets-bundles'
        },
        resolve: {
          entitiesTableConfig: WidgetsBundlesTableConfigResolver
        }
      },
      {
        path: ':widgetsBundleId/widgetTypes',
        data: {
          breadcrumb: {
            labelFunction: widgetTypesBreadcumbLabelFunction,
            icon: 'now_widgets'
          } as BreadCrumbConfig<any>
        },
        resolve: {
          widgetsBundle: WidgetsBundleResolver
        },
        children: [
          {
            path: '',
            component: WidgetLibraryComponent,
            data: {
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'widget.widget-library'
            },
            resolve: {
              widgetsData: WidgetsTypesDataResolver
            }
          },
          {
            path: ':widgetTypeId',
            component: WidgetEditorComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'widget.editor',
              breadcrumb: {
                labelFunction: widgetEditorBreadcumbLabelFunction,
                icon: 'insert_chart'
              } as BreadCrumbConfig<WidgetEditorComponent>
            },
            resolve: {
              widgetEditorData: WidgetEditorDataResolver
            }
          },
          {
            path: 'add/:widgetType',
            component: WidgetEditorComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'widget.editor',
              breadcrumb: {
                labelFunction: widgetEditorBreadcumbLabelFunction,
                icon: 'insert_chart'
              } as BreadCrumbConfig<WidgetEditorComponent>
            },
            resolve: {
              widgetEditorData: WidgetEditorAddDataResolver
            }
          }
        ]
      }
    ]
  }
];

// @dynamic
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    WidgetsBundlesTableConfigResolver,
    WidgetsBundleResolver,
    WidgetsTypesDataResolver,
    WidgetEditorDataResolver,
    WidgetEditorAddDataResolver
  ]
})
export class WidgetLibraryRoutingModule { }
