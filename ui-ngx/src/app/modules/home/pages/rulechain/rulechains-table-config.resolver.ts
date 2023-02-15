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

import { ActivatedRouteSnapshot, Resolve, Router } from '@angular/router';
import {
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { RuleChain, RuleChainParams } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { DialogService } from '@core/services/dialog.service';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { EdgeService } from '@core/http/edge.service';
import { MatDialog } from '@angular/material/dialog';
import { UtilsService } from '@core/services/utils.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { RuleChainsTableConfig } from '@home/pages/rulechain/rulechains-table-config';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';

@Injectable()
export class RuleChainsTableConfigResolver implements Resolve<EntityTableConfig<RuleChain>> {

  constructor(private ruleChainService: RuleChainService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private importExport: ImportExportService,
              private itembuffer: ItemBufferService,
              private edgeService: EdgeService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private userPermissionsService: UserPermissionsService) {
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<RuleChain> {
    return this.resolveRuleChainsTableConfig(route);
  }

  resolveRuleChainsTableConfig(params: ActivatedRouteSnapshot | RuleChainParams): EntityTableConfig<RuleChain> {
    return new RuleChainsTableConfig (
      this.ruleChainService,
      this.dialogService,
      this.dialog,
      this.importExport,
      this.itembuffer,
      this.edgeService,
      this.translate,
      this.datePipe,
      this.router,
      this.utils,
      this.userPermissionsService,
      params
    );
  }
}
