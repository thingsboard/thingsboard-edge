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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityInfo } from '@shared/models/entity.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { EntityService } from '@core/http/entity.service';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-aliases-entity-autocomplete',
  templateUrl: './aliases-entity-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AliasesEntityAutocompleteComponent),
    multi: true
  }]
})
export class AliasesEntityAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  selectEntityInfoFormGroup: FormGroup;

  modelValue: EntityInfo | null;

  @Input()
  alias: string;

  @Input()
  entityFilter: EntityFilter;

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

  @ViewChild('entityInfoInput', {static: true}) entityInfoInput: ElementRef;

  filteredEntityInfos: Observable<Array<EntityInfo>>;

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private entityService: EntityService,
              private fb: FormBuilder) {
    this.selectEntityInfoFormGroup = this.fb.group({
      entityInfo: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredEntityInfos = this.selectEntityInfoFormGroup.get('entityInfo').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchEntityInfos(name)),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: EntityInfo | null): void {
    this.searchText = '';
    if (isDefinedAndNotNull(value)) {
      this.modelValue = value;
      this.selectEntityInfoFormGroup.get('entityInfo').patchValue(value, {emitEvent: true});
    } else {
      this.modelValue = null;
      this.selectEntityInfoFormGroup.get('entityInfo').patchValue(null, {emitEvent: false});
    }
  }

  updateView(value: EntityInfo | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityInfoFn(entityInfo?: EntityInfo): string | undefined {
    return entityInfo ? entityInfo.name : undefined;
  }

  fetchEntityInfos(searchText?: string): Observable<Array<EntityInfo>> {
    this.searchText = searchText;
    return this.getEntityInfos(this.searchText).pipe(
      map(pageData => {
        return pageData.data;
      })
    );
  }

  getEntityInfos(searchText: string): Observable<PageData<EntityInfo>> {
    return this.entityService.findEntityInfosByFilterAndName(this.entityFilter, searchText, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<EntityInfo>()))
    );
  }

  clear() {
    this.selectEntityInfoFormGroup.get('entityInfo').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.entityInfoInput.nativeElement.blur();
      this.entityInfoInput.nativeElement.focus();
    }, 0);
  }

}
