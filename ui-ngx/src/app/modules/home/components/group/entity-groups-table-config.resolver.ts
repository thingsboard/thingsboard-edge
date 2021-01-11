///
/// Copyright © 2016-2021 The Thingsboard Authors
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

@Injectable()
export class EntityGroupsTableConfigResolver implements Resolve<EntityGroupsTableConfig> {

  constructor(private entityGroupService: EntityGroupService,
              private customerService: CustomerService,
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
      return this.customerService.getShortCustomerInfo(config.customerId).pipe(
        map((info) => {
          config.tableTitle = info.title + ': ' + this.translate.instant(entityGroupsTitle(config.groupType));
          return config;
        })
      );
    } else if (config.customerId && customerTitle){
      config.tableTitle = customerTitle + ': ' + this.translate.instant(entityGroupsTitle(config.groupType));
      return config;
    } else {
      return config;
    }
  }
}
