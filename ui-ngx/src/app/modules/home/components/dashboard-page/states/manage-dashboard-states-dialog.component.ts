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

import { AfterViewInit, Component, ElementRef, Inject, OnInit, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroupDirective, NgForm, UntypedFormBuilder, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { DashboardState } from '@app/shared/models/dashboard.models';
import { PageLink } from '@shared/models/page/page-link';
import {
  DashboardStateInfo,
  DashboardStatesDatasource
} from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component.models';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { DialogService } from '@core/services/dialog.service';
import { deepClone, isDefined } from '@core/utils';
import {
  DashboardStateDialogComponent,
  DashboardStateDialogData
} from '@home/components/dashboard-page/states/dashboard-state-dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { Widget } from '@shared/models/widget.models';

export interface ManageDashboardStatesDialogData {
  states: {[id: string]: DashboardState };
  widgets: {[id: string]: Widget };
}

@Component({
  selector: 'tb-manage-dashboard-states-dialog',
  templateUrl: './manage-dashboard-states-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ManageDashboardStatesDialogComponent}],
  styleUrls: ['./manage-dashboard-states-dialog.component.scss']
})
export class ManageDashboardStatesDialogComponent
  extends DialogComponent<ManageDashboardStatesDialogComponent, {states: {[id: string]: DashboardState}; widgets: {[id: string]: Widget}}>
  implements OnInit, ErrorStateMatcher, AfterViewInit {

  statesFormGroup: UntypedFormGroup;

  states: {[id: string]: DashboardState };
  widgets: {[id: string]: Widget};

  displayedColumns: string[];
  pageLink: PageLink;
  textSearchMode = false;
  dataSource: DashboardStatesDatasource;

  submitted = false;

  stateNames: Set<string> = new Set<string>();

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ManageDashboardStatesDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ManageDashboardStatesDialogComponent,
                {states: {[id: string]: DashboardState}; widgets: {[id: string]: Widget}}>,
              private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private dialogs: DialogService,
              private utils: UtilsService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);

    this.states = this.data.states;
    this.widgets = this.data.widgets;
    this.statesFormGroup = this.fb.group({});
    Object.values(this.states).forEach(value => this.stateNames.add(value.name));

    const sortOrder: SortOrder = { property: 'name', direction: Direction.ASC };
    this.pageLink = new PageLink(5, 0, null, sortOrder);
    this.displayedColumns = ['name', 'id', 'root', 'actions'];
    this.dataSource = new DashboardStatesDatasource(this.states);
  }

  ngOnInit(): void {
    this.dataSource.loadStates(this.pageLink);
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
  }

  updateData(reload: boolean = false) {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadStates(this.pageLink, reload);
  }

  addState($event: Event) {
    this.openStateDialog($event);
  }

  editState($event: Event, state: DashboardStateInfo) {
    this.openStateDialog($event, state);
  }

  deleteState($event: Event, state: DashboardStateInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('dashboard.delete-state-title');
    const content = this.translate.instant('dashboard.delete-state-text', {stateName: state.name});
    this.dialogs.confirm(title, content, this.translate.instant('action.no'),
      this.translate.instant('action.yes')).subscribe(
        (res) => {
          if (res) {
            this.stateNames.delete(state.name);
            delete this.states[state.id];
            this.onStatesUpdated();
          }
        }
    );
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

  openStateDialog($event: Event, state: DashboardStateInfo = null) {
    if ($event) {
      $event.stopPropagation();
    }
    const isAdd = state === null;
    let prevStateId = null;
    let prevStateName = '';
    if (!isAdd) {
      prevStateId = state.id;
      prevStateName = state.name;
    }
    this.dialog.open<DashboardStateDialogComponent, DashboardStateDialogData,
      DashboardStateInfo>(DashboardStateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        states: this.states,
        state: deepClone(state)
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.saveState(res, prevStateId, prevStateName);
        }
      }
    );
  }

  saveState(state: DashboardStateInfo, prevStateId: string, prevStateName: string) {
    const newState: DashboardState = {
      name: state.name,
      root: state.root,
      layouts: state.layouts
    };
    if (prevStateId && prevStateId !== state.id) {
      delete this.states[prevStateId];
      this.states[state.id] = newState;
    } else {
      this.states[state.id] = newState;
    }
    if (prevStateName && prevStateName !== state.name) {
      this.stateNames.delete(prevStateName);
      this.stateNames.add(state.name);
    }
    if (state.root) {
      for (const id of Object.keys(this.states)) {
        const otherState = this.states[id];
        if (id !== state.id) {
          otherState.root = false;
        }
      }
    } else {
      let rootFound = false;
      for (const id of Object.keys(this.states)) {
        const otherState = this.states[id];
        if (otherState.root) {
          rootFound = true;
          break;
        }
      }
      if (!rootFound) {
        const firstStateId = Object.keys(this.states)[0];
        this.states[firstStateId].root = true;
      }
    }
    this.onStatesUpdated();
  }

  duplicateState($event: Event, state: DashboardStateInfo) {
    const originalState = state;
    const newStateName = this.getNextDuplicatedName(state.name);
    if (newStateName) {
      const duplicatedStates = deepClone(originalState);
      const duplicatedWidgets = deepClone(this.widgets);
      const mainWidgets = {};
      const rightWidgets = {};
      duplicatedStates.id = newStateName.toLowerCase().replace(/\W/g, '_');
      duplicatedStates.name = newStateName;
      duplicatedStates.root = false;
      this.stateNames.add(duplicatedStates.name);

      for (const [key, value] of Object.entries(duplicatedStates.layouts.main.widgets)) {
        const guid = this.utils.guid();
        mainWidgets[guid] = value;
        duplicatedWidgets[guid] = this.widgets[key];
        duplicatedWidgets[guid].id = guid;
      }
      duplicatedStates.layouts.main.widgets = mainWidgets;

      if (isDefined(duplicatedStates.layouts?.right)) {
        for (const [key, value] of Object.entries(duplicatedStates.layouts.right.widgets)) {
          const guid = this.utils.guid();
          rightWidgets[guid] = value;
          duplicatedWidgets[guid] = this.widgets[key];
          duplicatedWidgets[guid].id = guid;
        }
        duplicatedStates.layouts.right.widgets = rightWidgets;
      }

      this.states[duplicatedStates.id] = duplicatedStates;
      this.widgets = duplicatedWidgets;
      this.onStatesUpdated();
    }
  }

  private getNextDuplicatedName(stateName: string): string {
    const suffix = ` - ${this.translate.instant('action.copy')} `;
    let counter = 0;
    while (++counter < Number.MAX_SAFE_INTEGER) {
      const newName = `${stateName}${suffix}${counter}`;
      if (!this.stateNames.has(newName)) {
        return newName;
      }
    }

    return null;
  }

  private onStatesUpdated() {
    this.statesFormGroup.markAsDirty();
    this.updateData(true);
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.dialogRef.close({ states: this.states, widgets: this.widgets });
  }
}
