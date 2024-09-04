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
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { FormBuilder, FormGroup, UntypedFormGroup, Validators } from '@angular/forms';
import { Observable, of, shareReplay, Subject } from 'rxjs';
import { catchError, distinctUntilChanged, map, startWith, switchMap, tap } from 'rxjs/operators';
import { ConverterLibraryService } from '@home/components/converter/converter-library.service';
import { IntegrationDirectory, IntegrationType } from '@shared/models/integration.models';
import { Converter, ConverterType, Model, Vendor } from '@shared/models/converter.models';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { isDefined, isEqual } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-converter-library',
  templateUrl: './converter-library.component.html',
  styleUrls: ['./converter-library.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConverterLibraryComponent implements OnChanges {

  @coerceBoolean()
  @Input() isUplink = true;

  @Input() integrationType: IntegrationType;

  @Output() converterChanged = new EventEmitter<UntypedFormGroup>();

  @ViewChild('modelInput', { static: true }) modelInput: ElementRef;
  @ViewChild('vendorInput', { static: true }) vendorInput: ElementRef;
  @ViewChild('dataConverter') dataConverterComponent: ConverterComponent;

  libraryFormGroup: FormGroup;
  vendors$: Observable<Array<Vendor>>;
  models$: Observable<Model[]>;
  converter$: Observable<Converter>;
  filteredModels$: Observable<Array<Model>>;
  filteredVendors$: Observable<Array<Vendor>>;
  integrationDir: IntegrationDirectory;
  vendorInputSubject = new Subject<void>();
  modelInputSubject = new Subject<void>();
  showConverter = false;
  initialConverter: Converter = {} as Converter;

  constructor(
    private fb: FormBuilder,
    private converterLibraryService: ConverterLibraryService,
  ) {
    this.libraryFormGroup = fb.group({
      vendor: ['', Validators.required],
      model: ['', Validators.required]
    });

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
          map(searchValue => {
              this.libraryFormGroup.get('model').patchValue('');
              return vendors.filter(vendor =>
                vendor.name.includes(searchValue) || searchValue?.name === vendor.name
              )
            }
          )
        )
      )
    );

    this.models$ = this.modelInputSubject.asObservable().pipe(
      switchMap(() => of(this.libraryFormGroup.get('vendor').value)),
      distinctUntilChanged(),
      switchMap(() => {
          if (this.libraryFormGroup.get('vendor').value?.name) {
            return this.converterLibraryService.getModels(this.integrationDir, this.libraryFormGroup.get('vendor').value.name)
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
          map(searchValue =>
            models.filter(model =>
              model.info.label.includes(searchValue) || searchValue?.info?.label === model.info.label
            )
          )
        )
      )
    );

    this.converter$ = this.libraryFormGroup.get('vendor').valueChanges.pipe(
      startWith(null),
      switchMap(vendor =>
        vendor ? this.libraryFormGroup.get('model').valueChanges.pipe(
          startWith(null),
          switchMap(model =>
            model ? this.converterLibraryService.getConverter(
              this.integrationDir,
              vendor?.name ?? '',
              model?.name ?? '',
              this.isUplink
            ).pipe(catchError(() => of(this.initialConverter)))
              : of(this.initialConverter)
          )
        ) : of(this.initialConverter)
      ),
      tap(converter => this.onConverterChanged(converter))
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.integrationType && this.integrationType) {
      this.libraryFormGroup.get('vendor').patchValue('');
      this.libraryFormGroup.get('model').patchValue('');
      this.integrationDir = IntegrationDirectory[this.integrationType] ?? this.integrationType;
    }
    if (changes.isUplink && isDefined(this.isUplink)) {
      this.initialConverter.type = this.isUplink ? ConverterType.UPLINK : ConverterType.DOWNLINK;
      if (!this.isUplink) {
        this.libraryFormGroup.get('vendor').clearValidators();
        this.libraryFormGroup.get('model').clearValidators();
      }
    }
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

  private onConverterChanged(converter: Converter): void {
    const converterData = {...converter};
    delete converterData.type;
    this.showConverter = !isEqual(converterData, {});
    setTimeout(() => {
      this.converterChanged.emit(this.dataConverterComponent.entityForm)
    });
  }
}
