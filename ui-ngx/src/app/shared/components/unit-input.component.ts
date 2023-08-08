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

import {
  Component,
  ElementRef,
  forwardRef,
  HostBinding,
  Input,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of, shareReplay, switchMap } from 'rxjs';
import { getUnits, searchUnits, Unit, unitBySymbol } from '@shared/models/unit.models';
import { map, mergeMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { ResourcesService } from '@core/services/resources.service';

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

  @HostBinding('style.display') get hostDisplay() {return 'flex';};

  unitsFormControl: FormControl;

  modelValue: string | null;

  @Input()
  disabled: boolean;

  @ViewChild('unitInput', {static: true}) unitInput: ElementRef;

  filteredUnits: Observable<Array<Unit | string>>;

  searchText = '';

  private dirty = false;

  private fetchUnits$: Observable<Array<Unit>> = null;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private resourcesService: ResourcesService,
              private translate: TranslateService) {
  }

  ngOnInit() {
    this.unitsFormControl = this.fb.control('', []);
    this.filteredUnits = this.unitsFormControl.valueChanges
      .pipe(
        tap(value => {
          this.updateView(value);
        }),
        map(value => (value as Unit)?.symbol ? (value as Unit).symbol : (value ? value as string : '')),
        mergeMap(symbol => this.fetchUnits(symbol))
      );
  }

  writeValue(symbol?: string): void {
    this.searchText = '';
    this.modelValue = symbol;
    of(symbol).pipe(
      switchMap(value => value
        ? this.unitsConstant().pipe(map(units => unitBySymbol(units, value) ?? value))
        : of(null))
    ).subscribe(result => {
      this.unitsFormControl.patchValue(result, {emitEvent: false});
      this.dirty = true;
    });
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
    return this.unitsConstant().pipe(
      map(unit => searchUnits(unit, searchText))
    );
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

  private unitsConstant(): Observable<Array<Unit>> {
    if (this.fetchUnits$ === null) {
      this.fetchUnits$ = getUnits(this.resourcesService).pipe(
        map(units => units.map(u => ({
          symbol: u.symbol,
          name: this.translate.instant(u.name),
          tags: u.tags
        }))),
        shareReplay(1)
      );
    }
    return this.fetchUnits$;
  }
}
