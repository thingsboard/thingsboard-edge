///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import { Observable, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { EntityId } from '@shared/models/id/entity-id';
import { BaseData } from '@shared/models/base-data';
import { EntityService } from '@core/http/entity.service';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { QueueInfo, ServiceType } from '@shared/models/queue.models';
import { QueueService } from '@core/http/queue.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { emptyPageData } from '@shared/models/page/page-data';

@Component({
  selector: 'tb-queue-autocomplete',
  templateUrl: './queue-autocomplete.component.html',
  styleUrls: ['./queue-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => QueueAutocompleteComponent),
    multi: true
  }]
})
export class QueueAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectQueueFormGroup: FormGroup;

  modelValue: string | null;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  private hint: string;
  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set autocompleteHint(value) {
    this.hint = value;
  }

  get autocompleteHint() {
    return this.hint;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  queueType: ServiceType;

  @Input()
  disabled: boolean;

  @ViewChild('queueInput', {static: true}) queueInput: ElementRef;

  filteredQueues: Observable<Array<BaseData<EntityId>>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private entityService: EntityService,
              private queueService: QueueService,
              private fb: FormBuilder) {
    this.selectQueueFormGroup = this.fb.group({
      queueName: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredQueues = this.selectQueueFormGroup.get('queueName').valueChanges
      .pipe(
        debounceTime(150),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value.name;
          }
          this.updateView(modelValue);
          if (value === null) {
            this.clear();
          }
        }),
        map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
        distinctUntilChanged(),
        switchMap(name => this.fetchQueue(name) ),
        share()
      );
  }

  ngAfterViewInit(): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectQueueFormGroup.disable({emitEvent: false});
    } else {
      this.selectQueueFormGroup.enable({emitEvent: false});
    }
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    if (value != null) {
      this.queueService.getQueueByName(value, {ignoreLoading: true, ignoreErrors: true}).subscribe(
        (entity) => {
          this.modelValue = entity.name;
          this.selectQueueFormGroup.get('queueName').patchValue(entity, {emitEvent: false});
        },
        () => {
          this.modelValue = null;
          this.selectQueueFormGroup.get('queueName').patchValue('', {emitEvent: false});
          if (value !== null) {
            this.propagateChange(this.modelValue);
          }
        }
      );
    } else {
      this.modelValue = null;
      this.selectQueueFormGroup.get('queueName').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectQueueFormGroup.get('queueName').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  reset() {
    this.selectQueueFormGroup.get('queueName').patchValue('', {emitEvent: false});
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayQueueFn(queue?: BaseData<EntityId>): string | undefined {
    return queue ? queue.name : undefined;
  }

  fetchQueue(searchText?: string): Observable<Array<QueueInfo>> {
    this.searchText = searchText;
    const pageLink = new PageLink(10, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.queueService.getTenantQueuesByServiceType(pageLink, this.queueType, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<QueueInfo>())),
      map(pageData => {
        return pageData.data;
      })
    );
  }

  getDescription(value) {
    return value.additionalInfo?.description ? value.additionalInfo.description :
      this.translate.instant(
        'queue.alt-description',
        {submitStrategy: value.submitStrategy.type, processingStrategy: value.processingStrategy.type}
      );
  }

  clear() {
    this.selectQueueFormGroup.get('queueName').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.queueInput.nativeElement.blur();
      this.queueInput.nativeElement.focus();
    }, 0);
  }
}
