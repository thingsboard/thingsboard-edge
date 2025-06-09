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
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { combineLatest, debounce, interval, Observable, of, shareReplay, Subject, Subscription } from 'rxjs';
import { catchError, distinctUntilChanged, map, startWith, switchMap, takeUntil, } from 'rxjs/operators';
import { ConverterLibraryService } from '@core/http/converter-library.service';
import { IntegrationType } from '@shared/models/integration.models';
import { Converter, ConverterLibraryInfo, ConverterType, Model, Vendor } from '@shared/models/converter.models';
import { isDefinedAndNotNull, isEmptyStr, isNotEmptyStr } from '@core/utils';

@Component({
  selector: 'tb-converter-library',
  templateUrl: './converter-library.component.html',
  styleUrls: ['./converter-library.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ConverterLibraryComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ConverterLibraryComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ConverterLibraryComponent implements ControlValueAccessor, Validator, OnChanges, OnDestroy, OnInit {

  @Input() converterType = ConverterType.UPLINK;
  @Input() integrationType: IntegrationType;
  @Input() set interacted(interacted: boolean) {
    if (interacted) {
      this.libraryFormGroup.markAllAsTouched();
    }
  }

  @Output() converter = new EventEmitter<Converter>();

  @ViewChild('modelInput') modelInput: ElementRef;
  @ViewChild('vendorInput', { static: true }) vendorInput: ElementRef;

  libraryFormGroup: UntypedFormGroup;
  vendors$: Observable<Array<Vendor>>;
  models$: Observable<Model[]>;
  converter$: Subscription;
  filteredModels$: Observable<Array<Model>>;
  filteredVendors$: Observable<Array<Vendor>>;
  vendorInputSubject = new Subject<void>();
  modelInputSubject = new Subject<void>();

  private destroy$ = new Subject<void>();
  private modelValue: ConverterLibraryInfo;
  private propagateChange: (value: any) => void = () => {};

  constructor(
    private fb: FormBuilder,
    private converterLibraryService: ConverterLibraryService,
  ) {
    this.libraryFormGroup = this.fb.group({
      vendor: ['', Validators.required],
      model: ['', Validators.required],
    });

    this.libraryFormGroup.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updateView(this.libraryFormGroup.getRawValue()));
  }

  ngOnInit() {
    this.vendors$ = this.vendorInputSubject.asObservable().pipe(
      switchMap(() => of(`${this.integrationType};${this.converterType}`)),
      distinctUntilChanged(),
      switchMap(() =>
        this.converterLibraryService.getVendors(this.integrationType, this.converterType)
      ),
      shareReplay(1)
    );

    this.filteredVendors$ = combineLatest([
      this.vendorValueChanges,
      this.vendors$
    ]).pipe(
      map(([value, vendors]) => {
        this.libraryFormGroup.get('model').patchValue('');
        if (isEmptyStr(value)) {
          return vendors;
        }
        const searchValue = ((value as Vendor)?.name ?? (value as string).trim()).toLowerCase();
        return vendors.filter(vendor => vendor.name.toLowerCase().includes(searchValue));
      }),
      shareReplay(1)
    );

    this.models$ = this.modelInputSubject.asObservable().pipe(
      switchMap(() => of(this.libraryFormGroup.get('vendor').value)),
      distinctUntilChanged(),
      switchMap(() => {
          if (this.libraryFormGroup.get('vendor').value?.name) {
            return this.converterLibraryService.getModels(this.integrationType, this.libraryFormGroup.get('vendor').value.name, this.converterType);
          }
          return of(null);
        }
      ),
      map((models: Model[]) => models?.map(model => ({ ...model, searchText: (model.info.label + model.info.description).toLowerCase() }))),
      shareReplay(1)
    );

    this.filteredModels$ = combineLatest([
      this.models$,
      this.modelValueChanges
    ]).pipe(
      map(([models, value]) => {
        if (isEmptyStr(value)) {
          return models;
        }
        const searchValue = ((value as Model)?.name ?? (value as string).trim()).toLowerCase();
        return models.filter(model => model.searchText.toLowerCase().includes(searchValue));
      }),
      shareReplay(1)
    );

    this.converter$ = combineLatest([this.vendorValueChanges, this.modelValueChanges])
      .pipe(
        switchMap(([vendor, model]: [Vendor, Model]) =>
          vendor?.name && model?.name
            ? this.converterLibraryService.getConverter(this.integrationType, vendor.name, model.name, this.converterType)
            : of(null)
        ),
        catchError(() => of(null)),
        distinctUntilChanged(),
        map((converter: Converter) => {
          const defaultDebugSettings = { allEnabled: true, failuresEnabled: true };
          const defaultConverter = {
            integrationType: this.integrationType,
            converterVersion: 1,
            debugSettings: defaultDebugSettings
          } as Converter;

          if (converter) {
            return {
              ...defaultConverter,
              ...converter,
              debugSettings: defaultDebugSettings
            };
          }
          return defaultConverter;
        }),
        takeUntil(this.destroy$)
    ).subscribe(value => this.converter.emit(value));
  }

  get vendorValueChanges(): Observable<Vendor | string> {
    return this.libraryFormGroup.get('vendor').valueChanges.pipe(
      startWith(''),
      debounce((value) => isNotEmptyStr(value) ? interval(300) : of(0)),
      distinctUntilChanged(),
    );
  }

  get modelValueChanges(): Observable<Model | string> {
    return this.libraryFormGroup.get('model').valueChanges.pipe(
      startWith(''),
      debounce((value) => isNotEmptyStr(value) ? interval(300) : of(0)),
      distinctUntilChanged(),
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (
      changes.integrationType
      && !changes.integrationType.firstChange
      && changes.integrationType.currentValue !== changes.integrationType.previousValue
    ) {
      this.libraryFormGroup.get('vendor').reset('');
      this.libraryFormGroup.get('model').reset('');
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.libraryFormGroup.disable({emitEvent: false});
    } else {
      this.libraryFormGroup.enable({emitEvent: false});
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: any): void {
  }

  private updateView(value: ConverterLibraryInfo) {
    if (this.modelValue !== value && (value.model && value.vendor)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

  writeValue(converterLibraryValue: ConverterLibraryInfo): void {
    if (isDefinedAndNotNull(converterLibraryValue)) {
      this.modelValue = converterLibraryValue;
      this.libraryFormGroup.patchValue(converterLibraryValue, {emitEvent: true});
    } else {
      this.modelValue = null;
      this.libraryFormGroup.patchValue( {vendor: '', model: ''}, {emitEvent: true})
    }
  }

  validate(): ValidationErrors | null {
    return this.libraryFormGroup.valid ? null : {
      converterFormGroup: {valid: false}
    };
  }

  displayModelFn(model?: Model): string {
    return model ? model.info.label : '';
  }

  displayVendorFn(vendor?: Vendor): string {
    return vendor ? vendor.name : '';
  }

  onLinkClick(event: MouseEvent, url: string): void {
    event.stopPropagation();
    window.open(url, '_blank');
  }

  clearModel(): void {
    this.libraryFormGroup.get('model').patchValue('');
    setTimeout(() => {
      this.modelInput?.nativeElement.blur();
      this.modelInput?.nativeElement.focus();
    }, 0);
  }

  clearVendor(): void {
    this.libraryFormGroup.get('vendor').patchValue('');
    this.modelInputSubject.next();
    setTimeout(() => {
      this.vendorInput.nativeElement.blur();
      this.vendorInput.nativeElement.focus();
    }, 0);
  }
}
