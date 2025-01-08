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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { FetchTo, FetchToTranslation } from '../rule-node-config.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-msg-metadata-chip',
  templateUrl: './msg-metadata-chip.component.html',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MsgMetadataChipComponent),
    multi: true
  }]
})

export class MsgMetadataChipComponent implements OnInit, ControlValueAccessor, OnDestroy {

  @Input() labelText: string;
  @Input() translation: Map<FetchTo, string> = FetchToTranslation;

  private propagateChange: (value: any) => void = () => {};
  private destroy$ = new Subject<void>();

  public chipControlGroup: FormGroup;
  public selectOptions = [];

  constructor(private fb: FormBuilder,
              private translate: TranslateService) {}

  ngOnInit(): void {
    this.initOptions();
    this.chipControlGroup = this.fb.group({
      chipControl: [null, []]
    });

    this.chipControlGroup.get('chipControl').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
        if (value) {
          this.propagateChange(value);
        }
      }
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  initOptions() {
    for (const key of this.translation.keys()) {
      this.selectOptions.push({
        value: key,
        name: this.translate.instant(this.translation.get(key))
      });
    }
  }

  writeValue(value: string | null): void {
    this.chipControlGroup.get('chipControl').patchValue(value, {emitEvent: false});
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.chipControlGroup.disable({emitEvent: false});
    } else {
      this.chipControlGroup.enable({emitEvent: false});
    }
  }
}
