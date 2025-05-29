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

import {
  AfterViewInit,
  booleanAttribute,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnChanges,
  Renderer2,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  ArgumentEntityType,
  ArgumentType,
  ArgumentTypeTranslations,
  CalculatedFieldArgument,
  CalculatedFieldArgumentValue,
  CalculatedFieldType,
  CFArgumentDynamicSourceType,
} from '@shared/models/calculated-field.models';
import { CalculatedFieldArgumentPanelComponent } from '@home/components/calculated-fields/components/public-api';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL, isEqual } from '@core/utils';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';
import { EntityService } from '@core/http/entity.service';
import { MatSort } from '@angular/material/sort';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { forkJoin, Observable } from 'rxjs';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { BaseData } from '@shared/models/base-data';

@Component({
  selector: 'tb-calculated-field-arguments-table',
  templateUrl: './calculated-field-arguments-table.component.html',
  styleUrls: [`calculated-field-arguments-table.component.scss`],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldArgumentsTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldArgumentsTableComponent),
      multi: true
    }
  ],
})
export class CalculatedFieldArgumentsTableComponent implements ControlValueAccessor, Validator, OnChanges, AfterViewInit {

  @Input() entityId: EntityId;
  @Input() tenantId: string;
  @Input() entityName: string;
  @Input() ownerId: EntityId;
  @Input() calculatedFieldType: CalculatedFieldType;
  @Input({ transform: booleanAttribute }) readonly: boolean;

  @ViewChild(MatSort, { static: true }) sort: MatSort;

  errorText = '';
  argumentsFormArray = this.fb.array<CalculatedFieldArgumentValue>([]);
  entityNameMap = new Map<string, string>();
  sortOrder = { direction: 'asc', property: '' };
  dataSource = new CalculatedFieldArgumentDatasource();

  readonly entityTypeTranslations = entityTypeTranslations;
  readonly ArgumentTypeTranslations = ArgumentTypeTranslations;
  readonly ArgumentEntityType = ArgumentEntityType;
  readonly CFArgumentDynamicSourceType = CFArgumentDynamicSourceType;
  readonly ArgumentType = ArgumentType;
  readonly CalculatedFieldType = CalculatedFieldType;
  readonly maxArgumentsPerCF = getCurrentAuthState(this.store).maxArgumentsPerCF;
  readonly NULL_UUID = NULL_UUID;

  private popoverComponent: TbPopoverComponent<CalculatedFieldArgumentPanelComponent>;
  private propagateChange: (argumentsObj: Record<string, CalculatedFieldArgument>) => void = () => {};

  constructor(
    private fb: FormBuilder,
    private popoverService: TbPopoverService,
    private viewContainerRef: ViewContainerRef,
    private cd: ChangeDetectorRef,
    private renderer: Renderer2,
    private entityService: EntityService,
    private destroyRef: DestroyRef,
    private store: Store<AppState>
  ) {
    this.argumentsFormArray.valueChanges.pipe(takeUntilDestroyed()).subscribe(value => {
      this.updateDataSource(value);
      this.propagateChange(this.getArgumentsObject(value));
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.calculatedFieldType?.previousValue
      && changes.calculatedFieldType.currentValue !== changes.calculatedFieldType.previousValue) {
      this.argumentsFormArray.updateValueAndValidity();
    }
  }

  ngAfterViewInit(): void {
    this.sort.sortChange.asObservable().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.sortOrder.property = this.sort.active;
      this.sortOrder.direction = this.sort.direction;
      this.updateDataSource(this.argumentsFormArray.value);
    });
  }

  registerOnChange(fn: (argumentsObj: Record<string, CalculatedFieldArgument>) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_): void {}

  validate(): ValidationErrors | null {
    this.updateErrorText();
    return this.errorText ? { argumentsFormArray: false } : null;
  }

  onDelete($event: Event, argument: CalculatedFieldArgumentValue): void {
    $event.stopPropagation();
    const index = this.argumentsFormArray.controls.findIndex(control => isEqual(control.value, argument));
    this.argumentsFormArray.removeAt(index);
    this.argumentsFormArray.markAsDirty();
  }

  manageArgument($event: Event, matButton: MatButton, argument = {} as CalculatedFieldArgumentValue): void {
    $event?.stopPropagation();
    if (this.popoverComponent && !this.popoverComponent.tbHidden) {
      this.popoverComponent.hide();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const index = this.argumentsFormArray.controls.findIndex(control => isEqual(control.value, argument));
      const isExists = index !== -1;
      const ctx = {
        index,
        argument,
        entityId: this.entityId,
        calculatedFieldType: this.calculatedFieldType,
        buttonTitle: isExists ? 'action.apply' : 'action.add',
        tenantId: this.tenantId,
        entityName: this.entityName,
        ownerId: this.ownerId,
        usedArgumentNames: this.argumentsFormArray.value.map(({ argumentName }) => argumentName).filter(name => name !== argument.argumentName),
      };
      this.popoverComponent = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: CalculatedFieldArgumentPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: isExists ? 'left' : 'right',
        context: ctx,
        isModal: true
      });
      this.popoverComponent.tbComponentRef.instance.argumentsDataApplied.subscribe(({ entityName, ...value }) => {
        this.popoverComponent.hide();
        if (entityName) {
          this.entityNameMap.set(value.refEntityId.id, entityName);
        }
        if (isExists) {
          this.argumentsFormArray.at(index).setValue(value);
        } else {
          this.argumentsFormArray.push(this.fb.control(value));
        }
        this.cd.markForCheck();
      });
    }
  }

  private updateDataSource(value: CalculatedFieldArgumentValue[]): void {
    const sortedValue = this.sortData(value);
    this.dataSource.loadData(sortedValue);
  }

  private updateErrorText(): void {
    if (this.calculatedFieldType === CalculatedFieldType.SIMPLE
      && this.argumentsFormArray.controls.some(control => control.value.refEntityKey.type === ArgumentType.Rolling)) {
      this.errorText = 'calculated-fields.hint.arguments-simple-with-rolling';
    } else if (this.argumentsFormArray.controls.some(control => control.value.refEntityId?.id === NULL_UUID)) {
      this.errorText = 'calculated-fields.hint.arguments-entity-not-found';
    } else if (!this.argumentsFormArray.controls.length) {
      this.errorText = 'calculated-fields.hint.arguments-empty';
    } else {
      this.errorText = '';
    }
  }

  private getArgumentsObject(value: CalculatedFieldArgumentValue[]): Record<string, CalculatedFieldArgument> {
    return value.reduce((acc, argumentValue) => {
      const { argumentName, ...argument } = argumentValue as CalculatedFieldArgumentValue;
      acc[argumentName] = argument;
      return acc;
    }, {} as Record<string, CalculatedFieldArgument>);
  }

  writeValue(argumentsObj: Record<string, CalculatedFieldArgument>): void {
    this.argumentsFormArray.clear();
    this.populateArgumentsFormArray(argumentsObj);
    this.updateEntityNameMap(this.argumentsFormArray.value);
  }

  getEntityDetailsPageURL(id: string, type: EntityType): string {
    return getEntityDetailsPageURL(id, type);
  }

  private populateArgumentsFormArray(argumentsObj: Record<string, CalculatedFieldArgument>): void {
    Object.keys(argumentsObj).forEach(key => {
      const value: CalculatedFieldArgumentValue = {
        ...argumentsObj[key],
        argumentName: key
      };
      this.argumentsFormArray.push(this.fb.control(value), { emitEvent: false });
    });
    this.argumentsFormArray.updateValueAndValidity();
  }

  private updateEntityNameMap(values: CalculatedFieldArgumentValue[]): void {
    const entitiesByType = values.reduce((acc, { refEntityId = {}}) => {
      if (refEntityId.id && refEntityId.entityType !== ArgumentEntityType.Tenant) {
        const { id, entityType } = refEntityId as EntityId;
        acc[entityType] = acc[entityType] ?? [];
        acc[entityType].push(id);
      }
      return acc;
    }, {} as Record<EntityType, string[]>);
    const tasks = Object.entries(entitiesByType).map(([entityType, ids]) =>
      this.entityService.getEntities(entityType as EntityType, ids)
    );
    if (!tasks.length) {
      return;
    }
    this.fetchEntityNames(tasks, values);
  }

  private fetchEntityNames(tasks: Observable<BaseData<EntityId>[]>[], values: CalculatedFieldArgumentValue[]): void {
    forkJoin(tasks as Observable<BaseData<EntityId>[]>[])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result: Array<BaseData<EntityId>>[]) => {
        result.forEach((entities: BaseData<EntityId>[]) => entities.forEach((entity: BaseData<EntityId>) => this.entityNameMap.set(entity.id.id, entity.name)));
        let updateTable = false;
        values.forEach(({ refEntityId }) => {
          if (refEntityId?.id && !this.entityNameMap.has(refEntityId.id) && refEntityId.entityType !== ArgumentEntityType.Tenant) {
            updateTable = true;
            const control = this.argumentsFormArray.controls.find(control => control.value.refEntityId?.id === refEntityId.id);
            const value = control.value;
            value.refEntityId.id = NULL_UUID;
            control.setValue(value, { emitEvent: false });
          }
        });
        if (updateTable) {
          this.argumentsFormArray.updateValueAndValidity();
        }
      });
  }

  private getSortValue(argument: CalculatedFieldArgumentValue, column: string): string {
    switch (column) {
      case 'entityType':
        if (argument.refEntityId?.entityType === ArgumentEntityType.Tenant) {
          return 'calculated-fields.argument-current-tenant';
        } else if (argument.refEntityId?.id) {
          return entityTypeTranslations.get((argument.refEntityId)?.entityType as unknown as EntityType).type;
        } else {
          return 'calculated-fields.argument-current';
        }
      case 'type':
        return ArgumentTypeTranslations.get(argument.refEntityKey.type);
      case 'key':
        return argument.refEntityKey.key;
      default:
        return argument.argumentName;
    }
  }

  private sortData(data: CalculatedFieldArgumentValue[]): CalculatedFieldArgumentValue[] {
    return data.sort((a, b) => {
      const valA = this.getSortValue(a, this.sortOrder.property) ?? '';
      const valB = this.getSortValue(b, this.sortOrder.property) ?? '';
      return (this.sortOrder.direction === 'asc' ? 1 : -1) * valA.localeCompare(valB);
    });
  }
}

class CalculatedFieldArgumentDatasource extends TbTableDatasource<CalculatedFieldArgumentValue> {
  constructor() {
    super();
  }
}
