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

import { Component, ElementRef, ViewChild } from '@angular/core';
import { WidgetActionType, WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable, of } from 'rxjs';
import { map, mergeMap, startWith } from 'rxjs/operators';

@Component({
  selector: 'tb-dashboard-state-widget-settings',
  templateUrl: './dashboard-state-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class DashboardStateWidgetSettingsComponent extends WidgetSettingsComponent {

  @ViewChild('dashboardStateInput') dashboardStateInput: ElementRef;

  dashboardStateWidgetSettingsForm: FormGroup;

  filteredDashboardStates: Observable<Array<string>>;
  dashboardStateSearchText = '';

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.dashboardStateWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      stateId: '',
      defaultAutofillLayout: true,
      defaultMargin: 0,
      defaultBackgroundColor: '#fff',
      syncParentStateParams: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.dashboardStateWidgetSettingsForm = this.fb.group({
      stateId: [settings.stateId, []],
      defaultAutofillLayout: [settings.defaultAutofillLayout, []],
      defaultMargin: [settings.defaultMargin, [Validators.min(0)]],
      defaultBackgroundColor: [settings.defaultBackgroundColor, []],
      syncParentStateParams: [settings.syncParentStateParams, []]
    });
    this.dashboardStateSearchText = '';
    this.filteredDashboardStates = this.dashboardStateWidgetSettingsForm.get('stateId').valueChanges
      .pipe(
        startWith(''),
        map(value => value ? value : ''),
        mergeMap(name => this.fetchDashboardStates(name) )
      );
  }

  public clearDashboardState(value: string = '') {
    this.dashboardStateInput.nativeElement.value = value;
    this.dashboardStateWidgetSettingsForm.get('stateId').patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.dashboardStateInput.nativeElement.blur();
      this.dashboardStateInput.nativeElement.focus();
    }, 0);
  }

  private fetchDashboardStates(searchText?: string): Observable<Array<string>> {
    this.dashboardStateSearchText = searchText;
    const stateIds = Object.keys(this.dashboard.configuration.states);
    const result = searchText ? stateIds.filter(this.createFilterForDashboardState(searchText)) : stateIds;
    if (result && result.length) {
      return of(result);
    } else {
      return of([searchText]);
    }
  }

  private createFilterForDashboardState(query: string): (stateId: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return stateId => stateId.toLowerCase().indexOf(lowercaseQuery) === 0;
  }
}
