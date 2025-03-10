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
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
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
import { combineLatest, debounce, interval, Observable, of, shareReplay, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, map, startWith, switchMap, takeUntil, } from 'rxjs/operators';
import { ConverterLibraryService } from '@core/http/converter-library.service';
import { IntegrationType } from '@shared/models/integration.models';
import { Converter, ConverterLibraryValue, ConverterType, Model, Vendor } from '@shared/models/converter.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
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
export class ConverterLibraryComponent implements ControlValueAccessor, Validator, OnChanges, AfterViewInit, OnDestroy {

  @Input() converterType = ConverterType.UPLINK;
  @Input() integrationType: IntegrationType;

  @ViewChild('modelInput') modelInput: ElementRef;
  @ViewChild('vendorInput', { static: true }) vendorInput: ElementRef;
  @ViewChild('dataConverter', { static: true }) dataConverterComponent: ConverterComponent;

  libraryFormGroup: UntypedFormGroup;
  vendors$: Observable<Array<Vendor>>;
  models$: Observable<Model[]>;
  converter$: Observable<Converter>;
  filteredModels$: Observable<Array<Model>>;
  filteredVendors$: Observable<Array<Vendor>>;
  vendorInputSubject = new Subject<void>();
  modelInputSubject = new Subject<void>();

  private destroy$ = new Subject<void>();

  private onChange: (converter: Converter) => void = (_) => {};

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
      .subscribe(() => this.onChange(this.libraryFormGroup.get('converter')?.getRawValue()));

    this.vendors$ = this.vendorInputSubject.asObservable().pipe(
      switchMap(() => of(this.integrationType)),
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
      map((models: Model[]) => models?.map(model => ({ ...model, searchText: (model.name + model.info.description).toLowerCase() }))),
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
          const debugSettings = { allEnabled: true, failuresEnabled: true };
          return converter ? { ...converter, debugSettings } : { type: this.converterType, debugSettings } as Converter;
        })
    );
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

  ngAfterViewInit(): void {
    this.libraryFormGroup.setControl('converter', this.dataConverterComponent.entityForm, {emitEvent: false});
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
      this.updateScriptLangEnable();
      this.libraryFormGroup.updateValueAndValidity();
    }
  }

  registerOnChange(fn: (converter: Converter) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(_): void {
  }

  writeValue(converterLibraryValue: ConverterLibraryValue): void {
    if (isDefinedAndNotNull(converterLibraryValue)) {
      this.libraryFormGroup.patchValue(converterLibraryValue, {emitEvent: false});
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

  private updateScriptLangEnable(): void {
    const converterControl = this.libraryFormGroup.get('converter');
    if (!converterControl) {return;}

    const { decoder, encoder, tbelDecoder, tbelEncoder, type } = converterControl.value.configuration || {};
    const scriptLangControl = converterControl.get('configuration')?.get('scriptLang');

    if (type === ConverterType.UPLINK && (!decoder || !tbelDecoder)) {
      scriptLangControl?.disable({ emitEvent: false });
    } else if (!encoder || !tbelEncoder) {
      scriptLangControl?.disable({ emitEvent: false });
    }
  }
}
