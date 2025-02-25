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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { merge } from 'rxjs';
import {
  DataToValueType,
  GetValueAction,
  getValueActions,
  getValueActionTranslations,
  GetValueSettings
} from '@shared/models/action-widget-settings.models';
import { ValueType } from '@shared/models/constants';
import { TargetDevice, WidgetAction, widgetType } from '@shared/models/widget.models';
import { AttributeScope, DataKeyType, telemetryTypeTranslationsShort } from '@shared/models/telemetry/telemetry.models';
import { IAliasController } from '@core/api/widget-api.models';
import { WidgetService } from '@core/http/widget.service';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';

@Component({
  selector: 'tb-widget-action-settings-panel',
  templateUrl: './widget-action-settings-panel.component.html',
  providers: [],
  styleUrls: ['./action-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class WidgetActionSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  widgetAction: WidgetAction;

  @Input()
  panelTitle: string;

  @Input()
  widgetType: widgetType;

  @Input()
  callbacks: WidgetActionCallbacks;

  @Input()
  popover: TbPopoverComponent<WidgetActionSettingsPanelComponent>;

  @Output()
  widgetActionApplied = new EventEmitter<WidgetAction>();

  widgetActionFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.widgetActionFormGroup = this.fb.group(
      {
        widgetAction: [this.widgetAction, []]
      }
    );
  }

  cancel() {
    this.popover?.hide();
  }

  applyWidgetAction() {
    const widgetAction: WidgetAction = this.widgetActionFormGroup.get('widgetAction').getRawValue();
    this.widgetActionApplied.emit(widgetAction);
  }
}
