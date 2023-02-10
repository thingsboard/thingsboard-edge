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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import {
  EntityTypeVersionCreateConfig,
  exportableEntityTypes, overrideEntityTypeTranslations,
  SyncStrategy,
  syncStrategyTranslationMap
} from '@shared/models/vc.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { isDefinedAndNotNull } from '@core/utils';
import { entityGroupTypes } from '@shared/models/entity-group.models';

@Component({
  selector: 'tb-entity-types-version-create',
  templateUrl: './entity-types-version-create.component.html',
  styleUrls: ['./entity-types-version.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityTypesVersionCreateComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => EntityTypesVersionCreateComponent),
      multi: true
    }
  ]
})
export class EntityTypesVersionCreateComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: {[entityType: string]: EntityTypeVersionCreateConfig};

  private propagateChange = null;

  public entityTypesVersionCreateFormGroup: UntypedFormGroup;

  syncStrategies = Object.values(SyncStrategy);

  syncStrategyTranslations = syncStrategyTranslationMap;

  entityTypes = EntityType;

  loading = true;

  overrideEntityTypeTranslationsMap = overrideEntityTypeTranslations;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.entityTypesVersionCreateFormGroup = this.fb.group({
      entityTypes: this.fb.array([], [])
    });
    this.entityTypesVersionCreateFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.entityTypesVersionCreateFormGroup.disable({emitEvent: false});
    } else {
      this.entityTypesVersionCreateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: {[entityType: string]: EntityTypeVersionCreateConfig} | undefined): void {
    this.modelValue = value;
    this.entityTypesVersionCreateFormGroup.setControl('entityTypes',
      this.prepareEntityTypesFormArray(value), {emitEvent: false});
  }

  public validate(c: UntypedFormControl) {
    return this.entityTypesVersionCreateFormGroup.valid && this.entityTypesFormGroupArray().length ? null : {
      entityTypes: {
        valid: false,
      },
    };
  }

  private prepareEntityTypesFormArray(entityTypes: {[entityType: string]: EntityTypeVersionCreateConfig} | undefined): UntypedFormArray {
    const entityTypesControls: Array<AbstractControl> = [];
    if (entityTypes) {
      for (const entityType of Object.keys(entityTypes)) {
        const config = entityTypes[entityType];
        entityTypesControls.push(this.createEntityTypeControl(entityType as EntityType, config));
      }
    }
    return this.fb.array(entityTypesControls);
  }

  private createEntityTypeControl(entityType: EntityType, config: EntityTypeVersionCreateConfig): AbstractControl {
    const entityTypeControl = this.fb.group(
      {
        entityType: [entityType, [Validators.required]],
        config: this.fb.group({
          syncStrategy: [config.syncStrategy === null ? 'default' : config.syncStrategy, []],
          saveRelations: [config.saveRelations, []],
          saveAttributes: [config.saveAttributes, []],
          saveCredentials: [config.saveCredentials, []],
          saveGroupEntities: [config.saveGroupEntities, []],
          savePermissions: [config.savePermissions, []],
          allEntities: [config.allEntities, []],
          entityIds: [config.entityIds, [Validators.required]]
        })
      }
    );
    this.updateEntityTypeValidators(entityTypeControl);
    entityTypeControl.get('config').get('allEntities').valueChanges.subscribe(() => {
      this.updateEntityTypeValidators(entityTypeControl);
    });
    entityTypeControl.get('entityType').valueChanges.subscribe(() => {
      entityTypeControl.get('config').get('entityIds').patchValue([], {emitEvent: false});
    });
    return entityTypeControl;
  }

  private updateEntityTypeValidators(entityTypeControl: AbstractControl): void {
    const allEntities: boolean = entityTypeControl.get('config').get('allEntities').value;
    if (allEntities) {
      entityTypeControl.get('config').get('entityIds').disable({emitEvent: false});
    } else {
      entityTypeControl.get('config').get('entityIds').enable({emitEvent: false});
    }
    entityTypeControl.get('config').get('entityIds').updateValueAndValidity({emitEvent: false});
  }

  entityTypesFormGroupArray(): UntypedFormGroup[] {
    return (this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray).controls as UntypedFormGroup[];
  }

  entityTypesFormGroupExpanded(entityTypeControl: AbstractControl): boolean {
    return !!(entityTypeControl as any).expanded;
  }

  public trackByEntityType(index: number, entityTypeControl: AbstractControl): any {
    return entityTypeControl;
  }

  public removeEntityType(index: number) {
    (this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray).removeAt(index);
  }

  public addEnabled(): boolean {
    const entityTypesArray = this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray;
    return entityTypesArray.length < exportableEntityTypes.length;
  }

  public addEntityType() {
    const entityTypesArray = this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray;
    const config: EntityTypeVersionCreateConfig = {
      syncStrategy: null,
      saveAttributes: true,
      saveRelations: true,
      saveCredentials: true,
      saveGroupEntities: true,
      savePermissions: true,
      allEntities: true,
      entityIds: []
    };
    const allowed = this.allowedEntityTypes();
    let entityType: EntityType = null;
    if (allowed.length) {
      entityType = allowed[0];
    }
    const entityTypeControl = this.createEntityTypeControl(entityType, config);
    (entityTypeControl as any).expanded = true;
    entityTypesArray.push(entityTypeControl);
    this.entityTypesVersionCreateFormGroup.updateValueAndValidity();
  }

  public removeAll() {
    const entityTypesArray = this.entityTypesVersionCreateFormGroup.get('entityTypes') as UntypedFormArray;
    entityTypesArray.clear();
    this.entityTypesVersionCreateFormGroup.updateValueAndValidity();
  }

  entityTypeText(entityTypeControl: AbstractControl): string {
    const entityType: EntityType = entityTypeControl.get('entityType').value;
    const config: EntityTypeVersionCreateConfig = entityTypeControl.get('config').value;
    let count = config?.entityIds?.length;
    if (!isDefinedAndNotNull(count)) {
      count = 0;
    }
    if (entityType) {
      const translation = entityTypeTranslations.get(entityType);
      return this.translate.instant((config?.allEntities ?
        (entityType === EntityType.USER ? 'entity-group.user-groups' : translation.typePlural)
        : (entityGroupTypes.includes(entityType) ? translation.groupList : translation.list)), {count});
    } else {
      return 'Undefined';
    }
  }

  allowedEntityTypes(entityTypeControl?: AbstractControl): Array<EntityType> {
    let res = [...exportableEntityTypes];
    const currentEntityType: EntityType = entityTypeControl?.get('entityType')?.value;
    const value: [{entityType: string, config: EntityTypeVersionCreateConfig}] =
      this.entityTypesVersionCreateFormGroup.get('entityTypes').value || [];
    const usedEntityTypes = value.map(val => val.entityType).filter(val => val);
    res = res.filter(entityType => !usedEntityTypes.includes(entityType) || entityType === currentEntityType);
    return res;
  }

  isGroupEntityType(entityType: EntityType): boolean {
    return entityGroupTypes.includes(entityType);
  }

  private updateModel() {
    const value: [{entityType: string, config: EntityTypeVersionCreateConfig}] =
      this.entityTypesVersionCreateFormGroup.get('entityTypes').value || [];
    let modelValue: {[entityType: string]: EntityTypeVersionCreateConfig} = null;
    if (value && value.length) {
      modelValue = {};
      value.forEach((val) => {
        modelValue[val.entityType] = val.config;
        if ((modelValue[val.entityType].syncStrategy as any) === 'default') {
          modelValue[val.entityType].syncStrategy = null;
        }
      });
    }
    this.modelValue = modelValue;
    this.propagateChange(this.modelValue);
  }
}
