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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import { merge, Observable, of, Subject } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, map, share, switchMap, tap } from 'rxjs/operators';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { CustomMenuId } from '@shared/models/id/custom-menu-id';
import { CMAssigneeType, CMScope, CustomMenuInfo } from '@shared/models/custom-menu.models';
import { CustomMenuService } from '@core/http/custom-menu.service';

@Component({
  selector: 'tb-custom-menu-autocomplete',
  templateUrl: './custom-menu-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => CustomMenuAutocompleteComponent),
    multi: true
  }]
})
export class CustomMenuAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  private dirty = false;
  private refresh$ = new Subject<Array<CustomMenuInfo>>();

  selectCustomMenuFormGroup: UntypedFormGroup;

  modelValue: CustomMenuId | null;

  @Input()
  label = this.translate.instant('custom-menu.custom-menu');

  @Input()
  placeholder: string;

  private scopeValue: CMScope;
  @Input({required: true})
  set scope(value: CMScope) {
    if (this.scopeValue !== value) {
      this.scopeValue = value;
      this.selectCustomMenuFormGroup.get('customMenu').patchValue('', {emitEvent: false});
      this.refresh$.next([]);
      this.dirty = true;
    }
  }
  get scope() {
    return this.scopeValue;
  }

  @Input()
  assigneeType: CMAssigneeType;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @Input()
  requiredText: string;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @ViewChild('customMenuInput', {static: true}) customMenuInput: ElementRef;

  filteredCustomMenus: Observable<Array<CustomMenuInfo>>;

  searchText = '';

  private propagateChange = (_v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private customMenuService: CustomMenuService,
              private fb: UntypedFormBuilder) {
    this.selectCustomMenuFormGroup = this.fb.group({
      customMenu: [null, this.required ? [Validators.required] : []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  ngOnInit() {
    this.filteredCustomMenus = merge(
      this.refresh$.asObservable(),
      this.selectCustomMenuFormGroup.get('customMenu').valueChanges.pipe(
        debounceTime(150),
        tap(value => {
          let modelValue: CustomMenuId | null;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.id;
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        switchMap(name => this.fetchCustomMenus(name) ),
        share()
      )
    );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectCustomMenuFormGroup.disable({emitEvent: false});
    } else {
      this.selectCustomMenuFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: CustomMenuId | null): void {
    this.searchText = '';
    if (value != null) {
      this.customMenuService.getCustomMenuInfo(value.id, {ignoreLoading: true, ignoreErrors: true}).subscribe(
        (customMenu) => {
          this.modelValue = customMenu.id;
          this.selectCustomMenuFormGroup.get('customMenu').patchValue(customMenu, {emitEvent: false});
        }
      );
    } else {
      this.modelValue = null;
      this.selectCustomMenuFormGroup.get('customMenu').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  displayCustomMenuFn(customMenu?: CustomMenuInfo): string | undefined {
    return customMenu?.name;
  }

  onFocus() {
    if (this.dirty) {
      this.selectCustomMenuFormGroup.get('customMenu').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear() {
    this.selectCustomMenuFormGroup.get('customMenu').patchValue('');
    setTimeout(() => {
      this.customMenuInput.nativeElement.blur();
      this.customMenuInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  private updateView(value: CustomMenuId | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  private fetchCustomMenus(searchText?: string): Observable<Array<CustomMenuInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(25, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getCustomMenus(pageLink).pipe(
      catchError(() => of(emptyPageData<CustomMenuInfo>())),
      map(pageData => pageData.data)
    );
  }

  private getCustomMenus(pageLink: PageLink): Observable<PageData<CustomMenuInfo>> {
    return this.customMenuService.getCustomMenuInfos(pageLink, this.scope, this.assigneeType, {ignoreLoading: true});
  }

}
