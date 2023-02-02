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

import { Component, forwardRef, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ScriptLanguage } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-script-lang',
  templateUrl: './script-lang.component.html',
  styleUrls: ['./script-lang.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TbScriptLangComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class TbScriptLangComponent extends PageComponent implements ControlValueAccessor, OnInit {

  scriptLangFormGroup: FormGroup;

  scriptLanguage = ScriptLanguage;

  @Input()
  disabled: boolean;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
    this.scriptLangFormGroup = this.fb.group({
      scriptLang: [null]
    });
  }

  ngOnInit() {
    this.scriptLangFormGroup.get('scriptLang').valueChanges.subscribe(
      (scriptLang) => {
        this.updateView(scriptLang);
      }
    );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.scriptLangFormGroup.disable({emitEvent: false});
    } else {
      this.scriptLangFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(scriptLang: ScriptLanguage): void {
    this.scriptLangFormGroup.get('scriptLang').patchValue(scriptLang, {emitEvent: false});
  }

  updateView(scriptLang: ScriptLanguage) {
    this.propagateChange(scriptLang);
  }
}
