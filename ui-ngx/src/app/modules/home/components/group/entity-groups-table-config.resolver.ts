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

import { Injectable } from '@angular/core';

import { ActivatedRoute, ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { UtilsService } from '@core/services/utils.service';
import { EntityGroupParams, entityGroupsTitle, resolveGroupParams } from '@shared/models/entity-group.models';
import { EntityGroupService } from '@core/http/entity-group.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BroadcastService } from '@core/services/broadcast.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { CustomerService } from '@core/http/customer.service';
import { EntityGroupsTableConfig } from './entity-groups-table-config';
import { MatDialog } from '@angular/material/dialog';
import { EdgeService } from '@core/http/edge.service';

@Injectable()
export class EntityGroupsTableConfigResolver implements Resolve<EntityGroupsTableConfig> {

  constructor(private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
              private edgeService: EdgeService,
              private userPermissionsService: UserPermissionsService,
              private broadcast: BroadcastService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private utils: UtilsService,
              private route: ActivatedRoute,
              private router: Router,
              private dialog: MatDialog,
              private homeDialogs: HomeDialogsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<EntityGroupsTableConfig> | EntityGroupsTableConfig {
    return this.resolveEntityGroupTableConfig(resolveGroupParams(route));
  }

  resolveEntityGroupTableConfig(params: EntityGroupParams, resolveCustomer = true, customerTitle?: string):
    Observable<EntityGroupsTableConfig> | EntityGroupsTableConfig {

    const config = new EntityGroupsTableConfig(
      this.entityGroupService,
      this.customerService,
      this.userPermissionsService,
      this.broadcast,
      this.translate,
      this.datePipe,
      this.utils,
      this.route,
      this.router,
      this.dialog,
      this.homeDialogs,
      params
    );

    if (config.customerId && resolveCustomer) {
      if (config.edgeId) {
        return this.resolveEdgeInfo(config);
      } else {
        return this.customerService.getShortCustomerInfo(config.customerId).pipe(
          map((info) => {
            config.tableTitle = info.title + ': ' + this.translate.instant(entityGroupsTitle(config.groupType));
            return config;
          })
        );
      }
    } else if (config.customerId && customerTitle){
      config.tableTitle = customerTitle + ': ' + this.translate.instant(entityGroupsTitle(config.groupType));
      return config;
    } else if (config.edgeId && resolveCustomer) {
      return this.resolveEdgeInfo(config);
    } else {
      return config;
    }
  }

  private resolveEdgeInfo(config: EntityGroupsTableConfig): Observable<EntityGroupsTableConfig> {
    return this.edgeService.getEdge(config.edgeId).pipe(
      map((info) => {
        config.tableTitle = info.name + ': ' + this.translate.instant(entityGroupsTitle(config.groupType));
        return config;
      })
    );
  }

}
