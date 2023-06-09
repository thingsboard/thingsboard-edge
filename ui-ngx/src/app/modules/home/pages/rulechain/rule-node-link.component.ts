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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { FcRuleEdge, LinkLabel } from '@shared/models/rule-node.models';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-rule-node-link',
  templateUrl: './rule-node-link.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => RuleNodeLinkComponent),
    multi: true
  }]
})
export class RuleNodeLinkComponent implements ControlValueAccessor, OnInit {

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

  private allowCustomValue: boolean;
  get allowCustom(): boolean {
    return this.allowCustomValue;
  }
  @Input()
  set allowCustom(value: boolean) {
    this.allowCustomValue = coerceBooleanProperty(value);
  }

  @Input()
  allowedLabels: {[label: string]: LinkLabel};

  @Input()
  sourceRuleChainId: string;

  ruleNodeLinkFormGroup: UntypedFormGroup;

  modelValue: FcRuleEdge;

  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder,
              public truncate: TruncatePipe,
              public translate: TranslateService) {
    this.ruleNodeLinkFormGroup = this.fb.group({
      labels: [[], Validators.required]
    });
    this.ruleNodeLinkFormGroup.get('labels').valueChanges.subscribe(
      (labels: string[]) => this.updateModel(labels)
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.ruleNodeLinkFormGroup.disable({emitEvent: false});
    } else {
      this.ruleNodeLinkFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: FcRuleEdge): void {
    this.modelValue = value;
    const labels = this.modelValue && this.modelValue.labels ? this.modelValue.labels : [];
    this.ruleNodeLinkFormGroup.get('labels').patchValue(labels, {emitEvent: false});
  }

  private updateModel(labels: string[]) {
    if (labels && labels.length) {
      this.modelValue.labels = labels;
      this.modelValue.label = labels.join(' / ');
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(this.required ? null : this.modelValue);
    }
  }
}
