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
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { emptyPageData } from '@shared/models/page/page-data';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { FloatLabelType, MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { WidgetTypeInfo } from '@shared/models/widget.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { WidgetService } from '@core/http/widget.service';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-widget-type-autocomplete',
  templateUrl: './widget-type-autocomplete.component.html',
  styleUrls: ['./widget-type-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => WidgetTypeAutocompleteComponent),
    multi: true
  }],
  encapsulation: ViewEncapsulation.None
})
export class WidgetTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit {

  private dirty = false;

  selectWidgetTypeFormGroup: UntypedFormGroup;

  modelValue: WidgetTypeInfo | null;

  @Input()
  label = this.translate.instant('widget.widget');

  @Input()
  placeholder: string;

  @Input()
  floatLabel: FloatLabelType = 'auto';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  excludeWidgetTypeIds: Array<string>;

  @ViewChild('widgetTypeInput', {static: true}) widgetTypeInput: ElementRef;

  filteredWidgetTypes: Observable<Array<WidgetTypeInfo>>;

  searchText = '';

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private widgetService: WidgetService,
              private sanitizer: DomSanitizer,
              private fb: UntypedFormBuilder) {
    this.selectWidgetTypeFormGroup = this.fb.group({
      widgetType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredWidgetTypes = this.selectWidgetTypeFormGroup.get('widgetType').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchWidgetTypes(name) ),
        share()
      );
  }

  ngAfterViewInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectWidgetTypeFormGroup.disable({emitEvent: false});
    } else {
      this.selectWidgetTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: WidgetTypeInfo | string | null): void {
    this.searchText = '';
    if (value != null) {
      if (typeof value === 'string') {
        this.widgetService.getWidgetTypeInfoById(value).subscribe(
          (widgetType) => {
            this.modelValue = widgetType;
            this.selectWidgetTypeFormGroup.get('widgetType').patchValue(widgetType, {emitEvent: false});
          }
        );
      } else {
        this.modelValue = value;
        this.selectWidgetTypeFormGroup.get('widgetType').patchValue(value, {emitEvent: false});
      }
    } else {
      this.modelValue = null;
      this.selectWidgetTypeFormGroup.get('widgetType').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  updateView(value: WidgetTypeInfo | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayWidgetTypeFn(widgetType?: WidgetTypeInfo): string | undefined {
    return widgetType ? widgetType.name : undefined;
  }

  getPreviewImage(imageUrl: string | null): SafeUrl | string {
    if (isDefinedAndNotNull(imageUrl)) {
      return this.sanitizer.bypassSecurityTrustUrl(imageUrl);
    }
    return '/assets/widget-preview-empty.svg';
  }

  fetchWidgetTypes(searchText?: string): Observable<Array<WidgetTypeInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.getWidgetTypes(pageLink);
  }

  getWidgetTypes(pageLink: PageLink,
                 result: Array<WidgetTypeInfo> = []): Observable<Array<WidgetTypeInfo>> {
    return this.widgetService.getWidgetTypes(pageLink, true).pipe(
      catchError(() => of(emptyPageData<WidgetTypeInfo>())),
      switchMap((data) => {
        if (this.excludeWidgetTypeIds?.length) {
          const filtered = data.data.filter(w => !this.excludeWidgetTypeIds.includes(w.id.id));
          result = result.concat(filtered);
        } else {
          result = data.data;
        }
        if (result.length >= pageLink.pageSize || !this.excludeWidgetTypeIds?.length || !data.hasNext) {
          return of(result);
        } else {
          return this.getWidgetTypes(pageLink.nextPageLink(), result);
        }
      })
    );
  }

  onFocus() {
    if (this.dirty) {
      this.selectWidgetTypeFormGroup.get('widgetType').updateValueAndValidity({onlySelf: true});
      this.dirty = false;
    }
  }

  clear() {
    this.selectWidgetTypeFormGroup.get('widgetType').patchValue('');
    setTimeout(() => {
      this.widgetTypeInput.nativeElement.blur();
      this.widgetTypeInput.nativeElement.focus();
    }, 0);
  }

}
