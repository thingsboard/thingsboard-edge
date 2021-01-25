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
import { Observable } from 'rxjs';
import { map, mergeMap, publishReplay, refCount, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { QueueService } from '@core/http/queue.service';
import { ServiceType } from '@shared/models/queue.models';

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

  modelValue: string | null;

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

  filteredQueues: Observable<Array<string>>;

  queues: Observable<Array<string>>;

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
        tap(value => {
          this.updateView(value);
        }),
        map(value => value ? value : ''),
        mergeMap(queue => this.fetchQueues(queue) )
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
    this.modelValue = value;
    this.queueFormGroup.get('queue').patchValue(value, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.queueFormGroup.get('queue').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: string | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayQueueFn(queue?: string): string | undefined {
    return queue ? queue : undefined;
  }

  fetchQueues(searchText?: string): Observable<Array<string>> {
    this.searchText = searchText;
    return this.getQueues().pipe(
      map(queues => {
        const result = queues.filter( queue => {
          return searchText ? queue.toUpperCase().startsWith(searchText.toUpperCase()) : true;
        });
        if (result.length) {
          if (searchText && searchText.length && result.indexOf(searchText) === -1) {
            result.push(searchText);
          }
          result.sort();
        } else if (searchText && searchText.length) {
          result.push(searchText);
        }
        return result;
      })
    );
  }

  getQueues(): Observable<Array<string>> {
    if (!this.queues) {
      this.queues = this.queueService.
      getTenantQueuesByServiceType(this.queueType, {ignoreLoading: true}).pipe(
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
