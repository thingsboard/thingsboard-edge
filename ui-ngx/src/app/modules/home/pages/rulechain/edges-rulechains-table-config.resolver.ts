///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import {ActivatedRouteSnapshot, Resolve, Router} from '@angular/router';
import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn, defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig, HeaderActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RuleChain, ruleChainType } from '@shared/models/rule-chain.models';
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
import { forkJoin, Observable } from "rxjs";
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from "@home/dialogs/add-entities-to-edge-dialog.component";
import { MatDialog } from "@angular/material/dialog";
import { EdgeService } from "@core/http/edge.service";
import { Edge } from "@shared/models/edge.models";

@Injectable()
export class EdgesRuleChainsTableConfigResolver implements Resolve<EntityTableConfig<RuleChain>> {

  private readonly config: EntityTableConfig<RuleChain> = new EntityTableConfig<RuleChain>();
  private edgeId: string;
  private edge: Edge;

  constructor(private ruleChainService: RuleChainService,
              private edgeRuleChainService: EdgeRuleChainService,
              private edgeService: EdgeService,
              private dialogService: DialogService,
              private dialog: MatDialog,
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

    this.config.columns.push(
      new DateEntityTableColumn<RuleChain>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<RuleChain>('name', 'rulechain.name', '100%'),
      new EntityTableColumn<RuleChain>('root', 'rulechain.root', '60px',
        entity => {
          if (this.edgeId) {
            return checkBoxCell((this.edge.rootRuleChainId.id == entity.id.id));
          } else {
            return checkBoxCell(entity.root);
          }
        })
    );

    this.config.entityTitle = (ruleChain) => ruleChain ?
      this.utils.customTranslation(ruleChain.name, ruleChain.name) : '';

    this.config.deleteEntityTitle = ruleChain => this.translate.instant('rulechain.delete-rulechain-title',
      { ruleChainName: ruleChain.name });
    this.config.deleteEntityContent = () => this.translate.instant('rulechain.delete-rulechain-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('rulechain.delete-rulechains-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('rulechain.delete-rulechains-text');
    this.config.loadEntity = id => this.ruleChainService.getRuleChain(id.id);
    this.config.saveEntity = ruleChain => this.ruleChainService.saveRuleChain({...ruleChain, type: ruleChainType.edge});
    this.config.deleteEntity = id => this.ruleChainService.deleteRuleChain(id.id);
    this.config.onEntityAction = action => this.onRuleChainAction(action);
  }

  resolve(route:ActivatedRouteSnapshot): EntityTableConfig<RuleChain> {
    this.edgeId = route.params.edgeId;
    this.config.addActionDescriptors = this.configureAddActionDescriptor();
    this.config.cellActionDescriptors = this.configureCellActions();

    if (this.edgeId) {
      this.config.entitiesDeleteEnabled = false;
      this.config.deleteEnabled = () => false;
      if (!this.edge) {
        this.edgeService.getEdge(this.edgeId).subscribe((edge) => {
          this.edge = edge;
          this.config.tableTitle = edge.name + ': ' + this.translate.instant('rulechain.edge-rulechains');
        });
      }
      this.config.entitySelectionEnabled = (ruleChain) => this.edge.rootRuleChainId.id != ruleChain.id.id;
      this.config.entitiesFetchFunction = pageLink => this.edgeRuleChainService.getEdgeRuleChains(this.edgeId, pageLink);
      this.config.groupActionDescriptors = [];
      this.config.groupActionDescriptors.push(
        {
          name: this.translate.instant('rulechain.unassign-rulechains'),
          icon: 'portable_wifi_off',
          isEnabled: true,
          onAction: ($event, entities) => this.unassignRuleChainsFromEdge($event, entities)
        }
      )
    }
    else {
      this.config.entitiesDeleteEnabled = true;
      this.config.entitySelectionEnabled = (ruleChain) => ruleChain && !ruleChain.root &&
        this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
      this.config.deleteEnabled = (ruleChain) => ruleChain && !ruleChain.root &&
        this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.DELETE);
      this.config.tableTitle = this.translate.instant('rulechain.edge-rulechains');
      this.config.entitiesFetchFunction = pageLink => this.fetchEdgeRuleChains(pageLink);
    }

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
              tasks.push(this.edgeRuleChainService.unassignRuleChainFromEdge(this.edgeId, ruleChain.id.id));
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
          this.edgeRuleChainService.setRootRuleChain(this.edgeId, ruleChain.id.id).subscribe(
            (edge) => {
              this.edge = edge;
              this.config.table.updateData();
            }
          )
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

  configureAddActionDescriptor() {
    const actions: Array<HeaderActionDescriptor> = [];
    if (this.edgeId) {
      actions.push(
        {
          name: this.translate.instant('action.assign'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.addRuleChainsToEdge($event)
        }
      );
    } else {
      actions.push(
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
    return actions;
  }

  configureCellActions(): Array<CellActionDescriptor<RuleChain>> {
    const actions: Array<CellActionDescriptor<RuleChain>> = [];
    if (this.edgeId) {
      actions.push(
        {
          name: this.translate.instant('rulechain.open-rulechain'),
          icon: 'settings_ethernet',
          isEnabled: () => true,
          onAction: ($event, entity) => this.openRuleChain($event, entity)
        },
        {
          name: this.translate.instant('rulechain.set-root'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity),
          onAction: ($event, entity) => this.setRootRuleChain($event, entity)
        },
        {
          name: this.translate.instant('edge.unassign-from-edge'),
          icon: 'portable_wifi_off',
          isEnabled: (entity) => entity.id.id != this.edge.rootRuleChainId.id,
          onAction: ($event, entity) => this.unassignRuleChainFromEdge($event, entity)
        }
      );
    } else {
      actions.push(
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
    }
    return actions;
  }

  addRuleChainsToEdge($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      boolean>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.edgeId,
        entityType: EntityType.RULE_CHAIN
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.table.updateData();
        }
      });
  }

  // TODO move to service & fix router includes
  fetchEdgeRuleChains(pageLink: PageLink) {
    let defaultEdgeRuleChainIds: Array<string> = [];
    this.edgeRuleChainService.getDefaultEdgeRuleChains().subscribe(ruleChains => {
        ruleChains.map(ruleChain => defaultEdgeRuleChainIds.push(ruleChain.id.id))
    });
    return this.edgeRuleChainService.getRuleChains(pageLink).pipe(
      map(response => {
        response.data.map(ruleChain =>
          ruleChain.isDefault = defaultEdgeRuleChainIds.some(id => ruleChain.id.id.includes(id))
        );
        return response;
      })
    );
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

  unassignRuleChainFromEdge($event: Event, ruleChain: RuleChain) {
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
          this.edgeRuleChainService.unassignRuleChainFromEdge(this.edgeId, ruleChain.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  isNonRootRuleChain(ruleChain: RuleChain) {
    if (this.edgeId) {
      return (this.edge.rootRuleChainId && this.edge.rootRuleChainId != null && this.edge.rootRuleChainId.id != ruleChain.id.id);
    }
    return (ruleChain && !ruleChain.root);
  }

  isDefaultEdgeRuleChain(ruleChain) {
    return (ruleChain && !ruleChain.root && ruleChain.isDefault);
  }

  isNonDefaultEdgeRuleChain(ruleChain) {
    return (ruleChain && !ruleChain.root && !ruleChain.isDefault);
  }

}
