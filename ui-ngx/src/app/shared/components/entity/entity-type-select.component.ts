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

import { AfterViewInit, Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { AliasEntityType, EntityType, entityTypeTranslations } from '@app/shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Operation } from '@shared/models/security.models';

@Component({
  selector: 'tb-entity-type-select',
  templateUrl: './entity-type-select.component.html',
  styleUrls: ['./entity-type-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityTypeSelectComponent),
    multi: true
  }]
})
export class EntityTypeSelectComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnChanges {

  entityTypeFormGroup: UntypedFormGroup;

  modelValue: EntityType | AliasEntityType | null;

  @Input()
  allowedEntityTypes: Array<EntityType | AliasEntityType>;

  @Input()
  useAliasEntityTypes: boolean;

  @Input()
  operation: Operation;

  @Input()
  filterAllowedEntityTypes = true;

  @Input()
  overrideEntityTypeTranslations: Map<EntityType | AliasEntityType, string>;

  private showLabelValue: boolean;
  get showLabel(): boolean {
    return this.showLabelValue;
  }
  @Input()
  set showLabel(value: boolean) {
    this.showLabelValue = coerceBooleanProperty(value);
  }

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  entityTypes: Array<EntityType | AliasEntityType>;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              public translate: TranslateService,
              private fb: UntypedFormBuilder) {
    this.entityTypeFormGroup = this.fb.group({
      entityType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.entityTypes = this.filterAllowedEntityTypes ?
      this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes, this.useAliasEntityTypes, this.operation) : this.allowedEntityTypes;
    this.entityTypeFormGroup.get('entityType').valueChanges.subscribe(
      (value) => {
        let modelValue;
        if (!value || value === '') {
          modelValue = null;
        } else {
          modelValue = value;
        }
        this.updateView(modelValue);
      }
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'allowedEntityTypes') {
          this.entityTypes = this.filterAllowedEntityTypes ?
            this.entityService.prepareAllowedEntityTypesList(this.allowedEntityTypes, this.useAliasEntityTypes) : this.allowedEntityTypes;
          const currentEntityType: EntityType | AliasEntityType = this.entityTypeFormGroup.get('entityType').value;
          if (currentEntityType && !this.entityTypes.includes(currentEntityType)) {
            this.entityTypeFormGroup.get('entityType').patchValue(null, {emitEvent: true});
          }
        }
      }
    }
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.entityTypeFormGroup.disable();
    } else {
      this.entityTypeFormGroup.enable();
    }
  }

  writeValue(value: EntityType | AliasEntityType | null): void {
    if (value != null) {
      this.modelValue = value;
      this.entityTypeFormGroup.get('entityType').patchValue(value, {emitEvent: true});
    } else {
      this.modelValue = null;
      this.entityTypeFormGroup.get('entityType').patchValue(null, {emitEvent: true});
    }
  }

  updateView(value: EntityType | AliasEntityType | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityTypeFn(entityType?: EntityType | AliasEntityType | null): string | undefined {
    if (entityType) {
      if (this.overrideEntityTypeTranslations && this.overrideEntityTypeTranslations.has(entityType)) {
        return this.translate.instant(this.overrideEntityTypeTranslations.get(entityType));
      } else {
        return this.translate.instant(entityTypeTranslations.get(entityType as EntityType).type);
      }
    } else {
      return '';
    }
  }
}
