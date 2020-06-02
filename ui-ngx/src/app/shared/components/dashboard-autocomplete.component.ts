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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { map, mergeMap, startWith, tap } from 'rxjs/operators';
import { PageData } from '@shared/models/page/page-data';
import { DashboardInfo } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { Operation } from '@shared/models/security.models';

@Component({
  selector: 'tb-dashboard-autocomplete',
  templateUrl: './dashboard-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DashboardAutocompleteComponent),
    multi: true
  }]
})
export class DashboardAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  selectDashboardFormGroup: FormGroup;

  modelValue: DashboardInfo | string | null;

  @Input()
  useIdValue = true;

  @Input()
  selectFirstDashboard = false;

  @Input()
  placeholder: string;

  @Input()
  userId: string;

  @Input()
  operation: Operation;

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

  @ViewChild('dashboardInput', {static: true}) dashboardInput: ElementRef;

  filteredDashboards: Observable<Array<DashboardInfo>>;

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private dashboardService: DashboardService,
              private fb: FormBuilder) {
    this.selectDashboardFormGroup = this.fb.group({
      dashboard: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredDashboards = this.selectDashboardFormGroup.get('dashboard').valueChanges
      .pipe(
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = this.useIdValue ? value.id.id : value;
          }
          this.updateView(modelValue);
        }),
        startWith<string | DashboardInfo>(''),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        mergeMap(name => this.fetchDashboards(name) )
      );
  }

  ngAfterViewInit(): void {
    // this.selectFirstDashboardIfNeeded();
  }

  selectFirstDashboardIfNeeded(): void {
    if (this.selectFirstDashboard && !this.modelValue) {
      this.getDashboards(new PageLink(1)).subscribe(
        (data) => {
          if (data.data.length) {
            const dashboard = data.data[0];
            this.modelValue = this.useIdValue ? dashboard.id.id : dashboard;
            this.selectDashboardFormGroup.get('dashboard').patchValue(dashboard, {emitEvent: false});
            this.propagateChange(this.modelValue);
          }
        }
      );
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: DashboardInfo | string | null): void {
    this.searchText = '';
    if (value != null) {
      if (typeof value === 'string') {
        this.dashboardService.getDashboardInfo(value).subscribe(
          (dashboard) => {
            this.modelValue = this.useIdValue ? dashboard.id.id : dashboard;
            this.selectDashboardFormGroup.get('dashboard').patchValue(dashboard, {emitEvent: true});
          }
        );
      } else {
        this.modelValue = this.useIdValue ? value.id.id : value;
        this.selectDashboardFormGroup.get('dashboard').patchValue(value, {emitEvent: true});
      }
    } else {
      this.modelValue = null;
      this.selectDashboardFormGroup.get('dashboard').patchValue(null, {emitEvent: true});
      this.selectFirstDashboardIfNeeded();
    }
  }

  updateView(value: DashboardInfo | string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayDashboardFn(dashboard?: DashboardInfo): string | undefined {
    return dashboard ? dashboard.title : undefined;
  }

  fetchDashboards(searchText?: string): Observable<Array<DashboardInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'title',
      direction: Direction.ASC
    });
    return this.getDashboards(pageLink).pipe(
      map(pageData => {
        return pageData.data;
      })
    );
  }

  getDashboards(pageLink: PageLink): Observable<PageData<DashboardInfo>> {
    return this.dashboardService.getUserDashboards(this.userId, this.operation, pageLink, {ignoreLoading: true});
  }

  clear() {
    this.selectDashboardFormGroup.get('dashboard').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.dashboardInput.nativeElement.blur();
      this.dashboardInput.nativeElement.focus();
    }, 0);
  }

}
