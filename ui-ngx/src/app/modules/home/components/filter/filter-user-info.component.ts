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
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  BooleanOperation,
  EntityKeyValueType,
  KeyFilterPredicateUserInfo, NumericOperation,
  StringOperation
} from '@shared/models/query/query.models';
import { MatDialog } from '@angular/material/dialog';
import {
  FilterUserInfoDialogComponent,
  FilterUserInfoDialogData
} from '@home/components/filter/filter-user-info-dialog.component';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-filter-user-info',
  templateUrl: './filter-user-info.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FilterUserInfoComponent),
      multi: true
    }
  ]
})
export class FilterUserInfoComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() key: string;

  @Input() operation: StringOperation | BooleanOperation | NumericOperation;

  @Input() valueType: EntityKeyValueType;

  private propagateChange = null;

  private keyFilterPredicateUserInfo: KeyFilterPredicateUserInfo;

  constructor(private dialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(keyFilterPredicateUserInfo: KeyFilterPredicateUserInfo): void {
    this.keyFilterPredicateUserInfo = keyFilterPredicateUserInfo;
  }

  public openFilterUserInfoDialog() {
   this.dialog.open<FilterUserInfoDialogComponent, FilterUserInfoDialogData,
     KeyFilterPredicateUserInfo>(FilterUserInfoDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        keyFilterPredicateUserInfo: deepClone(this.keyFilterPredicateUserInfo),
        valueType: this.valueType,
        key: this.key,
        operation: this.operation,
        readonly: this.disabled
      }
    }).afterClosed().subscribe(
      (result) => {
        if (result) {
          this.keyFilterPredicateUserInfo = result;
          this.updateModel();
        }
      }
    );
  }

  private updateModel() {
    this.propagateChange(this.keyFilterPredicateUserInfo);
  }

}
