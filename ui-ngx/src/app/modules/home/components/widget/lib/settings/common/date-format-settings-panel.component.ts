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
import { DateFormatSettings } from '@home/components/widget/config/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormControl, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'tb-date-format-settings-panel',
  templateUrl: './date-format-settings-panel.component.html',
  providers: [],
  styleUrls: ['./date-format-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DateFormatSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  dateFormat: DateFormatSettings;

  @Input()
  popover: TbPopoverComponent<DateFormatSettingsPanelComponent>;

  @Output()
  dateFormatApplied = new EventEmitter<DateFormatSettings>();

  dateFormatFormControl: UntypedFormControl;

  previewText = '';

  constructor(private date: DatePipe,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.dateFormatFormControl = new UntypedFormControl(this.dateFormat.format, [Validators.required]);
    this.dateFormatFormControl.valueChanges.subscribe((value: string) => {
      this.previewText = this.date.transform(Date.now(), value);
    });
    this.previewText = this.date.transform(Date.now(), this.dateFormat.format);
  }

  cancel() {
    this.popover?.hide();
  }

  applyDateFormat() {
    this.dateFormat.format = this.dateFormatFormControl.value;
    this.dateFormatApplied.emit(this.dateFormat);
  }

}
