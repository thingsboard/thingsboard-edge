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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { ColorRange } from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import {
  ColorRangeSettingsComponent
} from '@home/components/widget/lib/settings/common/color-range-settings.component';

@Component({
  selector: 'tb-color-range-panel',
  templateUrl: './color-range-panel.component.html',
  providers: [],
  styleUrls: ['./color-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ColorRangePanelComponent extends PageComponent implements OnInit {

  @Input()
  colorRangeSettings: Array<ColorRange>;

  @Input()
  popover: TbPopoverComponent<ColorRangePanelComponent>;

  @Input()
  settingsComponents: ColorRangeSettingsComponent[];

  @Output()
  colorRangeApplied = new EventEmitter<Array<ColorRange>>();

  colorRangeFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.colorRangeFormGroup = this.fb.group(
      {
        rangeList: [this.colorRangeSettings, []]
      }
    );
  }

  copyColorSettings(comp: ColorRangeSettingsComponent) {
    this.colorRangeSettings = deepClone(comp.modelValue);
    this.colorRangeFormGroup.get('rangeList').patchValue(this.colorRangeSettings || [], {emitEvent: false});
    this.colorRangeFormGroup.markAsDirty();
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorRangeSettings() {
    const colorRangeSettings = this.colorRangeFormGroup.get('rangeList').value;
    this.colorRangeApplied.emit(colorRangeSettings);
  }

}
