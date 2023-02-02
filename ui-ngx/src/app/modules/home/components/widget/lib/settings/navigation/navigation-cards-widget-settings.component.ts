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
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatChipInputEvent, MatChipList } from '@angular/material/chips';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { Observable, of, Subject } from 'rxjs';
import { map, mergeMap, share, startWith } from 'rxjs/operators';

@Component({
  selector: 'tb-navigation-cards-widget-settings',
  templateUrl: './navigation-cards-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class NavigationCardsWidgetSettingsComponent extends WidgetSettingsComponent {

  @ViewChild('filterItemsChipList') filterItemsChipList: MatChipList;
  @ViewChild('filterItemAutocomplete') filterItemAutocomplete: MatAutocomplete;
  @ViewChild('filterItemInput') filterItemInput: ElementRef<HTMLInputElement>;

  filterItems: Array<string> = ['/devices', '/assetGroups', '/profiles/deviceProfiles'];

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  navigationCardsWidgetSettingsForm: FormGroup;

  filteredFilterItems: Observable<Array<string>>;

  filterItemSearchText = '';

  filterItemInputChange = new Subject<string>();

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
    this.filteredFilterItems = this.filterItemInputChange
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchFilterItems(name) ),
        share()
      );
  }

  protected settingsForm(): FormGroup {
    return this.navigationCardsWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      filterType: 'all',
      filter: []
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.navigationCardsWidgetSettingsForm = this.fb.group({
      filterType: [settings.filterType, []],
      filter: [settings.filter, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['filterType'];
  }

  protected updateValidators(emitEvent: boolean) {
    const filterType: string = this.navigationCardsWidgetSettingsForm.get('filterType').value;
    if (filterType === 'all') {
      this.navigationCardsWidgetSettingsForm.get('filter').disable();
    } else {
      this.navigationCardsWidgetSettingsForm.get('filter').enable();
    }
    this.navigationCardsWidgetSettingsForm.get('filter').updateValueAndValidity({emitEvent});
  }

  private fetchFilterItems(searchText?: string): Observable<Array<string>> {
    this.filterItemSearchText = searchText;
    let result = [...this.filterItems];
    if (this.filterItemSearchText && this.filterItemSearchText.length) {
      result.unshift(this.filterItemSearchText);
      result = result.filter(item => item.includes(this.filterItemSearchText));
    }
    return of(result);
  }

  private addFilterItem(filterItem: string): boolean {
    if (filterItem) {
      const filterItems: string[] = this.navigationCardsWidgetSettingsForm.get('filter').value;
      const index = filterItems.indexOf(filterItem);
      if (index === -1) {
        filterItems.push(filterItem);
        this.navigationCardsWidgetSettingsForm.get('filter').setValue(filterItems);
        this.navigationCardsWidgetSettingsForm.get('filter').markAsDirty();
        return true;
      }
    }
    return false;
  }

  onFilterItemRemoved(filterItem: string): void {
    const filterItems: string[] = this.navigationCardsWidgetSettingsForm.get('filter').value;
    const index = filterItems.indexOf(filterItem);
    if (index > -1) {
      filterItems.splice(index, 1);
      this.navigationCardsWidgetSettingsForm.get('filter').setValue(filterItems);
      this.navigationCardsWidgetSettingsForm.get('filter').markAsDirty();
    }
  }

  onFilterItemInputFocus() {
    this.filterItemInputChange.next(this.filterItemInput.nativeElement.value);
  }

  addFilterItemFromChipInput(event: MatChipInputEvent): void {
    const value = event.value;
    if ((value || '').trim()) {
      const filterItem = value.trim();
      if (this.addFilterItem(filterItem)) {
        this.clearFilterItemInput('');
      }
    }
  }

  filterItemSelected(event: MatAutocompleteSelectedEvent): void {
    this.addFilterItem(event.option.value);
    this.clearFilterItemInput('');
  }

  clearFilterItemInput(value: string = '') {
    this.filterItemInput.nativeElement.value = value;
    this.filterItemInputChange.next(null);
    setTimeout(() => {
      this.filterItemInput.nativeElement.blur();
      this.filterItemInput.nativeElement.focus();
    }, 0);
  }
}
