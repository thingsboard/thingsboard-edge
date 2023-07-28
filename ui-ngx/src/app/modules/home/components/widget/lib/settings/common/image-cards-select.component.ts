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
  AfterContentInit,
  Component,
  ContentChildren,
  Directive,
  ElementRef,
  forwardRef,
  Input, OnChanges,
  OnDestroy, OnInit,
  QueryList, SimpleChanges,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormControl } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import { BehaviorSubject, combineLatest, Observable, Subject } from 'rxjs';
import { map, share, startWith, takeUntil } from 'rxjs/operators';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';

export interface ImageCardsSelectOption {
  name: string;
  value: any;
  image: string;
}

@Directive(
  {
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'tb-image-cards-select-option',
  }
)
export class ImageCardsSelectOptionDirective {

  @Input() value: any;

  @Input() image: string;

  get viewValue(): string {
    return (this._element?.nativeElement.textContent || '').trim();
  }

  constructor(
    private _element: ElementRef<HTMLElement>
  ) {}
}

@Component({
  selector: 'tb-image-cards-select',
  templateUrl: './image-cards-select.component.html',
  styleUrls: ['./image-cards-select.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ImageCardsSelectComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ImageCardsSelectComponent implements ControlValueAccessor, OnInit, OnChanges, AfterContentInit, OnDestroy {

  @ContentChildren(ImageCardsSelectOptionDirective) imageCardsSelectOptions: QueryList<ImageCardsSelectOptionDirective>;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  cols = 4;

  @Input()
  colsLtMd = 2;

  @Input()
  rowHeight = '9:5';

  @Input()
  label: string;

  valueFormControl: UntypedFormControl;

  options: ImageCardsSelectOption[] = [];

  modelValue: any;

  expanded = false;

  cols$: Observable<number>;

  private propagateChange = null;

  private _destroyed = new Subject<void>();

  private _colsChanged = new BehaviorSubject<void>(null);

  constructor(private breakpointObserver: BreakpointObserver) {
    this.valueFormControl = new UntypedFormControl('');
  }

  ngOnInit(): void {
    const gridColumns = this.breakpointObserver.isMatched(MediaBreakpoints['lt-md']) ? this.colsLtMd : this.cols;
    this.cols$ = combineLatest({state: this.breakpointObserver
      .observe(MediaBreakpoints['lt-md']), colsChanged: this._colsChanged.asObservable()}).pipe(
        map((data) => data.state.matches ? this.colsLtMd : this.cols),
        startWith(gridColumns),
        share()
      );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['cols', 'colsLtMd'].includes(propName)) {
          this._colsChanged.next(null);
        }
      }
    }
  }

  ngAfterContentInit(): void {
    this.imageCardsSelectOptions.changes.pipe(startWith(null), takeUntil(this._destroyed)).subscribe(() => {
      this.syncImageCardsSelectOptions();
    });
  }

  ngOnDestroy() {
    this._destroyed.next();
    this._destroyed.complete();
  }

  private syncImageCardsSelectOptions() {
    if (this.imageCardsSelectOptions?.length) {
      this.options.length = 0;
      this.imageCardsSelectOptions.forEach(option => {
        this.options.push(
          { name: option.viewValue,
            value: option.value,
            image: option.image
          }
        );
      });
      this.updateDisplayValue();
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
      this.valueFormControl.disable();
    } else {
      this.valueFormControl.enable();
    }
  }

  writeValue(value: any): void {
    this.modelValue = value;
    this.updateDisplayValue();
  }

  updateModel(value: any) {
    this.modelValue = value;
    this.updateDisplayValue();
    this.propagateChange(this.modelValue);
    this.expanded = false;
  }

  toggleSelectPanel($event: Event) {
    $event.stopPropagation();
    if (!this.disabled) {
      this.expanded = !this.expanded;
    }
  }

  private updateDisplayValue() {
    const currentOption = this.options.find(o => o.value === this.modelValue);
    const displayValue = currentOption ? currentOption.name : '';
    this.valueFormControl.patchValue(displayValue, {emitEvent: false});
  }
}
