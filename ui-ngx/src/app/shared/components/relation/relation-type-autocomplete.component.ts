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

import {AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild, OnDestroy} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import {Observable, of, throwError, Subscription} from 'rxjs';
import {PageLink} from '@shared/models/page/page-link';
import {Direction} from '@shared/models/page/sort-order';
import {filter, map, mergeMap, publishReplay, refCount, startWith, tap, publish} from 'rxjs/operators';
import {PageData, emptyPageData} from '@shared/models/page/page-data';
import {DashboardInfo} from '@app/shared/models/dashboard.models';
import {DashboardId} from '@app/shared/models/id/dashboard-id';
import {DashboardService} from '@core/http/dashboard.service';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {getCurrentAuthUser} from '@app/core/auth/auth.selectors';
import {Authority} from '@shared/models/authority.enum';
import {TranslateService} from '@ngx-translate/core';
import {DeviceService} from '@core/http/device.service';
import {EntitySubtype, EntityType} from '@app/shared/models/entity-type.models';
import {BroadcastService} from '@app/core/services/broadcast.service';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {AssetService} from '@core/http/asset.service';
import {EntityViewService} from '@core/http/entity-view.service';
import { RelationTypes } from '@app/shared/models/relation.models';

@Component({
  selector: 'tb-relation-type-autocomplete',
  templateUrl: './relation-type-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RelationTypeAutocompleteComponent),
    multi: true
  }]
})
export class RelationTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  relationTypeFormGroup: FormGroup;

  modelValue: string | null;

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

  @ViewChild('relationTypeInput', {static: true}) relationTypeInput: ElementRef;

  filteredRelationTypes: Observable<Array<string>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private broadcast: BroadcastService,
              public translate: TranslateService,
              private fb: FormBuilder) {
    this.relationTypeFormGroup = this.fb.group({
      relationType: [null, this.required ? [Validators.required] : []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {

    this.filteredRelationTypes = this.relationTypeFormGroup.get('relationType').valueChanges
      .pipe(
        tap(value => {
          this.updateView(value);
        }),
        // startWith<string | EntitySubtype>(''),
        map(value => value ? value : ''),
        mergeMap(type => this.fetchRelationTypes(type) )
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.relationTypeFormGroup.disable({emitEvent: false});
    } else {
      this.relationTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    this.relationTypeFormGroup.get('relationType').patchValue(value, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.relationTypeFormGroup.get('relationType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayRelationTypeFn(relationType?: string): string | undefined {
    return relationType ? relationType : undefined;
  }

  fetchRelationTypes(searchText?: string, strictMatch: boolean = false): Observable<Array<string>> {
    this.searchText = searchText;
    return of(RelationTypes).pipe(
      map(relationTypes => relationTypes.filter( relationType => {
        if (strictMatch) {
          return searchText ? relationType === searchText : false;
        } else {
          return searchText ? relationType.toUpperCase().startsWith(searchText.toUpperCase()) : true;
        }
      }))
    );
  }

  clear() {
    this.relationTypeFormGroup.get('relationType').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.relationTypeInput.nativeElement.blur();
      this.relationTypeInput.nativeElement.focus();
    }, 0);
  }

}
