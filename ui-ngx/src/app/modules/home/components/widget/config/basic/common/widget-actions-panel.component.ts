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

import { ChangeDetectorRef, Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup
} from '@angular/forms';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { WidgetActionsData } from '@home/components/widget/action/manage-widget-actions.component.models';
import { Datasource, WidgetActionDescriptor } from '@shared/models/widget.models';
import {
  ManageWidgetActionsDialogComponent,
  ManageWidgetActionsDialogData
} from '@home/components/widget/action/manage-widget-actions-dialog.component';
import { deepClone } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'tb-widget-actions-panel',
  templateUrl: './widget-actions-panel.component.html',
  styleUrls: ['../../widget-config.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WidgetActionsPanelComponent),
      multi: true
    }
  ]
})
export class WidgetActionsPanelComponent implements ControlValueAccessor, OnInit {

  @Input()
  disabled: boolean;

  actionsFormGroup: UntypedFormGroup;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef,
              private widgetConfigComponent: WidgetConfigComponent) {
  }

  ngOnInit() {
    this.actionsFormGroup = this.fb.group({
      actions: [null, []]
    });
    this.actionsFormGroup.get('actions').valueChanges.subscribe(
      (val) => this.propagateChange(val)
    );
  }

  writeValue(actions?: {[actionSourceId: string]: Array<WidgetActionDescriptor>}): void {
    this.actionsFormGroup.get('actions').patchValue(actions || {}, {emitEvent: false});
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.actionsFormGroup.disable({emitEvent: false});
    } else {
      this.actionsFormGroup.enable({emitEvent: false});
    }
  }

  public get widgetActionSourceIds(): Array<string> {
    const actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = this.actionsFormGroup.get('actions').value;
    return actions ? Object.keys(actions) : [];
  }

  public widgetActionsByActionSourceId(actionSourceId: string): Array<WidgetActionDescriptor> {
    const actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = this.actionsFormGroup.get('actions').value;
    return actions[actionSourceId] || [];
  }

  public get hasWidgetActions(): boolean {
    const actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = this.actionsFormGroup.get('actions').value;
    if (actions) {
      for (const actionSourceId of Object.keys(actions)) {
        if (actions[actionSourceId] && actions[actionSourceId].length) {
          return true;
        }
      }
    }
    return false;
  }

  public manageWidgetActions() {
    const actions: {[actionSourceId: string]: Array<WidgetActionDescriptor>} = this.actionsFormGroup.get('actions').value;
    const actionsData: WidgetActionsData = {
      actionsMap: deepClone(actions),
      actionSources: this.widgetConfigComponent.modelValue.actionSources || {}
    };
    this.dialog.open<ManageWidgetActionsDialogComponent, ManageWidgetActionsDialogData,
      {[actionSourceId: string]: Array<WidgetActionDescriptor>}>(ManageWidgetActionsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        widgetTitle: this.widgetConfigComponent.modelValue.widgetName,
        callbacks: this.widgetConfigComponent.widgetConfigCallbacks,
        actionsData,
        widgetType: this.widgetConfigComponent.widgetType
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.actionsFormGroup.get('actions').patchValue(res);
          this.cd.markForCheck();
        }
      }
    );
  }

}
