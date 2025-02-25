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

import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { aggregationTranslations, AggregationType } from '@shared/models/time/time.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-aggregation-type-select',
  templateUrl: './aggregation-type-select.component.html',
  styleUrls: ['./aggregation-type-select.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => AggregationTypeSelectComponent),
    multi: true
  }]
})
export class AggregationTypeSelectComponent implements ControlValueAccessor, OnInit, OnChanges {

  aggregationTypeFormGroup: FormGroup;

  modelValue: AggregationType | null;

  @Input()
  allowedAggregationTypes: Array<AggregationType>;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  displayLabel = true;

  @Input()
  labelText: string;

  get label(): string {
    if (this.labelText && this.labelText.length) {
      return this.labelText;
    }
    return this.defaultLabel;
  }

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  aggregationTypes: Array<AggregationType>;

  private defaultLabel = this.translate.instant('aggregation.aggregation');

  private allAggregationTypes: Array<AggregationType> = Object.values(AggregationType);

  private propagateChange = (v: any) => { };

  constructor(private translate: TranslateService,
              private fb: FormBuilder) {
    this.aggregationTypeFormGroup = this.fb.group({
      aggregationType: [null]
    });
    this.aggregationTypeFormGroup.get('aggregationType').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(
      (value) => {
        let modelValue;
        if (!value) {
          modelValue = null;
        } else {
          modelValue = value;
        }
        this.updateView(modelValue);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.aggregationTypes = this.allowedAggregationTypes?.length ? this.allowedAggregationTypes : this.allAggregationTypes;
  }

  ngOnChanges({allowedAggregationTypes}: SimpleChanges): void {
    if (allowedAggregationTypes && !allowedAggregationTypes.firstChange && !isEqual(allowedAggregationTypes.currentValue, allowedAggregationTypes.previousValue)) {
      this.aggregationTypes = this.allowedAggregationTypes?.length ? this.allowedAggregationTypes : this.allAggregationTypes;
      const currentAggregationType: AggregationType = this.aggregationTypeFormGroup.get('aggregationType').value;
      if (currentAggregationType && !this.aggregationTypes.includes(currentAggregationType)) {
        this.aggregationTypeFormGroup.get('aggregationType').patchValue(this.aggregationTypes[0], {emitEvent: true});
      }
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.aggregationTypeFormGroup.disable();
    } else {
      this.aggregationTypeFormGroup.enable();
    }
  }

  writeValue(value: AggregationType | null): void {
    let aggregationType: AggregationType;
    if (value && this.allowedAggregationTypes?.length && !this.allowedAggregationTypes.includes(value)) {
      aggregationType = this.allowedAggregationTypes[0];
    } else {
      aggregationType = value;
    }
    this.modelValue = aggregationType;
    this.aggregationTypeFormGroup.get('aggregationType').patchValue(aggregationType, {emitEvent: false});
  }

  updateView(value: AggregationType | null) {
    if (this.modelValue !== value) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  displayAggregationTypeFn(aggregationType?: AggregationType | null): string | undefined {
    if (aggregationType) {
      return this.translate.instant(aggregationTranslations.get(aggregationType));
    } else {
      return '';
    }
  }

}
