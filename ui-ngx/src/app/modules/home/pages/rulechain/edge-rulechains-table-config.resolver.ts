///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
  checkBoxCell,
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RuleChain } from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainComponent } from '@modules/home/pages/rulechain/rulechain.component';
import { DialogService } from '@core/services/dialog.service';
import { RuleChainTabsComponent } from '@home/pages/rulechain/rulechain-tabs.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UtilsService } from '@core/services/utils.service';
import {EdgeRuleChainService} from "@core/http/edge-rule-chain.service";
import {forkJoin, Observable} from "rxjs";
import {isDefined} from "@core/utils";

@Injectable()
export class EdgeRuleChainsTableConfigResolver implements Resolve<EntityTableConfig<RuleChain>> {

  private readonly config: EntityTableConfig<RuleChain> = new EntityTableConfig<RuleChain>();

  constructor(private ruleChainService: RuleChainService,
              private edgeRuleChainService: EdgeRuleChainService,
              private dialogService: DialogService,
              private importExport: ImportExportService,
              private itembuffer: ItemBufferService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private userPermissionsService: UserPermissionsService) {

    this.config.entityType = EntityType.RULE_CHAIN;
    this.config.entityComponent = RuleChainComponent;
    this.config.entityTabsComponent = RuleChainTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.RULE_CHAIN);
    this.config.entityResources = entityTypeResources.get(EntityType.RULE_CHAIN);

    this.config.entityTitle = (ruleChain) => ruleChain ?
      this.utils.customTranslation(ruleChain.name, ruleChain.name) : '';

    this.config.columns.push(
      new DateEntityTableColumn<RuleChain>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<RuleChain>('name', 'rulechain.name', '100%', this.config.entityTitle),
      new EntityTableColumn<RuleChain>('root', 'rulechain.root', '60px',
        entity => {
          return checkBoxCell((this.config.componentsData.edge.rootRuleChainId.id == entity.id.id));
        }),
    );

    this.config.addActionDescriptors.push(
      {
        name: this.translate.instant('rulechain.assign-new-rulechain'),
        icon: 'add',
        isEnabled: () => true,
        onAction: ($event) => this.addRuleChainsToEdge($event)
      }
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('rulechain.set-root'),
        icon: 'flag',
        isEnabled: (entity) => this.isNonRootRuleChain(entity),
        onAction: ($event, entity) => this.setRootRuleChain($event, entity)
      },
      {
        name: this.translate.instant('edge.unassign-from-edge'),
        icon: 'portable_wifi_off',
        isEnabled: (entity) => entity.id.id != this.config.componentsData.edge.rootRuleChainId.id,
        onAction: ($event, entity) => this.unassignFromEdge($event, entity)
      }
    );

    this.config.groupActionDescriptors.push(
      {
        name: this.translate.instant('rulechain.unassign-rulechains'),
        icon: 'portable_wifi_off',
        isEnabled: true,
        onAction: ($event, entities) => this.unassignRuleChainsFromEdge($event, entities)
      }
    )

    this.config.deleteEntityTitle = ruleChain => this.translate.instant('rulechain.delete-rulechain-title',
      { ruleChainName: ruleChain.name });
    this.config.deleteEntityContent = () => this.translate.instant('rulechain.delete-rulechain-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('rulechain.delete-rulechains-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('rulechain.delete-rulechains-text');

    this.config.entitiesFetchFunction = pageLink => this.edgeRuleChainService.getEdgeRuleChains(this.config.componentsData.edgeId, pageLink);
    this.config.loadEntity = id => this.ruleChainService.getRuleChain(id.id);
    this.config.saveEntity = ruleChain => this.ruleChainService.saveRuleChain(ruleChain);
    this.config.deleteEntity = id => this.ruleChainService.deleteRuleChain(id.id);
    this.config.onEntityAction = action => this.onRuleChainAction(action);
    this.config.deleteEnabled = (ruleChain) => ruleChain && !ruleChain.root &&
      this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
    this.config.entitySelectionEnabled = (ruleChain) => ruleChain && !ruleChain.root &&
      this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
  }

  resolve(): EntityTableConfig<RuleChain> {
    this.config.tableTitle = this.translate.instant('rulechain.rulechains');
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  importRuleChain($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.importRuleChain().subscribe((ruleChainImport) => {
      if (ruleChainImport) {
        this.itembuffer.storeRuleChainImport(ruleChainImport);
        this.router.navigateByUrl(`ruleChains/ruleChain/import`);
      }
    });
  }

  openRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`ruleChains/${ruleChain.id.id}`);
  }

  exportRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportRuleChain(ruleChain.id.id);
  }

  setRootRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-root-rulechain-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-root-rulechain-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.edgeRuleChainService.setRootRuleChain(this.config.componentsData.edgeId, ruleChain.id.id).subscribe(
            (edge) => {
              this.config.componentsData.edge = edge;
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  onRuleChainAction(action: EntityAction<RuleChain>): boolean {
    switch (action.action) {
      case 'open':
        this.openRuleChain(action.event, action.entity);
        return true;
      case 'export':
        this.exportRuleChain(action.event, action.entity);
        return true;
      case 'setRoot':
        this.setRootRuleChain(action.event, action.entity);
        return true;
    }
    return false;
  }

  addRuleChainsToEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    // this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
    //   boolean>(AddEntitiesToEdgeDialogComponent, {
    //   disableClose: true,
    //   panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
    //   data: {
    //     edgeId: this.config.componentsData.edgeId,
    //     entityType: EntityType.RULE_CHAIN
    //   }
    // }).afterClosed()
    //   .subscribe((res) => {
    //       if (res) {
    //         this.config.table.updateData();
    //       }
    //     }
    //   )
  }

  unassignFromEdge($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.unassign-rulechain-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.unassign-rulechain-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.edgeRuleChainService.unassignRuleChainFromEdge(this.config.componentsData.edgeId, ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  unassignRuleChainsFromEdge($event: Event, ruleChains: Array<RuleChain>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.unassign-rulechains-from-edge-title', {count: ruleChains.length}),
      this.translate.instant('rulechain.unassign-rulechains-from-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          const tasks: Observable<any>[] = [];
          ruleChains.forEach(
            (ruleChain) => {
              tasks.push(this.edgeRuleChainService.unassignRuleChainFromEdge(this.config.componentsData.edgeId, ruleChain.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  isNonRootRuleChain(ruleChain: RuleChain) {
    return (isDefined(this.config.componentsData.edge.rootRuleChainId) && this.config.componentsData.edge.rootRuleChainId != null && this.config.componentsData.edge.rootRuleChainId.id != ruleChain.id.id);
  }

  isDefaultEdgeRuleChain(ruleChain) {
    return (isDefined(ruleChain)) && !ruleChain.root && ruleChain.isDefault;
  }

  isNonDefaultEdgeRuleChain(ruleChain) {
    return (isDefined(ruleChain)) && !ruleChain.root && !ruleChain.isDefault;
  }

}
