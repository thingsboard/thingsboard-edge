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
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { Datasource } from '@shared/models/widget.models';
import { EntityService } from '@core/http/entity.service';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-datasources-key-autocomplete',
  templateUrl: './datasources-key-autocomplete.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatasourcesKeyAutocompleteComponent),
      multi: true
    }
  ]
})
export class DatasourcesKeyAutocompleteComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @ViewChild('keyInput') keyInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  datasources: Array<Datasource>;

  @Input()
  label = 'entity.key-name';

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private modelValue: string;

  private propagateChange = null;

  public keyFormGroup: FormGroup;

  filteredKeys: Observable<Array<string>>;
  keySearchText = '';

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private entityService: EntityService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.keyFormGroup = this.fb.group({
      key: [null, this.required ? [Validators.required] : []]
    });
    this.keyFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });

    this.filteredKeys = this.keyFormGroup.get('key').valueChanges
      .pipe(
        map(value => value ? value : ''),
        mergeMap(name => this.fetchKeys(name) )
      );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.keyFormGroup.disable({emitEvent: false});
    } else {
      this.keyFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string): void {
    this.modelValue = value;
    this.keyFormGroup.patchValue(
      {key: value}, {emitEvent: false}
    );
  }

  clearKey() {
    this.keyFormGroup.get('key').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.keyInput.nativeElement.blur();
      this.keyInput.nativeElement.focus();
    }, 0);
  }

  onFocus() {
    this.keyFormGroup.get('key').updateValueAndValidity({onlySelf: true, emitEvent: true});
  }

  private updateModel() {
    this.modelValue = this.keyFormGroup.get('key').value;
    this.propagateChange(this.modelValue);
  }

  private fetchKeys(searchText?: string): Observable<Array<string>> {
      this.keySearchText = searchText;
      const dataKeyFilter = this.createKeyFilter(this.keySearchText);
      return of(this.allKeys()).pipe(
        map(name => name.filter(dataKeyFilter))
      );
  }

  private createKeyFilter(query: string): (key: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.toLowerCase().startsWith(lowercaseQuery);
  }

  private allKeys(): string[] {
    const allDataKeys = (this.datasources || []).map(ds => ds.dataKeys)
      .reduce((accumulator, value) => accumulator.concat(value), []).map(dataKey => dataKey.label);
    return [...new Set(allDataKeys)].sort((a, b) => a.localeCompare(b));
  }
}
