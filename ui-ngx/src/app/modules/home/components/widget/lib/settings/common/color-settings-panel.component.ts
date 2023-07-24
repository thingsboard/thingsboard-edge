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
import {
  ColorRange,
  ColorSettings,
  ColorType,
  colorTypeTranslations
} from '@home/components/widget/config/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Datasource, DatasourceType } from '@shared/models/widget.models';
import { deepClone } from '@core/utils';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-color-settings-panel',
  templateUrl: './color-settings-panel.component.html',
  providers: [],
  styleUrls: ['./color-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ColorSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  colorSettings: ColorSettings;

  @Input()
  popover: TbPopoverComponent<ColorSettingsPanelComponent>;

  @Output()
  colorSettingsApplied = new EventEmitter<ColorSettings>();

  colorType = ColorType;

  colorTypes = Object.keys(ColorType) as ColorType[];

  colorTypeTranslationsMap = colorTypeTranslations;

  colorSettingsFormGroup: UntypedFormGroup;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.colorSettingsFormGroup = this.fb.group(
      {
        type: [this.colorSettings?.type, []],
        color: [this.colorSettings?.color, []],
        rangeList: this.fb.array((this.colorSettings?.rangeList || []).map(r => this.colorRangeControl(r))),
        colorFunction: [this.colorSettings?.colorFunction, []]
      }
    );
    this.colorSettingsFormGroup.get('type').valueChanges.subscribe(() => {
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
  }

  private colorRangeControl(range: ColorRange): AbstractControl {
    return this.fb.group({
      from: [range?.from, []],
      to: [range?.to, []],
      color: [range?.color, []]
    });
  }

  get rangeListFormArray(): UntypedFormArray {
    return this.colorSettingsFormGroup.get('rangeList') as UntypedFormArray;
  }

  get rangeListFormGroups(): FormGroup[] {
    return this.rangeListFormArray.controls as FormGroup[];
  }

  trackByRange(index: number, rangeControl: AbstractControl): any {
    return rangeControl;
  }

  removeRange(index: number) {
    this.rangeListFormArray.removeAt(index);
    this.colorSettingsFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  addRange() {
    const newRange: ColorRange = {
      color: 'rgba(0,0,0,0.87)'
    };
    this.rangeListFormArray.push(this.colorRangeControl(newRange));
    this.colorSettingsFormGroup.markAsDirty();
    setTimeout(() => {this.popover?.updatePosition();}, 0);
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorSettings() {
    const colorSettings = this.colorSettingsFormGroup.value;
    this.colorSettingsApplied.emit(colorSettings);
  }

}
