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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, shareReplay } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { isNotEmptyStr, isObject } from '@core/utils';
import { coerceArray } from '@shared/decorators/coercion';
import { MatAutocomplete } from '@angular/material/autocomplete';

type AvailableLocale = [locelCode: string, localeLanguage: string];
type AvailableLocales = Array<AvailableLocale>;

@Component({
  selector: 'tb-language-autocomplete',
  templateUrl: './language-autocomplete.component.html',
  styleUrls: ['./language-autocomplete.component.scss'],
  encapsulation:  ViewEncapsulation.None,
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => LanguageAutocompleteComponent),
    multi: true
  }]
})
export class LanguageAutocompleteComponent implements ControlValueAccessor, OnInit{

  @Input()
  disabled: boolean;

  @Input()
  @coerceArray()
  excludeLangs: string[];

  language = this.fb.group({
    language: this.fb.control<string|AvailableLocale>('', {nonNullable: true, validators: Validators.required})
  });

  @ViewChild('languageInput', {static: true}) languageInput: ElementRef;

  @ViewChild('languageAutocomplete', {static: true}) languageAutocomplete: MatAutocomplete;

  filteredTranslation: Observable<AvailableLocales>;
  searchText = '';

  private modelValue: string | null;

  private dirty = false;
  private allLocales: Observable<AvailableLocales>;

  private propagateChange = (_v: any) => { };

  constructor(private fb: FormBuilder,
              private customTranslationService: CustomTranslationService) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredTranslation = this.language.get('language').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue: string;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value[0];
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value[0]) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchLanguage(name)),
        tap(() => this.languageAutocomplete.panel?.nativeElement.scroll(0, 0)),
        share()
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.language.disable({emitEvent: false});
    } else {
      this.language.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    if (value != null) {
      this.modelValue = value;
    } else {
      this.modelValue = null;
      this.language.get('language').patchValue('', {emitEvent: false});
    }
    this.dirty = false;
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  onFocus() {
    if (!this.dirty) {
      this.language.get('language').updateValueAndValidity({onlySelf: true});
      this.dirty = true;
    }
  }

  clear(){
    this.language.get('language').reset('');
    setTimeout(() => {
      this.languageInput.nativeElement.blur();
      this.languageInput.nativeElement.focus();
    }, 0);
  }

  displayTranslateFn(translate?: AvailableLocale): string | undefined {
    return translate ? translate[1] : undefined;
  }

  getLocaleCode(translate?: AvailableLocale | string): string | undefined {
    return isObject(translate) ? translate[0] : undefined;
  }

  private fetchLanguage(searchText?: string): Observable<AvailableLocales> {
    this.searchText = searchText;
    return this.getAllTranslations().pipe(
      map((data) => {
        if (isNotEmptyStr(searchText)) {
          const normalizeSearchText = searchText.toLowerCase();
          return data
            .filter(translate => JSON.stringify(translate).toLowerCase().includes(normalizeSearchText))
            .sort((a, b) => {
              const firstLocaleCodeInclude = a[0].toLowerCase().startsWith(normalizeSearchText);
              const secondLocaleCodeInclude = b[0].toLowerCase().startsWith(normalizeSearchText);
              if (firstLocaleCodeInclude && !secondLocaleCodeInclude) {
                return -1;
              } else if (!firstLocaleCodeInclude && secondLocaleCodeInclude) {
                return 1;
              } else {
                return a[0].localeCompare(b[0]);
              }
            });
        }
        return data;
      })
    );
  }

  private getAllTranslations(): Observable<AvailableLocales> {
    if (!this.allLocales) {
      this.allLocales = this.customTranslationService.getAvailableJavaLocales().pipe(
        map(translates => Object.entries(translates)
          .filter(([localeCode]) => !this.excludeLangs.includes(localeCode))
          .sort((a, b) => a[0] > b[0] ? 1 : -1)
        ),
        shareReplay(1)
      );
    }
    return this.allLocales;
  }
}
