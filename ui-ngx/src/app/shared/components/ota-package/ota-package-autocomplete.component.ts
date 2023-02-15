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

import { Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { merge, Observable, of, Subject } from 'rxjs';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { OtaPackageInfo, OtaUpdateTranslation, OtaUpdateType } from '@shared/models/ota-package.models';
import { OtaPackageService } from '@core/http/ota-package.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { getEntityDetailsPageURL, isDefinedAndNotNull } from '@core/utils';
import { emptyPageData, PageData } from '@shared/models/page/page-data';

@Component({
  selector: 'tb-ota-package-autocomplete',
  templateUrl: './ota-package-autocomplete.component.html',
  styleUrls: ['./ota-package-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => OtaPackageAutocompleteComponent),
    multi: true
  }]
})
export class OtaPackageAutocompleteComponent implements ControlValueAccessor, OnInit {

  otaPackageFormGroup: FormGroup;

  modelValue: string | EntityId | null;

  private otaUpdateType: OtaUpdateType = OtaUpdateType.FIRMWARE;

  get type(): OtaUpdateType {
    return this.otaUpdateType;
  }

  @Input()
  set type(value ) {
    this.otaUpdateType = value ? value : OtaUpdateType.FIRMWARE;
    this.reset();
  }

  private deviceProfile: string;

  get deviceProfileId(): string {
    return this.deviceProfile;
  }

  @Input()
  set deviceProfileId(value: string) {
    this.deviceProfile = value;
    this.reset();
  }

  @Input()
  deviceGroupId: string;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  @Input()
  useFullEntityId = false;

  private deviceGroupAllValue: boolean;

  get deviceGroupAll(): boolean {
    return this.deviceGroupAllValue;
  }

  @Input()
  set deviceGroupAll(value: boolean) {
    this.deviceGroupAllValue = coerceBooleanProperty(value);
    this.setDisabledState(this.deviceGroupAll);
  }

  @Input()
  showDetailsPageLink = false;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  @ViewChild('packageInput', {static: true}) packageInput: ElementRef;

  filteredPackages: Observable<Array<OtaPackageInfo>>;

  searchText = '';
  packageURL: string;

  private dirty = false;
  private cleanFilteredPackages: Subject<Array<OtaPackageInfo>> = new Subject();

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private otaPackageService: OtaPackageService,
              private fb: FormBuilder) {
    this.otaPackageFormGroup = this.fb.group({
      packageId: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    const getPackages = this.otaPackageFormGroup.get('packageId').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = this.useFullEntityId ? value.id : value.id.id;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.title) : ''),
        switchMap(name => this.fetchPackages(name)),
        share()
      );

    this.filteredPackages = merge(this.cleanFilteredPackages, getPackages);
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy() {
    this.cleanFilteredPackages.complete();
    this.cleanFilteredPackages = null;
  }

  getCurrentEntity(): OtaPackageInfo | null {
    const currentPackage = this.otaPackageFormGroup.get('packageId').value;
    if (currentPackage && typeof currentPackage !== 'string') {
      return currentPackage as OtaPackageInfo;
    } else {
      return null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled || this.deviceGroupAll;
    if (this.disabled) {
      this.otaPackageFormGroup.disable({emitEvent: false});
    } else {
      this.otaPackageFormGroup.enable({emitEvent: false});
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  writeValue(value: string | EntityId | null): void {
    this.searchText = '';
    if (value != null && value !== '') {
      let packageId = '';
      if (typeof value === 'string') {
        packageId = value;
      } else if (value.entityType && value.id) {
        packageId = value.id;
      }
      if (packageId !== '') {
        this.entityService.getEntity(EntityType.OTA_PACKAGE, packageId, {ignoreLoading: true, ignoreErrors: true}).subscribe(
          (entity) => {
            this.packageURL = getEntityDetailsPageURL(entity.id.id, EntityType.OTA_PACKAGE);
            this.modelValue = this.useFullEntityId ? entity.id : entity.id.id;
            this.otaPackageFormGroup.get('packageId').patchValue(entity, {emitEvent: false});
          },
          () => {
            this.modelValue = null;
            this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
            if (value !== null) {
              this.propagateChange(this.modelValue);
            }
          }
        );
      } else {
        this.modelValue = null;
        this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
        this.propagateChange(null);
      }
    } else {
      this.modelValue = null;
      this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.otaPackageFormGroup.get('packageId').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  reset() {
    this.cleanFilteredPackages.next([]);
    this.otaPackageFormGroup.get('packageId').patchValue('', {emitEvent: false});
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayPackageFn(packageInfo?: OtaPackageInfo): string | undefined {
    return packageInfo ? `${packageInfo.title} (${packageInfo.version})` : undefined;
  }

  fetchPackages(searchText?: string): Observable<Array<OtaPackageInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(50, 0, searchText, {
      property: 'title',
      direction: Direction.ASC
    });
    let fetchFirmware$: Observable<PageData<OtaPackageInfo>>;
    if (isDefinedAndNotNull(this.deviceGroupId)) {
      fetchFirmware$ = this.otaPackageService
        .getOtaPackagesInfoByDeviceGroupId(pageLink, this.deviceGroupId, this.type, {ignoreLoading: true});
    } else{
      fetchFirmware$ = this.otaPackageService
        .getOtaPackagesInfoByDeviceProfileId(pageLink, this.deviceProfileId, this.type, {ignoreLoading: true});
    }
    return fetchFirmware$.pipe(
      catchError(() => of(emptyPageData<OtaPackageInfo>())),
      map((data) => data && data.data.length ? data.data : null)
    );
  }

  clear() {
    this.otaPackageFormGroup.get('packageId').patchValue('');
    setTimeout(() => {
      this.packageInput.nativeElement.blur();
      this.packageInput.nativeElement.focus();
    }, 0);
  }

  get placeholderText(): string {
    return this.labelText || OtaUpdateTranslation.get(this.type).label;
  }

  get requiredErrorText(): string {
    return this.requiredText || OtaUpdateTranslation.get(this.type).required;
  }

  get notFoundPackage(): string {
    return OtaUpdateTranslation.get(this.type).noFound;
  }

  get notMatchingPackage(): string {
    return OtaUpdateTranslation.get(this.type).noMatching;
  }

  get hintText(): string {
    return OtaUpdateTranslation.get(this.type).hint;
  }

  packageTitleText(firpackageInfomware: OtaPackageInfo): string {
    return `${firpackageInfomware.title} (${firpackageInfomware.version})`;
  }
}
