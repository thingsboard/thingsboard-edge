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
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  forwardRef,
  Input, OnChanges,
  OnInit, SimpleChanges,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { fromEvent, merge } from 'rxjs';
import { debounceTime, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ConnectedPosition,
  FlexibleConnectedPositionStrategy,
  Overlay,
  OverlayRef,
  PositionStrategy
} from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';

@Component({
  selector: 'tb-string-pattern-autocomplete',
  templateUrl: './string-pattern-autocomplete.component.html',
  styleUrls: ['./string-pattern-autocomplete.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => StringPatternAutocompleteComponent),
      multi: true
    }
  ]
})
export class StringPatternAutocompleteComponent implements ControlValueAccessor, OnInit, OnChanges {

  @ViewChild('inputRef', {static: true}) inputRef: ElementRef;
  @ViewChild('highlightTextRef', {static: true}) highlightTextRef: ElementRef;
  @ViewChild('autocompleteTemplate', {static: true}) autocompleteTemplate: TemplateRef<any>;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  required = false;

  @Input({required: true})
  predefinedValues: Array<string>;

  @Input()
  placeholderText: string = this.translate.instant('widget-config.set');

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  additionalClass: string | string[] | Record<string, boolean | undefined | null>;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  label: string;

  @Input()
  tooltipClass = 'tb-error-tooltip';

  @Input()
  errorText: string;

  @Input()
  @coerceBoolean()
  showInlineError = false;

  @Input()
  patternSymbol = '$';

  selectionFormControl: FormControl;
  filteredOptions: Array<string>;

  searchText = '';
  highlightedHtml = ''

  private modelValue: string | null;
  private overlayRef!: OverlayRef;

  private propagateChange = (_val: any) => {
  };

  constructor(private fb: FormBuilder,
              private overlay: Overlay,
              private translate: TranslateService,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.selectionFormControl = this.fb.control('', this.required ? [Validators.required] : []);
    merge(
      fromEvent(this.inputRef.nativeElement, 'selectionchange'),
      this.selectionFormControl.valueChanges.pipe(tap(value => this.updateView(value)))
    ).pipe(
      debounceTime(50),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.onSelectionChange();
      this.cd.markForCheck();
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'predefinedValues' || propName === 'patternSymbol') {
          this.highlightedHtml = this.getHighlightHtml(this.modelValue);
        }
        if (propName === 'required') {
          if (change.currentValue) {
            this.selectionFormControl.addValidators(Validators.required);
          } else {
            this.selectionFormControl.removeValidators(Validators.required);
          }
          this.selectionFormControl.updateValueAndValidity({emitEvent: false})
        }
      }
    }
  }

  writeValue(option?: string): void {
    this.searchText = '';
    this.modelValue = option ?? null;
    this.highlightedHtml = this.getHighlightHtml(this.modelValue);
    this.selectionFormControl.patchValue(this.modelValue, {emitEvent: false});
  }

  onFocus() {
    this.onSelectionChange();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectionFormControl.disable({emitEvent: false});
      this.closeAutocomplete();
    } else {
      this.selectionFormControl.enable({emitEvent: false});
    }
  }

  onInputScroll(event: Event) {
    const scrollLeft = (event.target as HTMLInputElement).scrollLeft;
    const scrollTop = (event.target as HTMLInputElement).scrollTop;
    if (this.highlightTextRef && this.highlightTextRef.nativeElement) {
      this.highlightTextRef.nativeElement.scrollLeft = scrollLeft;
      this.highlightTextRef.nativeElement.scrollTop = scrollTop;
    }
  }

  optionSelected(value: string) {
    const position = this.inputRef.nativeElement.selectionStart;
    const triggerIndex = this.modelValue.lastIndexOf(this.patternSymbol, position - 1);
    if (triggerIndex === -1) {
      return;
    }
    const newText = `${this.modelValue.substring(0, triggerIndex + 1)}${value}${this.modelValue.substring(position)}`;
    this.selectionFormControl.patchValue(newText);
    this.searchText = '';
    setTimeout(() => {
      this.inputRef.nativeElement.setSelectionRange(triggerIndex + value.length + 1, triggerIndex + value.length + 1);
      this.inputRef.nativeElement.focus();
    });
    this.closeAutocomplete();
  }

  private updateView(value: string) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.highlightedHtml = this.getHighlightHtml(this.modelValue);
      this.propagateChange(this.modelValue);
    }
  }

  private onSelectionChange() {
    const selectionStart = this.inputRef.nativeElement.selectionStart;
    const selectionEnd = this.inputRef.nativeElement.selectionEnd;
    if (selectionStart === selectionEnd && selectionStart > 0 && !this.disabled) {
      this.filteredOptions = this.getFilteredOptions(this.modelValue, selectionStart);
    } else {
      this.filteredOptions = [];
    }
    if (this.filteredOptions.length) {
      this.openAutocomplete(selectionStart);
    } else {
      this.closeAutocomplete()
    }
  }

  private getHighlightHtml(text: string): string {
    if (!text) {
      return '';
    }
    const regex = new RegExp('([' + this.patternSymbol + ']\\w+)', 'g');
    return text.replace(regex, (_match: string, p1: string) => {
      if (this.predefinedValues.includes(p1.substring(1))) {
        return `<span class="highlight">${p1}</span>`
      }
      return p1;
    });
  }

  private getFilteredOptions(text: string, index = text.length): Array<string> {
    const triggerIndex = text.lastIndexOf(this.patternSymbol, index - 1);
    if (triggerIndex === -1) {
      return [];
    }

    let currentWordEndIndex = text.indexOf(' ', index);

    if (currentWordEndIndex === -1) {
      currentWordEndIndex = text.length;
    }

    this.searchText = text.substring(triggerIndex + 1, currentWordEndIndex).toLowerCase();
    if (this.searchText.includes(' ')) {
      return [];
    }

    const result = this.predefinedValues.filter(value => value.toLowerCase().startsWith(this.searchText));
    if (result.length === 1 && result[0].toLowerCase() === this.searchText) {
      return [];
    }
    return result;
  }

  private openAutocomplete(cursorIndex?: number): void {
    const patternIndex = this.modelValue.lastIndexOf(this.patternSymbol, cursorIndex - 1);
    if (!this.overlayRef) {
      this.overlayRef = this.overlay.create({
        positionStrategy: this.getOverlayPosition(patternIndex),
        scrollStrategy: this.overlay.scrollStrategies.reposition(),
        panelClass: 'tb-select-overlay',
        backdropClass: 'cdk-overlay-transparent-backdrop',
        hasBackdrop: true
      });
      this.overlayRef.backdropClick().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.closeAutocomplete());
    }
    if (!this.overlayRef.hasAttached()) {
      const templatePortal = new TemplatePortal(this.autocompleteTemplate, this.viewContainerRef);
      this.overlayRef.attach(templatePortal);
      this.overlayRef.updatePositionStrategy(this.getOverlayPosition(patternIndex))
    }
  }

  private closeAutocomplete(): void {
    if (this.overlayRef && this.overlayRef.hasAttached()) {
      this.overlayRef.detach();
    }
  }

  private getOverlayPosition(patternIndex: number): PositionStrategy {
    const strategy = this.overlay
      .position()
      .flexibleConnectedTo(this.inputRef)
      .withFlexibleDimensions(false)
      .withPush(false);

    this.setStrategyPositions(strategy, patternIndex);
    return strategy;
  }

  private setStrategyPositions(positionStrategy: FlexibleConnectedPositionStrategy, patternIndex: number) {
    let offsetX = patternIndex > 0 ? patternIndex * 8 : 0;
    const inputBounds = this.inputRef.nativeElement.getBoundingClientRect();
    if (offsetX + 180 > inputBounds.width) {
      offsetX = inputBounds.width - 180;
    }
    const belowPositions: ConnectedPosition[] = [
      {originX: 'start', originY: 'bottom', overlayX: 'start', overlayY: 'top', offsetX},
      {originX: 'end', originY: 'bottom', overlayX: 'end', overlayY: 'top', offsetX},
    ];

    const panelClass = 'mat-mdc-autocomplete-panel-above';
    const abovePositions: ConnectedPosition[] = [
      {originX: 'start', originY: 'top', overlayX: 'start', overlayY: 'bottom', panelClass, offsetX},
      {originX: 'end', originY: 'top', overlayX: 'end', overlayY: 'bottom', panelClass, offsetX},
    ];

    const positions = [...belowPositions, ...abovePositions];
    positionStrategy.withPositions(positions);
  }
}
