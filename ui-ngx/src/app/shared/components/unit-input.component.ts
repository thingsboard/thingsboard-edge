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
  HostBinding,
  Input,
  OnChanges,
  OnInit,
  Renderer2,
  SimpleChanges,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Observable, of, shareReplay } from 'rxjs';
import {
  AllMeasures,
  getSourceTbUnitSymbol,
  getTbUnitFromSearch,
  isTbUnitMapping,
  searchUnit,
  TbUnit,
  UnitInfo,
  UnitSystem
} from '@shared/models/unit.models';
import { map, mergeMap } from 'rxjs/operators';
import { UnitService } from '@core/services/unit.service';
import { TbPopoverService } from '@shared/components/popover.service';
import { UnitSettingsPanelComponent } from '@shared/components/unit-settings-panel.component';
import { isDefinedAndNotNull, isEqual } from '@core/utils';

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
export class UnitInputComponent implements ControlValueAccessor, OnInit, OnChanges {

  @HostBinding('style.display') readonly hostDisplay = 'flex';
  @ViewChild('unitInput', {static: true}) unitInput: ElementRef;

  unitsFormControl: FormControl<TbUnit | UnitInfo>;

  @Input({transform: booleanAttribute})
  disabled: boolean;

  @Input({transform: booleanAttribute})
  required = false;

  @Input()
  tagFilter: string;

  @Input()
  measure: AllMeasures;

  @Input()
  unitSystem: UnitSystem;

  @Input({transform: booleanAttribute})
  supportsUnitConversion = false;

  @Input({transform: booleanAttribute})
  onlySystemUnits = false;

  filteredUnits$: Observable<Array<[AllMeasures, Array<UnitInfo>]>>;

  searchText = '';

  isUnitMapping = false;

  private dirty = false;

  private modelValue: TbUnit | null;

  private fetchUnits$: Observable<Array<[AllMeasures, Array<UnitInfo>]>> = null;

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private unitService: UnitService,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private elementRef: ElementRef) {
  }

  ngOnInit() {
    this.unitsFormControl = this.fb.control<TbUnit | UnitInfo>('', this.required ? [Validators.required] : []);
    this.filteredUnits$ = this.unitsFormControl.valueChanges.pipe(
      map(value => {
        this.updateModel(value);
        return getSourceTbUnitSymbol(value);
      }),
      mergeMap(symbol => this.fetchUnits(symbol))
    );
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'measure' || propName === 'unitSystem') {
          this.fetchUnits$ = null;
          this.dirty = true;
        }
      }
    }
  }

  writeValue(symbol?: TbUnit): void {
    this.searchText = '';
    this.modelValue = symbol;
    if (typeof symbol === 'string') {
      this.unitsFormControl.patchValue(this.unitService.getUnitInfo(symbol) ?? symbol, {emitEvent: false});
      this.isUnitMapping = false;
    } else {
      this.unitsFormControl.patchValue(symbol, {emitEvent: false});
      this.isUnitMapping = isDefinedAndNotNull(symbol);
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.unitsFormControl.updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  displayUnitFn(unit?: TbUnit | UnitInfo): string | undefined {
    if (unit) {
      return getSourceTbUnitSymbol(unit);
    }
    return undefined;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.unitsFormControl.disable({emitEvent: false});
    } else {
      this.unitsFormControl.enable({emitEvent: false});
    }
  }

  clear($event: Event) {
    $event.stopPropagation();
    this.unitsFormControl.patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.unitInput.nativeElement.blur();
      this.unitInput.nativeElement.focus();
    }, 0);
  }

  openUnitSettingsPopup($event: Event) {
    if (!this.supportsUnitConversion) {
      return;
    }
    $event.stopPropagation();
    this.unitInput.nativeElement.blur();
    const trigger = this.elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const popover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: UnitSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['left', 'bottom', 'top'],
        context: {
          unit: getTbUnitFromSearch(this.unitsFormControl.value),
          required: this.required,
          disabled: this.disabled,
          tagFilter: this.tagFilter,
          measure: this.measure
        },
        isModal: true
      });
      popover.tbComponentRef.instance.unitSettingsApplied.subscribe((unitSetting) => {
        popover.hide();
        this.unitsFormControl.patchValue(unitSetting, {emitEvent: false});
        this.updateModel(unitSetting);
      });
    }
  }

  private updateModel(value: UnitInfo | TbUnit ) {
    let res = getTbUnitFromSearch(value);
    if (this.onlySystemUnits && !isTbUnitMapping(res)) {
      const unitInfo = this.unitService.getUnitInfo(res as string);
      if (unitInfo) {
        if (this.measure && unitInfo.measure !== this.measure) {
          res = null;
        }
      } else {
        res = null;
      }
    }
    if (!isEqual(this.modelValue, res)) {
      this.modelValue = res;
      this.isUnitMapping = isTbUnitMapping(res);
      this.propagateChange(this.modelValue);
    }
  }

  private fetchUnits(searchText?: string): Observable<Array<[AllMeasures, Array<UnitInfo>]>> {
    this.searchText = searchText;
    return this.getGroupedUnits().pipe(
      map(unit => searchUnit(unit, searchText))
    );
  }

  private getGroupedUnits(): Observable<Array<[AllMeasures, Array<UnitInfo>]>> {
    if (this.fetchUnits$ === null) {
      this.fetchUnits$ = of(this.unitService.getUnitsGroupedByMeasure(this.measure, this.unitSystem, this.tagFilter)).pipe(
        map(data => Object.entries(data) as Array<[AllMeasures, UnitInfo[]]>),
        shareReplay(1)
      );
    }
    return this.fetchUnits$;
  }
}
