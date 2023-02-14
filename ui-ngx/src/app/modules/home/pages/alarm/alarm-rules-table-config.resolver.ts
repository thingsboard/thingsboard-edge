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
import { Resolve, Router } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { AlarmRuleComponent } from '@home/pages/alarm/alarm-rule.component';
import { AlarmRuleTabsComponent } from '@home/pages/alarm/alarm-rule-tabs.component';
import { AlarmRule } from '@shared/models/alarm-rule.models';
import { emptyPageData } from '@shared/models/page/page-data';
import { of } from 'rxjs';

@Injectable()
export class AlarmRulesTableConfigResolver implements Resolve<EntityTableConfig<AlarmRule>> {

  private readonly config: EntityTableConfig<AlarmRule> = new EntityTableConfig<AlarmRule>();

  constructor(private importExport: ImportExportService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialogService: DialogService,
              private router: Router,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.ALARM_RULE;
    this.config.entityComponent = AlarmRuleComponent;
    this.config.entityTabsComponent = AlarmRuleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ALARM_RULE);
    this.config.entityResources = entityTypeResources.get(EntityType.ALARM_RULE);

    this.config.hideDetailsTabsOnEdit = false;

    this.config.columns.push(
      new DateEntityTableColumn<AlarmRule>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AlarmRule>('name', 'alarm-rule.name', '50%')
    );

    this.config.deleteEntityTitle = alarmRule => this.translate.instant('alarm-rule.delete-alarm-rule-title',
      { alarmRuleName: alarmRule.name });
    this.config.deleteEntityContent = () => this.translate.instant('alarm-rule.delete-alarm-rule-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('alarm-rule.delete-alarm-rules-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('alarm-rule.delete-alarm-rules-text');

    this.config.entitiesFetchFunction = pageLink => of(emptyPageData<AlarmRule>()); // TODO
    this.config.loadEntity = id => of(null); // TODO
    this.config.saveEntity = alarmRule => of(alarmRule); // TODO
    this.config.deleteEntity = id => of(null); // TODO
    this.config.onEntityAction = action => this.onAlarmRuleAction(action);
  }

  resolve(): EntityTableConfig<AlarmRule> {
    this.config.tableTitle = this.translate.instant('alarm-rule.alarm-rules');

    return this.config;
  }

  private openAlarmRule($event: Event, alarmRule: AlarmRule) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['alarm', 'rules', alarmRule.id.id]);
    this.router.navigateByUrl(url);
  }

  onAlarmRuleAction(action: EntityAction<AlarmRule>): boolean {
    switch (action.action) {
      case 'open':
        this.openAlarmRule(action.event, action.entity);
        return true;
    }
    return false;
  }

}
