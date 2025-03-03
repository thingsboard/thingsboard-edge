///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { EntityTableColumn, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
import { PageLink } from '@shared/models/page/page-link';
import { Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { MINUTE } from '@shared/models/time/time.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DestroyRef, Renderer2 } from '@angular/core';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { catchError, filter, switchMap, tap } from 'rxjs/operators';
import {
  ArgumentType,
  CalculatedField,
  CalculatedFieldEventArguments,
  CalculatedFieldDebugDialogData,
  CalculatedFieldDialogData,
  CalculatedFieldTestScriptDialogData,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  CalculatedFieldTypeTranslations,
} from '@shared/models/calculated-field.models';
import {
  CalculatedFieldDebugDialogComponent,
  CalculatedFieldDialogComponent,
  CalculatedFieldScriptTestDialogComponent
} from './components/public-api';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { isObject } from '@core/utils';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';

export class CalculatedFieldsTableConfig extends EntityTableConfig<CalculatedField, PageLink> {

  // TODO: [Calculated Fields] remove hardcode when BE variable implemented
  readonly calculatedFieldsDebugPerTenantLimitsConfiguration =
    getCurrentAuthState(this.store)['calculatedFieldsDebugPerTenantLimitsConfiguration'] || '1:1';
  readonly maxDebugModeDuration = getCurrentAuthState(this.store).maxDebugModeDurationMinutes * MINUTE;
  readonly tenantId = getCurrentAuthUser(this.store).tenantId;
  additionalDebugActionConfig = {
    title: this.translate.instant('calculated-fields.see-debug-events'),
    action: (calculatedField: CalculatedField) => this.openDebugEventsDialog.call(this, calculatedField),
  };

  constructor(private calculatedFieldsService: CalculatedFieldsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              public entityId: EntityId = null,
              private store: Store<AppState>,
              private destroyRef: DestroyRef,
              private renderer: Renderer2,
              public entityName: string,
              private importExportService: ImportExportService,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private readonly: boolean = false,
  ) {
    super();
    this.tableTitle = this.translate.instant('entity.type-calculated-fields');
    this.detailsPanelEnabled = false;
    this.pageMode = false;
    this.entityType = EntityType.CALCULATED_FIELD;
    this.entityTranslations = entityTypeTranslations.get(EntityType.CALCULATED_FIELD);

    this.entitiesFetchFunction = (pageLink: PageLink) => this.fetchCalculatedFields(pageLink);
    this.addEntity = this.getCalculatedFieldDialog.bind(this);
    this.addEnabled = !this.readonly;
    this.entitiesDeleteEnabled = !this.readonly;
    this.deleteEntityTitle = (field: CalculatedField) => this.translate.instant('calculated-fields.delete-title', {title: field.name});
    this.deleteEntityContent = () => this.translate.instant('calculated-fields.delete-text');
    this.deleteEntitiesTitle = count => this.translate.instant('calculated-fields.delete-multiple-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('calculated-fields.delete-multiple-text');
    this.deleteEntity = id => this.calculatedFieldsService.deleteCalculatedField(id.id);
    this.addActionDescriptors = [
      {
        name: this.translate.instant('calculated-fields.create'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('calculated-fields.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: () => this.importCalculatedField()
      }
    ];

    this.defaultSortOrder = {property: 'name', direction: Direction.DESC};

    const expressionColumn = new EntityTableColumn<CalculatedField>('expression', 'calculated-fields.expression', '33%', entity => entity.configuration?.expression);
    expressionColumn.sortable = false;

    this.columns.push(new EntityTableColumn<CalculatedField>('name', 'common.name', '33%'));
    this.columns.push(new EntityTableColumn<CalculatedField>('type', 'common.type', '50px', entity => this.translate.instant(CalculatedFieldTypeTranslations.get(entity.type))));
    this.columns.push(expressionColumn);

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('action.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: (event$, entity) => this.exportCalculatedField(event$, entity),
      },
      {
        name: this.translate.instant('entity-view.events'),
        icon: 'mdi:clipboard-text-clock',
        isEnabled: () => true,
        onAction: (_, entity) => this.openDebugEventsDialog(entity),
      }
    );

    if (!this.readonly) {
      this.cellActionDescriptors.push({
        name: '',
        nameFunction: entity => this.entityDebugSettingsService.getDebugConfigLabel(entity?.debugSettings),
        icon: 'mdi:bug',
        isEnabled: () => true,
        iconFunction: ({ debugSettings }) => this.entityDebugSettingsService.isDebugActive(debugSettings?.allEnabledUntil) || debugSettings?.failuresEnabled ? 'mdi:bug' : 'mdi:bug-outline',
        onAction: ($event, entity) => this.onOpenDebugConfig($event, entity),
      });
    }

    this.cellActionDescriptors.push(      {
      name: this.translate.instant('action.edit'),
      nameFunction: () => this.translate.instant(this.readonly ? 'action.view' : 'action.edit'),
      icon: 'edit',
      iconFunction: () => this.readonly ? 'visibility' : 'edit',
      isEnabled: () => true,
      onAction: (_, entity) => this.editCalculatedField(entity),
    });
  }

  fetchCalculatedFields(pageLink: PageLink): Observable<PageData<CalculatedField>> {
    return this.calculatedFieldsService.getCalculatedFields(this.entityId, pageLink);
  }

  onOpenDebugConfig($event: Event, calculatedField: CalculatedField): void {
    const { debugSettings = {}, id } = calculatedField;
    const additionalActionConfig = {
      ...this.additionalDebugActionConfig,
      action: () => this.openDebugEventsDialog(calculatedField)
    };
    if ($event) {
      $event.stopPropagation();
    }

    const { viewContainerRef, renderer } = this.entityDebugSettingsService;
    if (!viewContainerRef || !renderer) {
      this.entityDebugSettingsService.viewContainerRef = this.getTable().viewContainerRef;
      this.entityDebugSettingsService.renderer = this.renderer;
    }

    this.entityDebugSettingsService.openDebugStrategyPanel({
      debugSettings,
      debugConfig: {
        debugLimitsConfiguration: this.calculatedFieldsDebugPerTenantLimitsConfiguration,
        maxDebugModeDuration: this.maxDebugModeDuration,
        entityLabel: this.translate.instant('debug-settings.calculated-field'),
        additionalActionConfig,
      },
      onSettingsAppliedFn: settings => this.onDebugConfigChanged(id.id, settings)
    }, $event.target as Element);
  }

  private editCalculatedField(calculatedField: CalculatedField, isDirty = false): void {
    this.getCalculatedFieldDialog(calculatedField, 'action.apply', isDirty)
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private getCalculatedFieldDialog(value?: CalculatedField, buttonTitle = 'action.add', isDirty = false): Observable<CalculatedField> {
    return this.dialog.open<CalculatedFieldDialogComponent, CalculatedFieldDialogData, CalculatedField>(CalculatedFieldDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        value,
        buttonTitle,
        entityId: this.entityId,
        debugLimitsConfiguration: this.calculatedFieldsDebugPerTenantLimitsConfiguration,
        tenantId: this.tenantId,
        entityName: this.entityName,
        additionalDebugActionConfig: this.additionalDebugActionConfig,
        getTestScriptDialogFn: this.getTestScriptDialog.bind(this),
        isDirty,
        readonly: this.readonly,
      },
      enterAnimationDuration: isDirty ? 0 : null,
    })
      .afterClosed()
      .pipe(filter(Boolean));
  }

  private openDebugEventsDialog(calculatedField: CalculatedField): void {
    this.dialog.open<CalculatedFieldDebugDialogComponent, CalculatedFieldDebugDialogData, null>(CalculatedFieldDebugDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        tenantId: this.tenantId,
        value: calculatedField,
        getTestScriptDialogFn: this.getTestScriptDialog.bind(this),
      }
    })
      .afterClosed()
      .subscribe();
  }

  private exportCalculatedField($event: Event, calculatedField: CalculatedField): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExportService.exportCalculatedField(calculatedField.id.id);
  }

  private importCalculatedField(): void {
    this.importExportService.importCalculatedField(this.entityId)
      .pipe(filter(Boolean), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.updateData());
  }

  private onDebugConfigChanged(id: string, debugSettings: EntityDebugSettings): void {
    this.calculatedFieldsService.getCalculatedFieldById(id).pipe(
      switchMap(field => this.calculatedFieldsService.saveCalculatedField({ ...field, debugSettings })),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.updateData());
  }

  private getTestScriptDialog(calculatedField: CalculatedField, argumentsObj?: CalculatedFieldEventArguments, openCalculatedFieldEdit = true): Observable<string> {
    const resultArguments = Object.keys(calculatedField.configuration.arguments).reduce((acc, key) => {
      const type = calculatedField.configuration.arguments[key].refEntityKey.type;
      acc[key] = isObject(argumentsObj) && argumentsObj.hasOwnProperty(key)
        ? { ...argumentsObj[key], type }
        : type === ArgumentType.Rolling ? { values: [], type } : { value: '', type, ts: new Date().getTime() };
      return acc;
    }, {});
    return this.dialog.open<CalculatedFieldScriptTestDialogComponent, CalculatedFieldTestScriptDialogData, string>(CalculatedFieldScriptTestDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          arguments: resultArguments,
          expression: calculatedField.configuration.expression,
          argumentsEditorCompleter: getCalculatedFieldArgumentsEditorCompleter(calculatedField.configuration.arguments),
          argumentsHighlightRules: getCalculatedFieldArgumentsHighlights(calculatedField.configuration.arguments),
          openCalculatedFieldEdit,
          readonly: this.readonly,
        }
      }).afterClosed()
      .pipe(
        filter(Boolean),
        tap(expression => {
          if (openCalculatedFieldEdit) {
            this.editCalculatedField({ entityId: this.entityId, ...calculatedField, configuration: {...calculatedField.configuration, expression } }, true)
          }
        }),
      );
  }
}
