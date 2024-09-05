///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { merge, Observable, of, shareReplay, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, startWith, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ConverterLibraryService } from '@core/http/converter-library.service';
import { IntegrationDirectory, IntegrationType } from '@shared/models/integration.models';
import { Converter, ConverterType, Model, Vendor } from '@shared/models/converter.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { isEmptyStr } from '@core/utils';

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
})
export class ConverterLibraryComponent implements ControlValueAccessor, Validator, OnChanges {

  @Input() converterType = ConverterType.UPLINK;
  @Input() integrationType: IntegrationType;

  @ViewChild('modelInput', { static: true }) modelInput: ElementRef;
  @ViewChild('vendorInput', { static: true }) vendorInput: ElementRef;
  @ViewChild('dataConverter') dataConverterComponent: ConverterComponent;

  libraryFormGroup: UntypedFormGroup;
  vendors$: Observable<Array<Vendor>>;
  models$: Observable<Model[]>;
  converter$: Observable<Converter>;
  filteredModels$: Observable<Array<Model>>;
  filteredVendors$: Observable<Array<Vendor>>;
  integrationDir: IntegrationDirectory;
  vendorInputSubject = new Subject<void>();
  modelInputSubject = new Subject<void>();

  private destroy$ = new Subject<void>();

  private onChange!: (converter: Converter) => void;
  private onTouched!: () => void;

  constructor(
    private fb: FormBuilder,
    private converterLibraryService: ConverterLibraryService,
  ) {
    this.libraryFormGroup = fb.group({
      vendor: ['', Validators.required],
      model: ['', Validators.required],
      converter: this.fb.group({})
    });

    merge(this.libraryFormGroup.valueChanges, this.converterFormGroup.valueChanges).pipe(
      filter(() => !!this.onChange),
      takeUntil(this.destroy$)
    ).subscribe(value => this.onChange(value?.converter));

    this.vendors$ = this.vendorInputSubject.asObservable().pipe(
      switchMap(() => of(this.integrationDir)),
      distinctUntilChanged(),
      switchMap(() =>
        this.converterLibraryService.getVendors(this.integrationDir)
      ),
      shareReplay(1)
    );

    this.filteredVendors$ = this.vendors$.pipe(
      switchMap(vendors =>
        this.libraryFormGroup.get('vendor').valueChanges.pipe(
          startWith(''),
          map(value => {
            this.libraryFormGroup.get('model').patchValue('');
            if (isEmptyStr(value)) {
              return vendors;
            }
            const searchValue = (value?.name ?? value.trim()).toLowerCase();
            return vendors.filter(vendor => vendor.name.toLowerCase().includes(searchValue));
          })
        )
      )
    );

    this.models$ = this.modelInputSubject.asObservable().pipe(
      switchMap(() => of(this.libraryFormGroup.get('vendor').value)),
      distinctUntilChanged(),
      switchMap(() => {
          if (this.libraryFormGroup.get('vendor').value?.name) {
            return this.converterLibraryService.getModels(this.integrationDir, this.libraryFormGroup.get('vendor').value.name, this.converterType)
          }
          this.libraryFormGroup.get('model').patchValue('');
          return of([]);
        }
      ),
      shareReplay(1)
    );

    this.filteredModels$ = this.models$.pipe(
      switchMap(models =>
        this.libraryFormGroup.get('model').valueChanges.pipe(
          startWith(''),
          map(value => {
            if (isEmptyStr(value)) {
              return models;
            }
            const searchValue = (value?.name ?? value.trim()).toLowerCase();
            return models.filter(model => model.name.toLowerCase().includes(searchValue));
          })
        )
      )
    );

    this.converter$ = this.libraryFormGroup.get('vendor').valueChanges.pipe(
      switchMap(vendor =>
        this.libraryFormGroup.get('model').valueChanges.pipe(
          switchMap(model =>
            model?.name && vendor?.name
              ? this.converterLibraryService.getConverter(this.integrationDir, vendor.name, model.name, this.converterType)
              : of(null)
          ),
          catchError(() => of(null))
        )
      ),
      tap((converter: Converter) => {
        if (!converter) {
          return;
        }
        this.onConverterChanged();
      })
    );
  }

  get converterFormGroup(): FormGroup {
    return this.libraryFormGroup.get('converter') as FormGroup;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (
      changes.integrationType
      && !changes.integrationType.firstChange
      && changes.integrationType.currentValue !== changes.integrationType.previousValue
    ) {
      this.libraryFormGroup.get('vendor').reset();
      this.libraryFormGroup.get('model').reset();
      this.integrationDir = IntegrationDirectory[this.integrationType] ?? this.integrationType;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setDisabledState(isDisabled: boolean): void {
    isDisabled
      ? this.converterFormGroup.disable({emitEvent: false})
      : this.converterFormGroup.enable({emitEvent: false});
    this.converterFormGroup.updateValueAndValidity();
  }

  registerOnChange(fn: (converter: Converter) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(_): void {
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
    setTimeout(() => {
      this.vendorInput.nativeElement.blur();
      this.vendorInput.nativeElement.focus();
    }, 0);
  }

  trackByName(_, item: Vendor | Model): string {
    return item.name;
  }

  private onConverterChanged(): void {
    setTimeout(() => {
      Object.keys(this.converterFormGroup.controls).forEach(key => this.converterFormGroup.removeControl(key))
      Object.keys(this.dataConverterComponent.entityForm.controls).forEach(key => {
        this.converterFormGroup.addControl(key, this.dataConverterComponent.entityForm.controls[key]);
      });
      this.converterFormGroup.updateValueAndValidity();
    });
  }
}
