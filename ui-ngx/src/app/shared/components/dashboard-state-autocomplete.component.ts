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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, ReplaySubject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Dashboard, DashboardInfo } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { FloatLabelType } from '@angular/material/form-field';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';

@Component({
  selector: 'tb-dashboard-state-autocomplete',
  templateUrl: './dashboard-state-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DashboardStateAutocompleteComponent),
    multi: true
  }]
})
export class DashboardStateAutocompleteComponent implements ControlValueAccessor, OnInit {

  private dirty = false;
  private modelValue: string;

  private latestDashboardStates: Array<string> = null;
  private dashboardStatesFetchObservable$: Observable<Array<string>> = null;

  private propagateChange = (v: any) => { };


  @Input()
  placeholder: string;

  @Input()
  floatLabel: FloatLabelType = 'auto';

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

  private dashboardIdValue: string;
  get dashboardId(): string {
    return this.dashboardIdValue;
  }

  set dashboardId(value: string) {
    this.dashboardIdValue = value;
    this.clearDashboardStateCache();
    this.searchText = '';
    this.selectDashboardStateFormGroup.get('dashboardStateId').patchValue('', {emitEvent: false});
    this.dirty = true;
  }

  @ViewChild('dashboardStateInput', {static: true}) dashboardStateInput: ElementRef;

  filteredStatesDashboard$: Observable<Array<string>>;

  searchText = '';

  selectDashboardStateFormGroup = this.fb.group({
    dashboardStateId: [null]
  });

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    if (this.required) {
      this.selectDashboardStateFormGroup.get('dashboardStateId').addValidators(Validators.required);
      this.selectDashboardStateFormGroup.get('dashboardStateId').updateValueAndValidity({emitEvent: false});
    }
    this.filteredStatesDashboard$ = this.selectDashboardStateFormGroup.get('dashboardStateId').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (!value || !this.latestDashboardStates.includes(value)) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        distinctUntilChanged(),
        switchMap(name => this.fetchDashboardStates(name) ),
        share()
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectDashboardStateFormGroup.disable({emitEvent: false});
    } else {
      this.selectDashboardStateFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    if (value != null) {
      this.modelValue = value;
      this.selectDashboardStateFormGroup.get('dashboardStateId').patchValue(value, {emitEvent: false});
    } else {
      this.modelValue = null;
      this.selectDashboardStateFormGroup.get('dashboardStateId').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  private updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayDashboardFn(dashboard?: DashboardInfo): string | undefined {
    return dashboard ? dashboard.title : undefined;
  }

  onFocus() {
    if (this.dirty) {
      this.selectDashboardStateFormGroup.get('dashboard').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear(value = '') {
    this.dashboardStateInput.nativeElement.value = value;
    this.selectDashboardStateFormGroup.get('dashboardStateId').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.dashboardStateInput.nativeElement.blur();
      this.dashboardStateInput.nativeElement.focus();
    }, 0);
  }

  private fetchDashboardStates(searchText?: string): Observable<Array<string>> {
    if (this.searchText !== searchText || this.latestDashboardStates === null) {
      this.searchText = searchText;
      const slackConversationFilter = this.createFilterForDashboardState(this.searchText);
      return this.getDashboardStatesById().pipe(
        map(name => name.filter(slackConversationFilter)),
        tap(res => this.latestDashboardStates = res)
      );
    }
    return of(this.latestDashboardStates);
  }

  private getDashboardStatesById() {
    if (this.dashboardStatesFetchObservable$ === null) {
      let fetchObservable: Observable<Array<string>>;
      if (this.dashboardId) {
        fetchObservable = this.dashboardService.getDashboard(this.dashboardId, {ignoreLoading: true}).pipe(
          map((dashboard: Dashboard) => {
            if (dashboard) {
              dashboard = this.dashboardUtils.validateAndUpdateDashboard(dashboard);
              const states = dashboard.configuration.states;
              return Object.keys(states);
            } else {
              return [];
            }
          })
        );
      } else {
        fetchObservable = of([]);
      }
      this.dashboardStatesFetchObservable$ = fetchObservable.pipe(
        share({
          connector: () => new ReplaySubject(1),
          resetOnError: false,
          resetOnComplete: false,
          resetOnRefCountZero: false
        })
      );
    }
    return this.dashboardStatesFetchObservable$;
  }

  private createFilterForDashboardState(query: string): (stateId: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return stateId => stateId.toLowerCase().indexOf(lowercaseQuery) === 0;
  }

  private clearDashboardStateCache(): void {
    this.latestDashboardStates = null;
    this.dashboardStatesFetchObservable$ = null;
  }

}
