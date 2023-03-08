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

import { Component, ElementRef, forwardRef, Input, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { AlarmSeverity, alarmSeverityTranslations } from '@shared/models/alarm.models';
import { TranslateService } from '@ngx-translate/core';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { Observable } from 'rxjs';
import { map, share } from 'rxjs/operators';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { FloatLabelType } from '@angular/material/form-field';

@Component({
  selector: 'tb-alarm-severities-list',
  templateUrl: './alarm-severities-list.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmSeveritiesListComponent),
      multi: true
    }
  ]
})
export class AlarmSeveritiesListComponent implements ControlValueAccessor{

  @ViewChild('severityInput', {static: true}) severityInput: ElementRef<HTMLInputElement>;
  @ViewChild('severityInput', {read: MatAutocompleteTrigger, static: true}) autocompleteTrigger: MatAutocompleteTrigger;

  alarmSeveritiesForm: FormGroup;

  alarmSeverityTranslationMap = alarmSeverityTranslations;
  private alarmSeveritiesValues = Object.keys(AlarmSeverity) as Array<AlarmSeverity>;
  alarmSeverity = AlarmSeverity;

  alarmSeverities: Array<AlarmSeverity> = []

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  private modelValue: Array<string> | null;

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
      this.updateValidators();
    }
  }

  @Input()
  disabled: boolean;

  @Input()
  flotLabel: FloatLabelType = 'auto';

  searchText = '';
  filteredDisplaySeverities: Observable<Array<string>>;

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private fb: FormBuilder,
              private translate: TranslateService,
              public truncate: TruncatePipe) {
    this.alarmSeveritiesForm = this.fb.group({
      alarmSeverities: [null, this.required ? [Validators.required] : []],
      alarmSeverity: ['']
    });

    this.filteredDisplaySeverities = this.alarmSeveritiesForm.get('alarmSeverity').valueChanges
      .pipe(
        map((value) => value ? value : ''),
        map(name => this.fetchSeverities(name) ),
        share()
      );
  }

  updateValidators() {
    this.alarmSeveritiesForm.get('alarmSeverities').setValidators(this.required ? [Validators.required] : []);
    this.alarmSeveritiesForm.get('alarmSeverities').updateValueAndValidity();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.alarmSeveritiesForm.disable({emitEvent: false});
    } else {
      this.alarmSeveritiesForm.enable({emitEvent: false});
    }
  }

  writeValue(value: Array<AlarmSeverity> | null): void {
    this.searchText = '';
    if (value != null && value.length > 0) {
      this.modelValue = [...value];
      this.alarmSeverities = [...value];
      this.alarmSeveritiesForm.get('alarmSeverities').setValue(value);
    } else {
      this.alarmSeverities = [];
      this.alarmSeveritiesForm.get('alarmSeverities').setValue(null);
      this.modelValue = null;
    }
    this.dirty = true;
  }

  addSeverityFromAutocomplete(event: MatAutocompleteSelectedEvent) {
    this.addSeverity(event.option.value);
  }

  addSeverityFromChipInput(event: MatChipInputEvent): void {
    const alarmSeverity = event.value || '';
    const result = this.fetchSeverities(alarmSeverity.trim());
    if (result.length === 1) {
      this.addSeverity(result[0]);
    }
  }

  private addSeverity(alarmSeverity: AlarmSeverity) {
    if (alarmSeverity) {
      if (!this.modelValue || this.modelValue.indexOf(alarmSeverity) === -1) {
        if (!this.modelValue) {
          this.modelValue = [];
        }
        this.modelValue.push(alarmSeverity);
        this.alarmSeverities.push(alarmSeverity);
        this.alarmSeveritiesForm.get('alarmSeverities').setValue(this.modelValue);
      }
      this.propagateChange(this.modelValue);
      if (!this.fetchSeverities().length) {
        this.clear('', true);
        this.autocompleteTrigger.closePanel();
      } else {
        this.clear();
      }
    }
  }

  onSeverityRemoved(alarmSeverity: string) {
    const index = this.modelValue.indexOf(alarmSeverity);
    if (index >= 0) {
      this.modelValue.splice(index, 1);
      this.alarmSeverities.splice(index, 1);
      if (!this.modelValue.length) {
        this.modelValue = null;
      }
      this.alarmSeveritiesForm.get('alarmSeverities').setValue(this.modelValue);
      this.propagateChange(this.modelValue);
      this.clear(this.severityInput.nativeElement.value, true);
      this.autocompleteTrigger.closePanel();
    }
  }

  displaySeverityFn(severity?: string): string | undefined {
    return severity ? this.translate.instant(alarmSeverityTranslations.get(AlarmSeverity[severity])) : undefined;
  }

  onFocus() {
    if (this.dirty) {
      this.alarmSeveritiesForm.get('alarmSeverity').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  private fetchSeverities(searchText?: string): Array<AlarmSeverity> {
    this.searchText = searchText;
    const allowAlarmSeverity = this.alarmSeveritiesValues.filter(severity => !this.modelValue?.includes(severity));
    if (this.textIsNotEmpty(this.searchText)) {
      const search = this.searchText.toUpperCase();
      return allowAlarmSeverity.filter(severity => severity.toUpperCase().includes(search));
    } else {
      return allowAlarmSeverity;
    }
  }

  private clear(value: string = '', ignoreFocus = false) {
    this.severityInput.nativeElement.value = value;
    this.alarmSeveritiesForm.get('alarmSeverity').patchValue(value);
    if (!ignoreFocus) {
      setTimeout(() => {
        this.severityInput.nativeElement.blur();
        this.severityInput.nativeElement.focus();
      }, 0);
    }
  }

}
