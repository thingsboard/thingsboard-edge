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

import { AfterViewInit, Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, mergeMap, startWith, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { MessageType, messageTypeNames } from '@shared/models/rule-node.models';
import { objectValues } from '@core/utils';

@Component({
  selector: 'tb-message-type-autocomplete',
  templateUrl: './message-type-autocomplete.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MessageTypeAutocompleteComponent),
    multi: true
  }]
})
export class MessageTypeAutocompleteComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  messageTypeFormGroup: UntypedFormGroup;

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

  @ViewChild('messageTypeInput', {static: true}) messageTypeInput: ElementRef;

  filteredMessageTypes: Observable<Array<MessageType | string>>;

  searchText = '';

  private dirty = false;

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              public translate: TranslateService,
              private fb: UntypedFormBuilder) {
    this.messageTypeFormGroup = this.fb.group({
      messageType: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredMessageTypes = this.messageTypeFormGroup.get('messageType').valueChanges
      .pipe(
        tap(value => {
          this.updateView(value);
        }),
        startWith<string | MessageType>(''),
        map(value => value ? value : ''),
        mergeMap(messageType => this.fetchMessageTypes(messageType) )
      );
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.messageTypeFormGroup.disable({emitEvent: false});
    } else {
      this.messageTypeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: string | null): void {
    this.searchText = '';
    this.modelValue = value;
    let res: MessageType | string = null;
    if (value) {
      if (objectValues(MessageType).includes(value)) {
        res = MessageType[value];
      } else {
        res = value;
      }
    }
    this.messageTypeFormGroup.get('messageType').patchValue(res, {emitEvent: false});
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.messageTypeFormGroup.get('messageType').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  updateView(value: MessageType | string | null) {
    let res: string = null;
    if (value) {
      if (objectValues(MessageType).includes(value)) {
        res = MessageType[value];
      } else {
        res = value;
      }
    }
    if (this.modelValue !== res) {
      this.modelValue = res;
      this.propagateChange(this.modelValue);
    }
  }

  displayMessageTypeFn(messageType?: MessageType | string): string | undefined {
    if (messageType) {
      if (objectValues(MessageType).includes(messageType)) {
        return messageTypeNames.get(MessageType[messageType]);
      } else {
        return messageType;
      }
    }
    return undefined;
  }

  fetchMessageTypes(searchText?: string): Observable<Array<MessageType | string>> {
    this.searchText = searchText;
    const result: Array<MessageType | string> = [];
    messageTypeNames.forEach((value, key) => {
      if (value.toUpperCase().includes(searchText.toUpperCase())) {
        result.push(key);
      }
    });
    if (result.length) {
      return of(result);
    } else {
      return of([searchText]);
    }
  }

  clear() {
    this.messageTypeFormGroup.get('messageType').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.messageTypeInput.nativeElement.blur();
      this.messageTypeInput.nativeElement.focus();
    }, 0);
  }

}
