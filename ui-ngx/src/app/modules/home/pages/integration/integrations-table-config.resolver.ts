///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import {
  Integration,
  IntegrationInfo,
  IntegrationParams,
  resolveIntegrationParams
} from '@shared/models/integration.models';
import { IntegrationService } from '@core/http/integration.service';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EdgeService } from '@core/http/edge.service';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { IntegrationsTableConfig } from '@home/pages/integration/integrations-table-config';
import { PageLink } from '@shared/models/page/page-link';

@Injectable()
export class IntegrationsTableConfigResolver implements Resolve<EntityTableConfig<Integration, PageLink, IntegrationInfo>> {

  constructor(private integrationService: IntegrationService,
              private userPermissionsService: UserPermissionsService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private dialogService: DialogService,
              private dialog: MatDialog) {
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<Integration, PageLink, IntegrationInfo> {
    return this.resolveIntegrationsTableConfig(resolveIntegrationParams(route));
  }

  resolveIntegrationsTableConfig(params: IntegrationParams): EntityTableConfig<Integration, PageLink, IntegrationInfo> {
    return new IntegrationsTableConfig(
      this.integrationService,
      this.userPermissionsService,
      this.edgeService,
      this.translate,
      this.datePipe,
      this.router,
      this.utils,
      this.dialogService,
      this.dialog,
      params
    );
  }
}
