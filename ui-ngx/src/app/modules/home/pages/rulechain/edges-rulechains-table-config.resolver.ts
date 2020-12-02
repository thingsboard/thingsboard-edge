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
import {RuleChain, ruleChainType} from '@shared/models/rule-chain.models';
import { RuleChainService } from '@core/http/rule-chain.service';
import { RuleChainComponent } from '@modules/home/pages/rulechain/rulechain.component';
import { DialogService } from '@core/services/dialog.service';
import { RuleChainTabsComponent } from '@home/pages/rulechain/rulechain-tabs.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { ItemBufferService } from '@core/services/item-buffer.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UtilsService } from '@core/services/utils.service';
import { PageLink } from "@shared/models/page/page-link";
import { EdgeRuleChainService } from "@core/http/edge-rule-chain.service";
import { map } from "rxjs/operators";
import { isDefined } from "@core/utils";

@Injectable()
export class EdgesRuleChainsTableConfigResolver implements Resolve<EntityTableConfig<RuleChain>> {

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
          return checkBoxCell(entity.root);
        }),
    );

    this.config.addActionDescriptors.push(
      {
        name: this.translate.instant('rulechain.create-new-rulechain'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.config.table.addEntity($event)
      },
      {
        name: this.translate.instant('rulechain.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: ($event) => this.importRuleChain($event)
      }
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('rulechain.open-rulechain'),
        icon: 'settings_ethernet',
        isEnabled: () => true,
        onAction: ($event, entity) => this.openRuleChain($event, entity)
      },
      {
        name: this.translate.instant('rulechain.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportRuleChain($event, entity)
      },
      {
        name: this.translate.instant('rulechain.set-default-root-edge'),
        icon: 'flag',
        isEnabled: (entity) => this.isNonRootRuleChain(entity),
        onAction: ($event, entity) => this.setDefaultRootEdgeRuleChain($event, entity)
      },
      {
        name: this.translate.instant('rulechain.set-default-edge'),
        icon: 'bookmark_outline',
        isEnabled: (entity) => this.isNonDefaultEdgeRuleChain(entity),
        onAction: ($event, entity) => this.setDefaultEdgeRuleChain($event, entity)
      },
      {
        name: this.translate.instant('rulechain.remove-default-edge'),
        icon: 'bookmark',
        isEnabled: (entity) => this.isDefaultEdgeRuleChain(entity),
        onAction: ($event, entity) => this.removeDefaultEdgeRuleChain($event, entity)
      }
    );

    this.config.deleteEntityTitle = ruleChain => this.translate.instant('rulechain.delete-rulechain-title',
      { ruleChainName: ruleChain.name });
    this.config.deleteEntityContent = () => this.translate.instant('rulechain.delete-rulechain-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('rulechain.delete-rulechains-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('rulechain.delete-rulechains-text');
    this.config.entitiesFetchFunction = pageLink => this.fetchEdgeRuleChains(pageLink);
    this.config.loadEntity = id => this.ruleChainService.getRuleChain(id.id);
    this.config.saveEntity = ruleChain => this.ruleChainService.saveRuleChain({...ruleChain, type: ruleChainType.edge});
    this.config.deleteEntity = id => this.ruleChainService.deleteRuleChain(id.id);
    this.config.onEntityAction = action => this.onRuleChainAction(action);
    this.config.deleteEnabled = (ruleChain) => ruleChain && !ruleChain.root &&
      this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
    this.config.entitySelectionEnabled = (ruleChain) => ruleChain && !ruleChain.root &&
      this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
  }

  resolve(): EntityTableConfig<RuleChain> {
    this.config.tableTitle = this.translate.instant('rulechain.edge-rulechains');
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
        this.router.navigateByUrl(`ruleChains/edge/ruleChain/import`);
      }
    });
  }

  openRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.router.navigateByUrl(`ruleChains/edge/${ruleChain.id.id}`);
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
          this.ruleChainService.setRootRuleChain(ruleChain.id.id).subscribe(
            () => {
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

  addActionDescriptor() {
    if (this.router.url.includes('edgeGroups')) {
      this.config.addActionDescriptors.push(
        {
          name: this.translate.instant('action.assign'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.config.table.addEntity($event)
        }
      );
    } else {
      this.config.addActionDescriptors.push(
        {
          name: this.translate.instant('rulechain.create-new-rulechain'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.config.table.addEntity($event)
        },
        {
          name: this.translate.instant('rulechain.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importRuleChain($event)
        }
      );
    }
  }

  // TODO move to service & fix router includes
  fetchEdgeRuleChains(pageLink: PageLink) {
    let defaultEdgeRuleChainIds: Array<string> = [];
    this.edgeRuleChainService.getDefaultEdgeRuleChains().subscribe(ruleChains => {
        ruleChains.map(ruleChain => defaultEdgeRuleChainIds.push(ruleChain.id.id))
    });
    if (this.router.url.includes('edgeGroups')) {
      const edgeId = this.router.parseUrl(this.router.url).root.children["primary"].segments[2].path;
      return this.edgeRuleChainService.getEdgeRuleChains(edgeId, pageLink).pipe(
        map(response => {
          response.data.map(ruleChain =>
            ruleChain.isDefault = defaultEdgeRuleChainIds.some(id => ruleChain.id.id.includes(id))
          );
          return response;
        })
      );
    } else {
      return this.edgeRuleChainService.getRuleChains(pageLink).pipe(
        map(response => {
          response.data.map(ruleChain =>
            ruleChain.isDefault = defaultEdgeRuleChainIds.some(id => ruleChain.id.id.includes(id))
          );
          return response;
        })
      );
    }
  }

  setDefaultRootEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-default-root-edge-rulechain-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-default-root-edge-rulechain-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.edgeRuleChainService.setDefaultRootEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  setDefaultEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-default-edge-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-default-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.edgeRuleChainService.addDefaultEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          )
        }
      }
    );
  }

  removeDefaultEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.remove-default-edge-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.remove-default-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.edgeRuleChainService.removeDefaultEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          )
        }
      }
    );
  }

  isNonRootRuleChain(ruleChain: RuleChain) {
    return (isDefined(ruleChain)) && !ruleChain.root;
  }

  isDefaultEdgeRuleChain(ruleChain) {
    return (isDefined(ruleChain)) && !ruleChain.root && ruleChain.isDefault;
  }

  isNonDefaultEdgeRuleChain(ruleChain) {
    return (isDefined(ruleChain)) && !ruleChain.root && !ruleChain.isDefault;
  }

}
