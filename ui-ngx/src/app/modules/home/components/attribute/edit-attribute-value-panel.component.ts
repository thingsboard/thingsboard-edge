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

import { Component, Inject, InjectionToken, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { OverlayRef } from '@angular/cdk/overlay';

export const EDIT_ATTRIBUTE_VALUE_PANEL_DATA = new InjectionToken<any>('EditAttributeValuePanelData');

export interface EditAttributeValuePanelData {
  attributeValue: any;
}

@Component({
  selector: 'tb-edit-attribute-value-panel',
  templateUrl: './edit-attribute-value-panel.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: EditAttributeValuePanelComponent}],
  styleUrls: ['./edit-attribute-value-panel.component.scss']
})
export class EditAttributeValuePanelComponent extends PageComponent implements OnInit, ErrorStateMatcher {

  attributeFormGroup: UntypedFormGroup;

  result: any = null;

  submitted = false;

  constructor(protected store: Store<AppState>,
              @Inject(EDIT_ATTRIBUTE_VALUE_PANEL_DATA) public data: EditAttributeValuePanelData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public overlayRef: OverlayRef,
              public fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.attributeFormGroup = this.fb.group({
      value: [this.data.attributeValue, [Validators.required]]
    });
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  update(): void {
    this.submitted = true;
    this.result = this.attributeFormGroup.get('value').value;
    this.overlayRef.dispose();
  }
}
