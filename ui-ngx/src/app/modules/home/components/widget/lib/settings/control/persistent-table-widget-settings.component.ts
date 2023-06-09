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

import { Component, ElementRef, ViewChild } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of, Subject } from 'rxjs';
import { TruncatePipe } from '@shared/pipe/truncate.pipe';
import { MatChipInputEvent, MatChipGrid } from '@angular/material/chips';
import { MatAutocomplete, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { map, mergeMap, share, startWith } from 'rxjs/operators';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';

interface DisplayColumn {
  name: string;
  value: string;
}

@Component({
  selector: 'tb-persistent-table-widget-settings',
  templateUrl: './persistent-table-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class PersistentTableWidgetSettingsComponent extends WidgetSettingsComponent {

  @ViewChild('columnsChipList') columnsChipList: MatChipGrid;
  @ViewChild('columnAutocomplete') columnAutocomplete: MatAutocomplete;
  @ViewChild('columnInput') columnInput: ElementRef<HTMLInputElement>;

  displayColumns: Array<DisplayColumn> = [
    {value: 'rpcId', name: this.translate.instant('widgets.persistent-table.rpc-id')},
    {value: 'messageType', name: this.translate.instant('widgets.persistent-table.message-type')},
    {value: 'status', name: this.translate.instant('widgets.persistent-table.status')},
    {value: 'method', name: this.translate.instant('widgets.persistent-table.method')},
    {value: 'createdTime', name: this.translate.instant('widgets.persistent-table.created-time')},
    {value: 'expirationTime', name: this.translate.instant('widgets.persistent-table.expiration-time')},
  ];

  separatorKeysCodes = [ENTER, COMMA, SEMICOLON];

  persistentTableWidgetSettingsForm: UntypedFormGroup;

  filteredDisplayColumns: Observable<Array<DisplayColumn>>;

  columnSearchText = '';

  columnInputChange = new Subject<string>();

  constructor(protected store: Store<AppState>,
              public translate: TranslateService,
              public truncate: TruncatePipe,
              private fb: UntypedFormBuilder) {
    super(store);
    this.filteredDisplayColumns = this.columnInputChange
      .pipe(
        startWith(''),
        map((value) => value ? value : ''),
        mergeMap(name => this.fetchColumns(name) ),
        share()
      );
  }

  protected settingsForm(): UntypedFormGroup {
    return this.persistentTableWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      enableFilter: true,

      enableStickyHeader: true,
      enableStickyAction: true,

      displayDetails: true,
      allowSendRequest: true,
      allowDelete: true,

      displayPagination: true,
      defaultPageSize: 10,

      defaultSortOrder: '-createdTime',
      displayColumns: ['rpcId', 'messageType', 'status', 'method', 'createdTime', 'expirationTime']
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.persistentTableWidgetSettingsForm = this.fb.group({
      enableFilter: [settings.enableFilter, []],
      enableStickyHeader: [settings.enableStickyHeader, []],
      enableStickyAction: [settings.enableStickyAction, []],
      allowSendRequest: [settings.allowSendRequest, []],
      allowDelete: [settings.allowDelete, []],
      displayDetails: [settings.displayDetails, []],
      displayPagination: [settings.displayPagination, []],
      defaultPageSize: [settings.defaultPageSize, [Validators.min(1)]],
      defaultSortOrder: [settings.defaultSortOrder, []],
      displayColumns: [settings.displayColumns, [Validators.required]]
    });
  }

  protected validateSettings(): boolean {
    const displayColumns: string[] = this.persistentTableWidgetSettingsForm.get('displayColumns').value;
    this.columnsChipList.errorState = !displayColumns?.length;
    return super.validateSettings();
  }

  protected validatorTriggers(): string[] {
    return ['displayPagination'];
  }

  protected updateValidators(emitEvent: boolean) {
    const displayPagination: boolean = this.persistentTableWidgetSettingsForm.get('displayPagination').value;
    if (displayPagination) {
      this.persistentTableWidgetSettingsForm.get('defaultPageSize').enable();
    } else {
      this.persistentTableWidgetSettingsForm.get('defaultPageSize').disable();
    }
    this.persistentTableWidgetSettingsForm.get('defaultPageSize').updateValueAndValidity({emitEvent});
  }

  private fetchColumns(searchText?: string): Observable<Array<DisplayColumn>> {
    this.columnSearchText = searchText;
    if (this.columnSearchText && this.columnSearchText.length) {
      const search = this.columnSearchText.toUpperCase();
      return of(this.displayColumns.filter(column => column.name.toUpperCase().includes(search)));
    } else {
      return of(this.displayColumns);
    }
  }

  private addColumn(existingColumn: DisplayColumn): boolean {
    if (existingColumn) {
      const displayColumns: string[] = this.persistentTableWidgetSettingsForm.get('displayColumns').value;
      const index = displayColumns.indexOf(existingColumn.value);
      if (index === -1) {
        displayColumns.push(existingColumn.value);
        this.persistentTableWidgetSettingsForm.get('displayColumns').setValue(displayColumns);
        this.persistentTableWidgetSettingsForm.get('displayColumns').markAsDirty();
        this.columnsChipList.errorState = false;
        return true;
      }
    }
    return false;
  }

  displayColumnFromValue(columnValue: string): DisplayColumn | undefined {
    return this.displayColumns.find((column) => column.value === columnValue);
  }

  displayColumnFn(column?: DisplayColumn): string | undefined {
    return column ? column.name : undefined;
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  columnDrop(event: CdkDragDrop<string[]>): void {
    const displayColumns: string[] = this.persistentTableWidgetSettingsForm.get('displayColumns').value;
    moveItemInArray(displayColumns, event.previousIndex, event.currentIndex);
    this.persistentTableWidgetSettingsForm.get('displayColumns').setValue(displayColumns);
    this.persistentTableWidgetSettingsForm.get('displayColumns').markAsDirty();
  }

  onColumnRemoved(column: string): void {
    const displayColumns: string[] = this.persistentTableWidgetSettingsForm.get('displayColumns').value;
    const index = displayColumns.indexOf(column);
    if (index > -1) {
      displayColumns.splice(index, 1);
      this.persistentTableWidgetSettingsForm.get('displayColumns').setValue(displayColumns);
      this.persistentTableWidgetSettingsForm.get('displayColumns').markAsDirty();
      this.columnsChipList.errorState = !displayColumns.length;
    }
  }

  onColumnInputFocus() {
    this.columnInputChange.next(this.columnInput.nativeElement.value);
  }

  addColumnFromChipInput(event: MatChipInputEvent): void {
    const value = event.value;
    if ((value || '').trim()) {
      const columnName = value.trim().toUpperCase();
      const existingColumn = this.displayColumns.find(column => column.name.toUpperCase() === columnName);
      if (this.addColumn(existingColumn)) {
        this.clearColumnInput('');
      }
    }
  }

  columnSelected(event: MatAutocompleteSelectedEvent): void {
    this.addColumn(event.option.value);
    this.clearColumnInput('');
  }

  clearColumnInput(value: string = '') {
    this.columnInput.nativeElement.value = value;
    this.columnInputChange.next(null);
    setTimeout(() => {
      this.columnInput.nativeElement.blur();
      this.columnInput.nativeElement.focus();
    }, 0);
  }
}
