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

import {
  booleanAttribute,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, shareReplay } from 'rxjs';
import { catchError, debounceTime, map, switchMap, tap } from 'rxjs/operators';
import { PageLink } from '@shared/models/page/page-link';
import { isDefinedAndNotNull } from '@core/utils';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { SecretStorage, SecretStorageType } from '@shared/models/secret-storage.models';
import { SecretStorageService } from '@core/http/secret-storage.service';
import { emptyPageData } from '@shared/models/page/page-data';
import { Direction } from '@shared/models/page/sort-order';

@Component({
  selector: 'tb-secret-autocomplete',
  templateUrl: './secret-autocomplete.component.html',
  styleUrls: ['./secret-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SecretAutocompleteComponent),
    multi: true
  }],
  encapsulation: ViewEncapsulation.None
})
export class SecretAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectSecretFormGroup: FormGroup;

  private modelValue: string | null;

  @Input({required: true})
  secretType: SecretStorageType;

  @Input({transform: booleanAttribute})
  required: boolean;

  @Input()
  disabled: boolean;

  @ViewChild('converterInput', {static: true}) converterInput: ElementRef<HTMLInputElement>;
  @ViewChild('converterInput', {static: true, read: MatAutocompleteTrigger}) converterAutocomplete: MatAutocompleteTrigger;

  labelText: string;
  requiredText: string;

  filteredEntities: Observable<Array<SecretStorage>>;

  selectedSecret: string;

  searchText = '';

  private dirty = false;

  private propagateChange: (value: any) => void = () => {};

  constructor(private secretStorageService: SecretStorageService,
              private fb: FormBuilder) {
    this.selectSecretFormGroup = this.fb.group({
      secret: [null, [Validators.required]]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.labelText = this.secretType === SecretStorageType.TEXT ? 'secret-storage.autocomplete-title.text' : 'secret-storage.autocomplete-title.file';
    this.requiredText = this.secretType === SecretStorageType.TEXT ? 'secret-storage.autocomplete-title.text-required' : 'secret-storage.autocomplete-title.file-required';

    this.filteredEntities = this.selectSecretFormGroup.get('secret').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (value === null) {
            this.clear();
          } else {
            if (typeof value === 'string' || !value) {
              modelValue = null;
            } else {
              modelValue = value.name;
              this.selectedSecret = value.name;
              this.converterInput.nativeElement.value = '';
            }
            this.updateView(modelValue);
          }
        }),
        map(value => value ? (typeof value === 'string' ? value.trim() : value.name) : ''),
        switchMap(name => this.fetchEntities(name)),
        shareReplay(1)
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectSecretFormGroup.disable({emitEvent: false});
    } else {
      this.selectSecretFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    if (isDefinedAndNotNull(value)) {
      this.secretStorageService.getSecretByName(value, {ignoreLoading: true}).subscribe(
        (secret) => {
          if (secret) {
            this.modelValue = secret.name;
            this.selectedSecret = secret.name;
            this.selectSecretFormGroup.get('secret').patchValue(secret, {emitEvent: false});
          }
        }
      );
    } else {
      this.modelValue = null;
      this.selectSecretFormGroup.get('secret').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectSecretFormGroup.get('secret').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  private updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayEntityFn(secret?: SecretStorage): string | undefined {
    return secret ? secret.name : undefined;
  }

  private fetchEntities(searchText?: string): Observable<Array<SecretStorage>> {
    this.searchText = searchText;
    let limit = 50;
    const pageLink = new PageLink(limit, 0, this.searchText, {property: 'name', direction: Direction.ASC});
    return this.secretStorageService.getSecrets(pageLink, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<SecretStorage>())),
      map((data) => {
        let entities: Array<SecretStorage>;
        entities = data.data;
        if (this.secretType) {
          entities = entities.filter((secret) => secret.type === this.secretType);
        }
        return entities;
        }
      ));
  }

  clear() {
    this.selectSecretFormGroup.get('secret').patchValue('', {emitEvent: true});
    this.converterInput.nativeElement.value = '';
    this.selectedSecret = null;
    setTimeout(() => {
      this.converterInput.nativeElement.blur();
      this.converterInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return text?.trim().length > 0;
  }
}
