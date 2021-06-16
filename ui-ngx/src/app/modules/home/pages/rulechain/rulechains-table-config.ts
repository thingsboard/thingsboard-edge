import {
  CellActionDescriptor,
  checkBoxCell,
  DateEntityTableColumn,
  EntityColumn, EntityTableColumn,
  EntityTableConfig, GroupActionDescriptor, HeaderActionDescriptor
} from "@home/models/entity/entities-table-config.models";
import { RuleChain, RuleChainType } from "@shared/models/rule-chain.models";
import { RuleChainService } from "@core/http/rule-chain.service";
import { DialogService } from "@core/services/dialog.service";
import { MatDialog } from "@angular/material/dialog";
import { ImportExportService } from "@home/components/import-export/import-export.service";
import { ItemBufferService } from "@core/services/item-buffer.service";
import { EdgeService } from "@core/http/edge.service";
import { TranslateService } from "@ngx-translate/core";
import { DatePipe } from "@angular/common";
import { Router, UrlTree } from "@angular/router";
import { UtilsService } from "@core/services/utils.service";
import { UserPermissionsService } from "@core/http/user-permissions.service";
import { EntityGroupParams } from "@shared/models/entity-group.models";
import { EntityType, entityTypeResources, entityTypeTranslations } from "@shared/models/entity-type.models";
import { RuleChainComponent } from "@home/pages/rulechain/rulechain.component";
import { RuleChainTabsComponent } from "@home/pages/rulechain/rulechain-tabs.component";
import { forkJoin, Observable } from "rxjs";
import { PageData } from "@shared/models/page/page-data";
import { Edge } from "@shared/models/edge.models";
import { Operation, Resource } from "@shared/models/security.models";
import { isUndefined } from "@core/utils";
import { EntityAction } from "@home/models/entity/entity-component.models";
import {
  AddEntitiesToEdgeDialogComponent,
  AddEntitiesToEdgeDialogData
} from "@home/dialogs/add-entities-to-edge-dialog.component";
import { PageLink } from "@shared/models/page/page-link";
import { mergeMap } from "rxjs/operators";

export class RulechainsTableConfig extends EntityTableConfig<RuleChain> {

  constructor(private ruleChainService: RuleChainService,
              private dialogService: DialogService,
              private dialog: MatDialog,
              private importExport: ImportExportService,
              private itembuffer: ItemBufferService,
              private edgeService: EdgeService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private router: Router,
              private utils: UtilsService,
              private userPermissionsService: UserPermissionsService) {
    super();

    this.entityType = EntityType.RULE_CHAIN;
    this.entityComponent = RuleChainComponent;
    this.entityTabsComponent = RuleChainTabsComponent;
    this.entityTranslations = entityTypeTranslations.get(EntityType.RULE_CHAIN);
    this.entityResources = entityTypeResources.get(EntityType.RULE_CHAIN);

    this.deleteEntityTitle = ruleChain => this.translate.instant('rulechain.delete-rulechain-title',
      {ruleChainName: ruleChain.name});
    this.deleteEntityContent = () => this.translate.instant('rulechain.delete-rulechain-text');
    this.deleteEntitiesTitle = count => this.translate.instant('rulechain.delete-rulechains-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('rulechain.delete-rulechains-text');
    this.loadEntity = id => this.ruleChainService.getRuleChain(id.id);
    this.saveEntity = ruleChain => this.saveRuleChain(ruleChain);
    this.deleteEntity = id => this.ruleChainService.deleteRuleChain(id.id);
    this.onEntityAction = action => this.onRuleChainAction(action);

  }

  private configureEntityTableColumns(ruleChainScope: string): Array<EntityColumn<RuleChain>> {
    const columns: Array<EntityColumn<RuleChain>> = [];

    this.entityTitle = (ruleChain) => ruleChain ?
      this.utils.customTranslation(ruleChain.name, ruleChain.name) : '';

    columns.push(
      new DateEntityTableColumn<RuleChain>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<RuleChain>('name', 'rulechain.name', '100%', this.entityTitle)
    );
    if (ruleChainScope === 'tenant' || ruleChainScope === 'edge') {
      columns.push(
        new EntityTableColumn<RuleChain>('root', 'rulechain.root', '60px',
          entity => {
            if (ruleChainScope === 'edge') {
              return checkBoxCell((this.componentsData.edge.rootRuleChainId.id === entity.id.id));
            } else {
              return checkBoxCell(entity.root);
            }
          })
      );
    } else if (ruleChainScope === 'edges') {
      columns.push(
        new EntityTableColumn<RuleChain>('root', 'rulechain.edge-template-root', '60px',
          entity => {
            return checkBoxCell(entity.root);
          }),
        new EntityTableColumn<RuleChain>('assignToEdge', 'rulechain.assign-to-edge', '60px',
          entity => {
            return checkBoxCell(this.isAutoAssignToEdgeRuleChain(entity));
          })
      );
    }
    return columns;
  }

  private configureAddActions(ruleChainScope: string): Array<HeaderActionDescriptor> {
    const actions: Array<HeaderActionDescriptor> = [];
    if (ruleChainScope === 'tenant' || ruleChainScope === 'edges') {
      actions.push(
        {
          name: this.translate.instant('rulechain.create-new-rulechain'),
          icon: 'insert_drive_file',
          isEnabled: () => true,
          onAction: ($event) => this.table.addEntity($event)
        },
        {
          name: this.translate.instant('rulechain.import'),
          icon: 'file_upload',
          isEnabled: () => true,
          onAction: ($event) => this.importRuleChain($event)
        }
      );
    }
    if (ruleChainScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('edge.assign-to-edge'),
          icon: 'add',
          isEnabled: () => true,
          onAction: ($event) => this.assignRuleChainsToEdge($event)
        }
      );
    }
    return actions;
  }

  private configureEntityFunctions(ruleChainScope: string, edgeId: string): (pageLink) => Observable<PageData<RuleChain>> {
    if (ruleChainScope === 'tenant') {
      return pageLink => this.fetchRuleChains(pageLink);
    } else if (ruleChainScope === 'edges') {
      return pageLink => this.fetchEdgeRuleChains(pageLink);
    } else if (ruleChainScope === 'edge') {
      return pageLink => this.ruleChainService.getEdgeRuleChains(edgeId, pageLink);
    }
  }

  private configureTableTitle(ruleChainScope: string, edge: Edge): string {
    if (ruleChainScope === 'tenant') {
      return this.translate.instant('rulechain.rulechains');
    } else if (ruleChainScope === 'edges') {
      return this.translate.instant('edge.rulechain-templates');
    } else if (ruleChainScope === 'edge') {
      return this.tableTitle = edge.name + ': ' + this.translate.instant('edge.rulechains');
    }
  }

  private configureGroupActions(ruleChainScope: string): Array<GroupActionDescriptor<RuleChain>> {
    const actions: Array<GroupActionDescriptor<RuleChain>> = [];
    if (ruleChainScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('rulechain.unassign-rulechains'),
          icon: 'assignment_return',
          isEnabled: this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE),
          onAction: ($event, entities) => this.unassignRuleChainsFromEdge($event, entities)
        }
      );
    }
    return actions;
  }

  private configureCellActions(ruleChainScope: string, params?: EntityGroupParams): Array<CellActionDescriptor<RuleChain>> {
    const actions: Array<CellActionDescriptor<RuleChain>> = [];
    actions.push(
      {
        name: this.translate.instant('rulechain.open-rulechain'),
        icon: 'settings_ethernet',
        isEnabled: () => true,
        onAction: ($event, entity) => this.openRuleChain($event, entity, params)
      },
      {
        name: this.translate.instant('rulechain.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportRuleChain($event, entity)
      }
    );
    if (ruleChainScope === 'tenant') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-root'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity) &&
            this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.WRITE),
          onAction: ($event, entity) => this.setRootRuleChain($event, entity)
        }
      );
    }
    if (ruleChainScope === 'edges') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-edge-template-root-rulechain'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity) &&
            this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.WRITE),
          onAction: ($event, entity) => this.setEdgeTemplateRootRuleChain($event, entity)
        },
        {
          name: this.translate.instant('rulechain.set-auto-assign-to-edge'),
          icon: 'bookmark_outline',
          isEnabled: (entity) => this.isNotAutoAssignToEdgeRuleChain(entity) &&
            this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.WRITE),
          onAction: ($event, entity) => this.setAutoAssignToEdgeRuleChain($event, entity)
        },
        {
          name: this.translate.instant('rulechain.unset-auto-assign-to-edge'),
          icon: 'bookmark',
          isEnabled: (entity) => this.isAutoAssignToEdgeRuleChain(entity) &&
            this.userPermissionsService.hasGenericPermission(Resource.RULE_CHAIN, Operation.WRITE),
          onAction: ($event, entity) => this.unsetAutoAssignToEdgeRuleChain($event, entity)
        }
      );
    }
    if (ruleChainScope === 'edge') {
      actions.push(
        {
          name: this.translate.instant('rulechain.set-root'),
          icon: 'flag',
          isEnabled: (entity) => this.isNonRootRuleChain(entity) &&
            this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE),
          onAction: ($event, entity) => this.setEdgeRootRuleChain($event, entity)
        },
        {
          name: this.translate.instant('edge.unassign-from-edge'),
          icon: 'assignment_return',
          isEnabled: (entity) => entity.id.id !== this.componentsData.edge.rootRuleChainId.id &&
            this.userPermissionsService.hasGenericPermission(Resource.EDGE, Operation.WRITE),
          onAction: ($event, entity) => this.unassignFromEdge($event, entity)
        }
      );
    }
    return actions;
  }

  private importRuleChain($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const expectedRuleChainType = this.componentsData.ruleChainScope === 'tenant' ? RuleChainType.CORE : RuleChainType.EDGE;
    this.importExport.importRuleChain(expectedRuleChainType).subscribe((ruleChainImport) => {
      if (ruleChainImport) {
        this.itembuffer.storeRuleChainImport(ruleChainImport);
        if (this.componentsData.ruleChainScope === 'edges') {
          this.router.navigateByUrl(`edges/ruleChains/ruleChain/import`);
        } else {
          this.router.navigateByUrl(`ruleChains/ruleChain/import`);
        }
      }
    });
  }

  private openRuleChain($event: Event, ruleChain: RuleChain, params?: EntityGroupParams) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.componentsData.ruleChainScope === 'edge') {
      var url: UrlTree;
      if (params && params.hierarchyView) {
        url = this.router.createUrlTree(['customerGroups', params.customerGroupId, params.customerId,
          'edgeGroups', params.entityGroupId, params.edgeId, 'ruleChains', ruleChain.id.id]);
        window.open(window.location.origin + url, '_blank');
      } else {
        url = this.router.createUrlTree([ruleChain.id.id], { relativeTo: this.table.route });
        this.router.navigateByUrl(url);
      }
    } else if (this.componentsData.ruleChainScope === 'edges') {
      this.router.navigateByUrl(`edges/ruleChains/${ruleChain.id.id}`);
    } else if (this.componentsData.ruleChainScope === 'edge') {
      this.router.navigateByUrl(`edges/${this.componentsData.edgeId}/ruleChains/${ruleChain.id.id}`);
    } else {
      this.router.navigateByUrl(`ruleChains/${ruleChain.id.id}`);
    }
  }

  private saveRuleChain(ruleChain: RuleChain) {
    if (isUndefined(ruleChain.type)) {
      if (this.componentsData.ruleChainScope === 'tenant') {
        ruleChain.type = RuleChainType.CORE;
      } else if (this.componentsData.ruleChainScope === 'edges') {
        ruleChain.type = RuleChainType.EDGE;
      } else {
        // safe fallback to default core type
        ruleChain.type = RuleChainType.CORE;
      }
    }
    return this.ruleChainService.saveRuleChain(ruleChain);
  }

  private exportRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExport.exportRuleChain(ruleChain.id.id);
  }

  private setRootRuleChain($event: Event, ruleChain: RuleChain) {
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
              this.table.updateData();
            }
          );
        }
      }
    );
  }

  private setEdgeRootRuleChain($event: Event, ruleChain: RuleChain) {
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
          this.ruleChainService.setEdgeRootRuleChain(this.componentsData.edgeId, ruleChain.id.id).subscribe(
            (edge) => {
              this.componentsData.edge = edge;
              this.table.updateData();
            }
          );
        }
      }
    );
  }

  private onRuleChainAction(action: EntityAction<RuleChain>): boolean {
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
      case 'setEdgeRoot':
        this.setEdgeRootRuleChain(action.event, action.entity);
        return true;
      case 'setEdgeTemplateRoot':
        this.setEdgeTemplateRootRuleChain(action.event, action.entity);
        return true;
      case 'unassignFromEdge':
        this.unassignFromEdge(action.event, action.entity);
        return true;
      case 'setAutoAssignToEdge':
        this.setAutoAssignToEdgeRuleChain(action.event, action.entity);
        return true;
      case 'unsetAutoAssignToEdge':
        this.unsetAutoAssignToEdgeRuleChain(action.event, action.entity);
        return true;
    }
    return false;
  }

  private setEdgeTemplateRootRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-edge-template-root-rulechain-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-edge-template-root-rulechain-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.setEdgeTemplateRootRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.table.updateData();
            }
          );
        }
      }
    );
  }

  private assignRuleChainsToEdge($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<AddEntitiesToEdgeDialogComponent, AddEntitiesToEdgeDialogData,
      boolean>(AddEntitiesToEdgeDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        edgeId: this.componentsData.edgeId,
        entityType: EntityType.RULE_CHAIN
      }
    }).afterClosed()
      .subscribe((res) => {
          if (res) {
            this.edgeService.findMissingToRelatedRuleChains(this.componentsData.edgeId).subscribe(
              (missingRuleChains) => {
                if (missingRuleChains && Object.keys(missingRuleChains).length > 0) {
                  const formattedMissingRuleChains: Array<string> = new Array<string>();
                  for (const missingRuleChain of Object.keys(missingRuleChains)) {
                    const arrayOfMissingRuleChains = missingRuleChains[missingRuleChain];
                    const tmp = '- \'' + missingRuleChain + '\': \'' + arrayOfMissingRuleChains.join('\', ') + '\'';
                    formattedMissingRuleChains.push(tmp);
                  }
                  const message = this.translate.instant('edge.missing-related-rule-chains-text',
                    {missingRuleChains: formattedMissingRuleChains.join('<br>')});
                  this.dialogService.alert(this.translate.instant('edge.missing-related-rule-chains-title'),
                    message, this.translate.instant('action.close'), true).subscribe(
                    () => {
                      this.table.updateData();
                    }
                  );
                } else {
                  this.table.updateData();
                }
              }
            );
          }
        }
      );
  }

  private unassignFromEdge($event: Event, ruleChain: RuleChain) {
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
          this.ruleChainService.unassignRuleChainFromEdge(this.componentsData.edgeId, ruleChain.id.id).subscribe(
            () => {
              this.table.updateData();
            }
          );
        }
      }
    );
  }

  private unassignRuleChainsFromEdge($event: Event, ruleChains: Array<RuleChain>) {
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
              tasks.push(this.ruleChainService.unassignRuleChainFromEdge(this.componentsData.edgeId, ruleChain.id.id));
            }
          );
          forkJoin(tasks).subscribe(
            () => {
              this.table.updateData();
            }
          );
        }
      }
    );
  }

  private setAutoAssignToEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.set-auto-assign-to-edge-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.set-auto-assign-to-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.setAutoAssignToEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.table.updateData();
            }
          );
        }
      }
    );
  }

  private unsetAutoAssignToEdgeRuleChain($event: Event, ruleChain: RuleChain) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('rulechain.unset-auto-assign-to-edge-title', {ruleChainName: ruleChain.name}),
      this.translate.instant('rulechain.unset-auto-assign-to-edge-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.ruleChainService.unsetAutoAssignToEdgeRuleChain(ruleChain.id.id).subscribe(
            () => {
              this.table.updateData();
            }
          );
        }
      }
    );
  }

  private isNonRootRuleChain(ruleChain: RuleChain) {
    if (this.componentsData.ruleChainScope === 'edge') {
      return this.componentsData.edge.rootRuleChainId &&
        this.componentsData.edge.rootRuleChainId.id !== ruleChain.id.id;
    }
    return !ruleChain.root;
  }

  private isAutoAssignToEdgeRuleChain(ruleChain) {
    return !ruleChain.root && this.componentsData.autoAssignToEdgeRuleChainIds.includes(ruleChain.id.id);
  }

  private isNotAutoAssignToEdgeRuleChain(ruleChain) {
    return !ruleChain.root && !this.componentsData.autoAssignToEdgeRuleChainIds.includes(ruleChain.id.id);
  }

  private fetchRuleChains(pageLink: PageLink) {
    return this.ruleChainService.getRuleChains(pageLink, RuleChainType.CORE);
  }

  private fetchEdgeRuleChains(pageLink: PageLink) {
    return this.ruleChainService.getAutoAssignToEdgeRuleChains().pipe(
      mergeMap((ruleChains) => {
        this.componentsData.autoAssignToEdgeRuleChainIds = [];
        ruleChains.map(ruleChain => this.componentsData.autoAssignToEdgeRuleChainIds.push(ruleChain.id.id));
        return this.ruleChainService.getRuleChains(pageLink, RuleChainType.EDGE);
      })
    );
  }
}
