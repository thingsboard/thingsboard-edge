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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, UntypedFormBuilder } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { searchUnits, Unit, unitBySymbol, units } from '@shared/models/unit.models';
import { map, mergeMap, startWith, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-unit-input',
  templateUrl: './unit-input.component.html',
  styleUrls: ['./unit-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UnitInputComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class UnitInputComponent implements ControlValueAccessor, OnInit {

  unitsFormControl: FormControl;

  modelValue: string | null;

  @Input()
  disabled: boolean;

  @ViewChild('unitInput', {static: true}) unitInput: ElementRef;

  filteredUnits: Observable<Array<Unit | string>>;

  searchText = '';

  private dirty = false;

  private translatedUnits: Array<Unit> = units.map(u => ({symbol: u.symbol,
    name: this.translate.instant(u.name),
    tags: u.tags}));

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private translate: TranslateService) {
  }

  ngOnInit() {
    this.unitsFormControl = this.fb.control('', []);
    this.filteredUnits = this.unitsFormControl.valueChanges
      .pipe(
        tap(value => {
          this.updateView(value);
        }),
        startWith<string | Unit>(''),
        map(value => (value as Unit)?.symbol ? (value as Unit).symbol : (value ? value as string : '')),
        mergeMap(symbol => this.fetchUnits(symbol) )
      );
  }

  writeValue(symbol?: string): void {
    this.searchText = '';
    this.modelValue = symbol;
    let res: Unit | string = null;
    if (symbol) {
      const unit = unitBySymbol(symbol);
      res = unit ? unit : symbol;
    }
    this.unitsFormControl.patchValue(res, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.unitsFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: Unit | string | null) {
    const res: string = (value as Unit)?.symbol ? (value as Unit)?.symbol : (value as string);
    if (this.modelValue !== res) {
      this.modelValue = res;
      this.propagateChange(this.modelValue);
    }
  }

  displayUnitFn(unit?: Unit | string): string | undefined {
    if (unit) {
      if ((unit as Unit).symbol) {
        return (unit as Unit).symbol;
      } else {
        return unit as string;
      }
    }
    return undefined;
  }

  fetchUnits(searchText?: string): Observable<Array<Unit | string>> {
    this.searchText = searchText;
    const result = searchUnits(this.translatedUnits, searchText);
    if (result.length) {
      return of(result);
    } else {
      return of([]);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.unitsFormControl.disable({emitEvent: false});
    } else {
      this.unitsFormControl.enable({emitEvent: false});
    }
  }

  clear() {
    this.unitsFormControl.patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.unitInput.nativeElement.blur();
      this.unitInput.nativeElement.focus();
    }, 0);
  }
}
