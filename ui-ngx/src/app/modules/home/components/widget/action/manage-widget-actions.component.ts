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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, first, tap } from 'rxjs/operators';
import {
  toWidgetActionDescriptor,
  WidgetActionCallbacks,
  WidgetActionDescriptorInfo,
  WidgetActionsData,
  WidgetActionsDatasource
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { UtilsService } from '@core/services/utils.service';
import { WidgetActionDescriptor, WidgetActionSource, WidgetActionType, widgetType } from '@shared/models/widget.models';
import {
  WidgetActionDialogComponent,
  WidgetActionDialogData
} from '@home/components/widget/action/widget-action-dialog.component';
import { deepClone } from '@core/utils';
import { ResizeObserver } from '@juggle/resize-observer';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';

@Component({
  selector: 'tb-manage-widget-actions',
  templateUrl: './manage-widget-actions.component.html',
  styleUrls: ['./manage-widget-actions.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ManageWidgetActionsComponent),
      multi: true
    }
  ]
})
export class ManageWidgetActionsComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input() disabled: boolean;

  @Input() widgetType: widgetType;

  @Input() callbacks: WidgetActionCallbacks;

  @Input() actionTypes: WidgetActionType[];

  @Input() customFunctionArgs: string[];

  innerValue: WidgetActionsData;

  displayedColumns: string[];
  pageLink: PageLink;
  textSearchMode = false;
  hidePageSize = false;
  dataSource: WidgetActionsDatasource;

  viewsInited = false;
  dirtyValue = false;
  dragDisabled = true;

  private widgetResize$: ResizeObserver;

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  private propagateChange = (_: any) => {};

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private dialogs: DialogService,
              private cd: ChangeDetectorRef,
              private elementRef: ElementRef) {
    super(store);
    const sortOrder: SortOrder = { property: 'actionSourceName', direction: Direction.ASC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new WidgetActionsDatasource(this.translate, this.utils);
    this.displayedColumns = ['actionSourceId', 'actionSourceName', 'name', 'icon', 'typeName', 'actions'];
  }

  ngOnInit(): void {
    this.widgetResize$ = new ResizeObserver(() => {
      const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
      if (showHidePageSize !== this.hidePageSize) {
        this.hidePageSize = showHidePageSize;
        this.cd.markForCheck();
      }
    });
    this.widgetResize$.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
  }

  ngAfterViewInit() {

    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.paginator.pageIndex = 0;
          this.updateData();
        })
      )
      .subscribe();

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();

    this.viewsInited = true;
    if (this.dirtyValue) {
      this.dirtyValue = false;
      this.updateData(true);
    }

  }

  updateData(reload: boolean = false) {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadActions(this.pageLink, reload);
  }

  dropAction(event: CdkDragDrop<WidgetActionsDatasource>) {
    this.dragDisabled = true;
    const droppedAction: WidgetActionDescriptorInfo = event.item.data;
    this.dataSource.pageData$.pipe(
      first()
    ).subscribe((actions) => {
      const action = actions.data;
      let startActionSourceIndex = action.findIndex(element => element.actionSourceId === droppedAction.actionSourceId);
      const targetActions = this.getOrCreateTargetActions(droppedAction.actionSourceId);
      if (startActionSourceIndex === 0) {
        startActionSourceIndex -= targetActions.findIndex(element => element.id === action[0].id);
      }
      moveItemInArray(targetActions, event.previousIndex - startActionSourceIndex, event.currentIndex - startActionSourceIndex);
      this.onActionsUpdated();
    });
  }

  addAction($event: Event) {
    this.openWidgetActionDialog($event);
  }

  editAction($event: Event, action: WidgetActionDescriptorInfo) {
    this.openWidgetActionDialog($event, action);
  }

  openWidgetActionDialog($event: Event, action: WidgetActionDescriptorInfo = null) {
    if ($event) {
      $event.stopPropagation();
    }
    const isAdd = action === null;
    let prevActionSourceId = null;
    if (!isAdd) {
      prevActionSourceId = action.actionSourceId;
    }
    const availableActionSources: {[actionSourceId: string]: WidgetActionSource} = {};
    for (const id of Object.keys(this.innerValue.actionSources)) {
      const actionSource = this.innerValue.actionSources[id];
      if (actionSource.multiple) {
        availableActionSources[id] = actionSource;
      } else {
        if (!isAdd && action.actionSourceId === id) {
          availableActionSources[id] = actionSource;
        } else {
          const existing = this.innerValue.actionsMap[id];
          if (!existing || !existing.length) {
            availableActionSources[id] = actionSource;
          }
        }
      }
    }

    const actionsData: WidgetActionsData = {
      actionsMap: this.innerValue.actionsMap,
      actionSources: availableActionSources
    };

    this.dialog.open<WidgetActionDialogComponent, WidgetActionDialogData,
      WidgetActionDescriptorInfo>(WidgetActionDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        callbacks: this.callbacks,
        actionsData,
        action: deepClone(action),
        actionTypes: this.actionTypes,
        customFunctionArgs: this.customFunctionArgs,
        widgetType: this.widgetType
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.saveAction(res, isAdd, prevActionSourceId);
        }
      }
    );
  }

  private saveAction(actionInfo: WidgetActionDescriptorInfo, isAdd: boolean, prevActionSourceId: string) {
    const actionSourceId = actionInfo.actionSourceId;
    const action = toWidgetActionDescriptor(actionInfo);
    if (isAdd) {
      const targetActions = this.getOrCreateTargetActions(actionSourceId);
      targetActions.push(action);
    } else {
      if (actionSourceId !== prevActionSourceId) {
        let targetActions = this.getOrCreateTargetActions(prevActionSourceId);
        const targetIndex = targetActions.findIndex((targetAction) => targetAction.id === action.id);
        if (targetIndex > -1) {
          targetActions.splice(targetIndex, 1);
        }
        targetActions = this.getOrCreateTargetActions(actionSourceId);
        targetActions.push(action);
      } else {
        const targetActions = this.getOrCreateTargetActions(actionSourceId);
        const targetIndex = targetActions.findIndex((targetAction) => targetAction.id === action.id);
        if (targetIndex > -1) {
          targetActions[targetIndex] = action;
        }
      }
    }
    this.onActionsUpdated();
  }

  private getOrCreateTargetActions(actionSourceId: string): Array<WidgetActionDescriptor> {
    const actionsMap = this.innerValue.actionsMap;
    let targetActions = actionsMap[actionSourceId];
    if (!targetActions) {
      targetActions = [];
      actionsMap[actionSourceId] = targetActions;
    }
    return targetActions;
  }

  deleteAction($event: Event, action: WidgetActionDescriptorInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('widget-config.delete-action-title');
    const content = this.translate.instant('widget-config.delete-action-text', {actionName: action.name});
    this.dialogs.confirm(title, content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'), true).subscribe(
      (res) => {
        if (res) {
          const targetActions = this.getOrCreateTargetActions(action.actionSourceId);
          const targetIndex = targetActions.findIndex((targetAction) => targetAction.id === action.id);
          if (targetIndex > -1) {
            targetActions.splice(targetIndex, 1);
            this.onActionsUpdated();
          }
        }
      });
  }

  enterFilterMode() {
    this.textSearchMode = true;
    this.pageLink.textSearch = '';
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    this.updateData();
  }

  resetSortAndFilter(update: boolean = true) {
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    const sortable = this.sort.sortables.get('actionSourceName');
    this.sort.active = sortable.id;
    this.sort.direction = 'asc';
    if (update) {
      this.updateData(true);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(obj: WidgetActionsData): void {
    this.innerValue = obj;
    if (this.innerValue) {
      setTimeout(() => {
        this.dataSource.setActions(this.innerValue);
        if (this.viewsInited) {
          this.resetSortAndFilter(true);
        } else {
          this.dirtyValue = true;
        }
      }, 0);
    }
  }

  private onActionsUpdated() {
    this.updateData(true);
    this.propagateChange(this.innerValue);
  }
}
