///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import { Country, CountryData } from '@shared/models/country.models';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator
} from '@angular/forms';
import { isNotEmptyStr } from '@core/utils';
import { Observable, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { SubscriptSizing, MatFormFieldAppearance } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { TranslateService } from '@ngx-translate/core';

interface CountrySearchData extends Country {
  searchText?: string;
}

@Component({
  selector: 'tb-country-autocomplete',
  templateUrl: 'country-autocomplete.component.html',
  providers: [
    CountryData,
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CountryAutocompleteComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CountryAutocompleteComponent),
      multi: true
    }
  ]
})
export class CountryAutocompleteComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  labelText = this.translate.instant('contact.country');

  @Input()
  requiredText = this.translate.instant('contact.country-required');

  @Input()
  autocompleteHint: string;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @ViewChild('countryInput', {static: true}) countryInput: ElementRef;

  @Output()
  selectCountryCode = new EventEmitter<string>();

  countryFormGroup: FormGroup;

  searchText = '';

  filteredCountries: Observable<Array<Country>>;

  onTouched = () => {
  };
  private propagateChange: (value: any) => void = () => {
  };

  private modelValue: Country;

  private allCountries: CountrySearchData[] = this.countryData.allCountries;
  private initSearchData = false;
  private dirty = false;

  constructor(private fb: FormBuilder,
              private countryData: CountryData,
              private translate: TranslateService) {
    this.countryFormGroup = this.fb.group({
      country: ['']
    });
  }

  ngOnInit(): void {
    this.filteredCountries = this.countryFormGroup.get('country').valueChanges.pipe(
      debounceTime(150),
      tap(value => {
        let modelValue: Country;
        if (typeof value === 'string' || !value) {
          modelValue = null;
        } else {
          modelValue = value;
        }
        this.updateView(modelValue);
        if (value === null) {
          this.clear();
        }
      }),
      map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
      distinctUntilChanged(),
      switchMap(name => of(this.fetchCountries(name))),
      share()
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.countryFormGroup.disable({emitEvent: false});
    } else {
      this.countryFormGroup.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    return this.countryFormGroup.valid ? null : {
      countryFormGroup: false
    };
  }

  writeValue(country: string) {
    this.dirty = true;

    const findCountry = isNotEmptyStr(country) ? this.allCountries.find(item => item.name === country) : null;

    this.modelValue = findCountry || null;
    this.countryFormGroup.get('country').patchValue(this.modelValue || '', { emitEvent: false });
    this.selectCountryCode.emit(this.modelValue ? this.modelValue.iso2 : null);
  }

  displayCountryFn(country?: Country): string | undefined {
    return country ? `${country.flag} ${country.name}` : undefined;
  }

  onFocus() {
    if (this.dirty) {
      this.countryFormGroup.get('country').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  clear() {
    this.countryFormGroup.get('country').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.countryInput.nativeElement.blur();
      this.countryInput.nativeElement.focus();
    }, 0);
  }

  private fetchCountries(searchText: string): Country[] {
    this.searchText = searchText;
    if (!this.initSearchData) {
      this.allCountries.forEach(country => {
        country.searchText = `${country.name} ${country.iso2}`.toLowerCase();
      });
      this.initSearchData = true;
    }
    if (isNotEmptyStr(searchText)) {
      const filterValue = searchText.toLowerCase();
      return this.allCountries.filter(country => country.searchText.includes(filterValue));
    }
    return this.allCountries;
  }

  private updateView(value: Country | null) {
    if (this.modelValue?.name !== value?.name) {
      this.modelValue = value;
      this.propagateChange(this.modelValue?.name);
      if (value) {
        this.selectCountryCode.emit(value.iso2);
      }
    }
  }
}
