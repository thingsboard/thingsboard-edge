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
import { defaultTimewindowStyle, TimewindowStyle } from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Timewindow } from '@shared/models/time/time.models';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-timewindow-style-panel',
  templateUrl: './timewindow-style-panel.component.html',
  providers: [],
  styleUrls: ['./timewindow-style-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TimewindowStylePanelComponent extends PageComponent implements OnInit {

  @Input()
  timewindowStyle: TimewindowStyle;

  @Input()
  previewValue: Timewindow;

  @Input()
  popover: TbPopoverComponent<TimewindowStylePanelComponent>;

  @Output()
  timewindowStyleApplied = new EventEmitter<TimewindowStyle>();

  timewindowStyleFormGroup: UntypedFormGroup;

  previewTimewindowStyle: TimewindowStyle;

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    const computedTimewindowStyle = {...defaultTimewindowStyle, ...(this.timewindowStyle || {})};
    this.timewindowStyleFormGroup = this.fb.group(
      {
        showIcon: [computedTimewindowStyle.showIcon, []],
        iconSize: [computedTimewindowStyle.iconSize, []],
        icon: [computedTimewindowStyle.icon, []],
        iconPosition: [computedTimewindowStyle.iconPosition, []],
        font: [computedTimewindowStyle.font, []],
        color: [computedTimewindowStyle.color, []]
      }
    );
    this.updatePreviewStyle(this.timewindowStyle);
    this.updateTimewindowStyleEnabledState();
    this.timewindowStyleFormGroup.valueChanges.subscribe((timewindowStyle: TimewindowStyle) => {
      if (this.timewindowStyleFormGroup.valid) {
        this.updatePreviewStyle(timewindowStyle);
        setTimeout(() => {this.popover?.updatePosition();}, 0);
      }
    });
    this.timewindowStyleFormGroup.get('showIcon').valueChanges.subscribe(() => {
      this.updateTimewindowStyleEnabledState();
    });
  }

  cancel() {
    this.popover?.hide();
  }

  applyTimewindowStyle() {
    const timewindowStyle = this.timewindowStyleFormGroup.getRawValue();
    this.timewindowStyleApplied.emit(timewindowStyle);
  }

  private updateTimewindowStyleEnabledState() {
    const showIcon: boolean = this.timewindowStyleFormGroup.get('showIcon').value;
    if (showIcon) {
      this.timewindowStyleFormGroup.get('iconSize').enable({emitEvent: false});
      this.timewindowStyleFormGroup.get('icon').enable({emitEvent: false});
      this.timewindowStyleFormGroup.get('iconPosition').enable({emitEvent: false});
    } else {
      this.timewindowStyleFormGroup.get('iconSize').disable({emitEvent: false});
      this.timewindowStyleFormGroup.get('icon').disable({emitEvent: false});
      this.timewindowStyleFormGroup.get('iconPosition').disable({emitEvent: false});
    }
  }

  private updatePreviewStyle(timewindowStyle: TimewindowStyle) {
    this.previewTimewindowStyle = deepClone(timewindowStyle);
  }

}
