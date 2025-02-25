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

import { Component, Inject, OnDestroy, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NgForm,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import {
  toWidgetActionDescriptor,
  WidgetActionCallbacks,
  WidgetActionDescriptorInfo,
  WidgetActionsData
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { UtilsService } from '@core/services/utils.service';
import {
  actionDescriptorToAction,
  CellClickColumnInfo,
  defaultWidgetAction,
  WidgetActionSource,
  WidgetActionType,
  widgetType
} from '@shared/models/widget.models';
import { takeUntil } from 'rxjs/operators';
import { CustomActionEditorCompleter } from '@home/components/widget/lib/settings/common/action/custom-action.models';
import { WidgetService } from '@core/http/widget.service';
import { isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { MatSelect } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';

export interface WidgetActionDialogData {
  isAdd: boolean;
  callbacks: WidgetActionCallbacks;
  actionsData: WidgetActionsData;
  actionTypes: WidgetActionType[];
  customFunctionArgs: string[];
  action?: WidgetActionDescriptorInfo;
  widgetType: widgetType;
  isEntityGroup?: boolean;
}

@Component({
  selector: 'tb-widget-action-dialog',
  templateUrl: './widget-action-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: WidgetActionDialogComponent}],
  styleUrls: []
})
export class WidgetActionDialogComponent extends DialogComponent<WidgetActionDialogComponent,
                                                 WidgetActionDescriptorInfo> implements OnInit, OnDestroy, ErrorStateMatcher {

  private destroy$ = new Subject<void>();

  widgetActionFormGroup: FormGroup;

  isAdd: boolean;
  action: WidgetActionDescriptorInfo;

  customFunctionArgs = this.data.customFunctionArgs;
  widgetActionTypes = this.data.actionTypes;

  customActionEditorCompleter = CustomActionEditorCompleter;

  submitted = false;

  functionScopeVariables: string[];

  private isEntityGroup = this.data.isEntityGroup;

  configuredColumns: Array<CellClickColumnInfo> = [];
  usedCellClickColumns: Array<number> = [];

  @ViewChild('columnIndexSelect') columnIndexSelect: MatSelect;
  columnIndexPlaceholderText = this.translate.instant('widget-config.select-column-index');

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private utils: UtilsService,
              private widgetService: WidgetService,
              @Inject(MAT_DIALOG_DATA) public data: WidgetActionDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<WidgetActionDialogComponent, WidgetActionDescriptorInfo>,
              public fb: FormBuilder,
              private translate: TranslateService) {
    super(store, router, dialogRef);
    this.isAdd = data.isAdd;
    if (this.isAdd) {
      this.action = {
        id: this.utils.guid(),
        name: '',
        icon: 'more_horiz',
        ...defaultWidgetAction(this.isEntityGroup, data.widgetType !== widgetType.static)
      };
    } else {
      this.action = this.data.action;
    }
    this.functionScopeVariables = this.widgetService.getWidgetScopeVariables();
    if (this.action.actionSourceId === 'cellClick') {
      this.getCellClickColumnsInfo();
    }
  }

  ngOnInit(): void {
    this.widgetActionFormGroup = this.fb.group({
      actionSourceId: [this.action.actionSourceId, Validators.required],
      columnIndex: [{value: this.checkColumnIndex(this.action.columnIndex), disabled: true}, Validators.required],
      name: [this.action.name, [this.validateActionName(), Validators.required]],
      icon: [this.action.icon, Validators.required],
      useShowWidgetActionFunction: [this.action.useShowWidgetActionFunction],
      showWidgetActionFunction: [this.action.showWidgetActionFunction || 'return true;'],
      widgetAction: [actionDescriptorToAction(toWidgetActionDescriptor(this.action)), Validators.required]
    });
    this.updateShowWidgetActionForm();
    this.widgetActionFormGroup.get('actionSourceId').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.widgetActionFormGroup.get('name').updateValueAndValidity();
      this.updateShowWidgetActionForm();
      if (value === 'cellClick') {
        this.widgetActionFormGroup.get('columnIndex').enable();
        this.getCellClickColumnsInfo();
      } else {
        this.widgetActionFormGroup.get('columnIndex').disable();
      }
    });
    this.widgetActionFormGroup.get('useShowWidgetActionFunction').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateShowWidgetActionForm();
    });
    setTimeout(() => {
      if (this.action?.actionSourceId === 'cellClick') {
        this.widgetActionFormGroup.get('columnIndex').enable();
        if (isDefinedAndNotNull(this.action.columnIndex) && this.widgetActionFormGroup.get('columnIndex').value === null) {
          this.columnIndexPlaceholderText = `${this.action.columnIndex} (${this.translate.instant('widget-config.not-set')})`;
          this.columnIndexSelect.focus();
        }
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    super.ngOnDestroy();
  }

  displayShowWidgetActionForm(): boolean {
    return !!this.data.actionsData.actionSources[this.widgetActionFormGroup.get('actionSourceId').value]?.hasShowCondition;
  }

  customFunctionHelpId(): string {
      return this.isEntityGroup ? 'entity_group/action/custom_action_fn' : 'widget/action/custom_action_fn';
  }

  entityGroupRowClickHint(): string {
    return this.isEntityGroup && this.widgetActionFormGroup.get('actionSourceId').value === 'rowClick' ?
      this.translate.instant('widget-config.entity-group-row-click-hint') : '';
  }

  getWidgetActionFunctionHelpId(): string | undefined {
    const actionSourceId = this.widgetActionFormGroup.get('actionSourceId').value;
    if (actionSourceId === 'headerButton') {
      return 'widget/action/show_widget_action_header_fn';
    } else if (actionSourceId === 'actionCellButton') {
      return 'widget/action/show_widget_action_cell_fn';
    }
    return undefined;
  }

  private updateShowWidgetActionForm() {
    const actionSourceId = this.widgetActionFormGroup.get('actionSourceId').value;
    const useShowWidgetActionFunction = this.widgetActionFormGroup.get('useShowWidgetActionFunction').value;
    if (!!this.data.actionsData.actionSources[actionSourceId]?.hasShowCondition && useShowWidgetActionFunction) {
      this.widgetActionFormGroup.get('showWidgetActionFunction').setValidators([Validators.required]);
    } else {
      this.widgetActionFormGroup.get('showWidgetActionFunction').clearValidators();
    }
    this.widgetActionFormGroup.get('showWidgetActionFunction').updateValueAndValidity();
  }

  private checkColumnIndex(columnIndex: number): number | null {
    return isDefinedAndNotNull(columnIndex) && this.configuredColumns.length - 1 < columnIndex ? null : columnIndex;
  }

  private getCellClickColumnsInfo(): void {
    if (!this.configuredColumns.length) {
      this.configuredColumns = this.data.callbacks.fetchCellClickColumns();
      this.data.actionsData.actionsMap['cellClick']?.forEach(action => {
        const actionColumn = this.configuredColumns[action.columnIndex];
        if (actionColumn && action.columnIndex !== this.action.columnIndex) {
          this.usedCellClickColumns.push(action.columnIndex);
        }
      });
    }
  }

  private validateActionName(): ValidatorFn {
    return (c: FormControl) => {
      const newName = c.value;
      const valid = this.checkActionName(newName, c.parent?.get('actionSourceId').value);
      return !valid ? {
        actionNameNotUnique: true
      } : null;
    };
  }

  private checkActionName(name: string, actionSourceId: string): boolean {
    let actionNameIsUnique = true;
    if (name && actionSourceId) {
      const sourceActions = this.data.actionsData.actionsMap[actionSourceId];
      if (sourceActions) {
        const result = sourceActions.filter((sourceAction) => sourceAction.name === name);
        if (result && result.length && result[0].id !== this.action.id) {
          actionNameIsUnique = false;
        }
      }
    }
    return actionNameIsUnique;
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  public actionSourceName(actionSource: WidgetActionSource): string {
    if (actionSource) {
      return this.utils.customTranslation(actionSource.name, actionSource.name);
    } else {
      return '';
    }
  }

  public getCellClickColumnInfo(index: number, columnInfo: CellClickColumnInfo): string {
    return `${index} (${isNotEmptyStr(columnInfo.label) ? columnInfo.label : columnInfo.name})`;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.widgetActionFormGroup.valid) {
      const result: WidgetActionDescriptorInfo =
        {...this.widgetActionFormGroup.value, ...this.widgetActionFormGroup.get('widgetAction').value};
      delete (result as any).widgetAction;
      result.id = this.action.id;
      this.dialogRef.close(result);
    }
  }
}
