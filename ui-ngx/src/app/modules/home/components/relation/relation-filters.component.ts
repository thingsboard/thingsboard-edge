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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALUE_ACCESSOR
} from '@angular/forms';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { RelationEntityTypeFilter } from '@shared/models/relation.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-relation-filters',
  templateUrl: './relation-filters.component.html',
  styleUrls: ['./relation-filters.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RelationFiltersComponent),
      multi: true
    }
  ]
})
export class RelationFiltersComponent extends PageComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input() disabled: boolean;

  @Input() allowedEntityTypes: Array<EntityType | AliasEntityType>;

  relationFiltersFormGroup: UntypedFormGroup;

  private destroy$ = new Subject<void>();
  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.relationFiltersFormGroup = this.fb.group({
      relationFilters: this.fb.array([])
    });

    this.relationFiltersFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get relationFiltersFormArray(): UntypedFormArray {
      return this.relationFiltersFormGroup.get('relationFilters') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(filters: Array<RelationEntityTypeFilter>): void {
    if (filters?.length === this.relationFiltersFormArray.length) {
      this.relationFiltersFormArray.patchValue(filters, {emitEvent: false});
    } else {
      const relationFiltersControls: Array<AbstractControl> = [];
      if (filters && filters.length) {
        filters.forEach((filter) => {
          relationFiltersControls.push(this.createRelationFilterFormGroup(filter));
        });
      }
      this.relationFiltersFormGroup.setControl('relationFilters', this.fb.array(relationFiltersControls), {emitEvent: false});
    }
  }

  public removeFilter(index: number) {
    (this.relationFiltersFormGroup.get('relationFilters') as UntypedFormArray).removeAt(index);
  }

  public addFilter() {
    const filter: RelationEntityTypeFilter = {
      relationType: null,
      entityTypes: []
    };
    this.relationFiltersFormArray.push(this.createRelationFilterFormGroup(filter));
  }

  private createRelationFilterFormGroup(filter: RelationEntityTypeFilter): AbstractControl {
    return this.fb.group({
      relationType: [filter ? filter.relationType : null],
      entityTypes: [filter ? filter.entityTypes : []]
    });
  }

  private updateModel() {
    const filters: Array<RelationEntityTypeFilter> = this.relationFiltersFormGroup.get('relationFilters').value;
    this.propagateChange(filters);
  }
}
