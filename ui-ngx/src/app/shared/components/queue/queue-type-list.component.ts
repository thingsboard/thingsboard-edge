///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  publishReplay,
  refCount,
  switchMap,
  tap
} from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { QueueService } from '@core/http/queue.service';
import { ServiceType } from '@shared/models/queue.models';

interface Queue {
  queueName: string;
}

@Component({
  selector: 'tb-queue-type-list',
  templateUrl: './queue-type-list.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => QueueTypeListComponent),
    multi: true
  }]
})
export class QueueTypeListComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  queueFormGroup: FormGroup;

  modelValue: Queue | null;

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

  @Input()
  queueType: ServiceType;

  @ViewChild('queueInput', {static: true}) queueInput: ElementRef<HTMLInputElement>;

  filteredQueues: Observable<Array<Queue>>;

  queues: Observable<Array<Queue>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private queueService: QueueService,
              private fb: FormBuilder) {
    this.queueFormGroup = this.fb.group({
      queue: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredQueues = this.queueFormGroup.get('queue').valueChanges
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(value => {
          let modelValue;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          this.updateView(modelValue);
        }),
        map(value => value ? (typeof value === 'string' ? value : value.queueName) : ''),
        switchMap(queue => this.fetchQueues(queue) )
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.queueFormGroup.disable({emitEvent: false});
    } else {
      this.queueFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value ? { queueName: value } : null;
    this.queueFormGroup.get('queue').patchValue(this.modelValue, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.queueFormGroup.get('queue').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: Queue | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue ? this.modelValue.queueName : null);
    }
  }

  displayQueueFn(queue?: Queue): string | undefined {
    return queue ? queue.queueName : undefined;
  }

  fetchQueues(searchText?: string): Observable<Array<Queue>> {
    this.searchText = searchText;
    return this.getQueues().pipe(
      catchError(() => of([] as Array<Queue>)),
      map(queues => {
        const result = queues.filter( queue => {
          return searchText ? queue.queueName.toUpperCase().startsWith(searchText.toUpperCase()) : true;
        });
        if (result.length) {
          result.sort((q1, q2) => q1.queueName.localeCompare(q2.queueName));
        }
        return result;
      })
    );
  }

  getQueues(): Observable<Array<Queue>> {
    if (!this.queues) {
      this.queues = this.queueService.
      getTenantQueuesByServiceType(this.queueType, {ignoreLoading: true}).pipe(
        map((queues) => {
          return queues.map((queueName) => {
            return { queueName };
          });
        }),
        publishReplay(1),
        refCount()
      );
    }
    return this.queues;
  }

  clear() {
    this.queueFormGroup.get('queue').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.queueInput.nativeElement.blur();
      this.queueInput.nativeElement.focus();
    }, 0);
  }

}
