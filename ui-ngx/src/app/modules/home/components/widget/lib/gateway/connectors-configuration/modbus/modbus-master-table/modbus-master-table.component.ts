///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, take, takeUntil } from 'rxjs/operators';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormGroup,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  ModbusMasterConfig,
  ModbusProtocolLabelsMap,
  SlaveConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isDefinedAndNotNull, isUndefinedOrNull } from '@core/utils';
import { SharedModule } from '@shared/shared.module';
import { CommonModule } from '@angular/common';
import { ModbusSlaveDialogComponent } from '../modbus-slave-dialog/modbus-slave-dialog.component';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';

@Component({
  selector: 'tb-modbus-master-table',
  templateUrl: './modbus-master-table.component.html',
  styleUrls: ['./modbus-master-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModbusMasterTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ModbusMasterTableComponent),
      multi: true
    }
  ],
  standalone: true,
  imports: [CommonModule, SharedModule]
})
export class ModbusMasterTableComponent implements ControlValueAccessor, Validator, AfterViewInit, OnInit, OnDestroy {

  @ViewChild('searchInput') searchInputField: ElementRef;

  textSearchMode = false;
  dataSource: SlavesDatasource;
  masterFormGroup: UntypedFormGroup;
  textSearch = this.fb.control('', {nonNullable: true});

  readonly ModbusProtocolLabelsMap = ModbusProtocolLabelsMap;

  private onChange: (value: ModbusMasterConfig) => void = () => {};
  private onTouched: () => void  = () => {};

  private destroy$ = new Subject<void>();

  constructor(
    public translate: TranslateService,
    public dialog: MatDialog,
    private dialogService: DialogService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {
    this.masterFormGroup =  this.fb.group({ slaves: this.fb.array([]) });
    this.dataSource = new SlavesDatasource();
  }

  get slaves(): FormArray {
    return this.masterFormGroup.get('slaves') as FormArray;
  }

  ngOnInit(): void {
    this.masterFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((value) => {
      this.updateTableData(value.slaves);
      this.onChange(value);
      this.onTouched();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    this.textSearch.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged((prev, current) => (prev ?? '') === current.trim()),
      takeUntil(this.destroy$)
    ).subscribe(text => this.updateTableData(this.slaves.value, text.trim()));
  }

  registerOnChange(fn: (value: ModbusMasterConfig) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  writeValue(master: ModbusMasterConfig): void {
    this.slaves.clear();
    this.pushDataAsFormArrays(master.slaves);
  }

  validate(): ValidationErrors | null {
    return this.slaves.controls.length ? null : {
      slavesFormGroup: {valid: false}
    };
  }

  enterFilterMode(): void {
    this.textSearchMode = true;
    this.cdr.detectChanges();
    const searchInput = this.searchInputField.nativeElement;
    searchInput.focus();
    searchInput.setSelectionRange(0, 0);
  }

  exitFilterMode(): void {
    this.updateTableData(this.slaves.value);
    this.textSearchMode = false;
    this.textSearch.reset();
  }

  manageSlave($event: Event, index?: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    const withIndex = isDefinedAndNotNull(index);
    const value = withIndex ? this.slaves.at(index).value : {};
    this.dialog.open<ModbusSlaveDialogComponent, any, any>(ModbusSlaveDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        value,
        buttonTitle: withIndex ? 'action.add' : 'action.apply'
      }
    }).afterClosed()
      .pipe(take(1), takeUntil(this.destroy$))
      .subscribe(res => {
        if (res) {
          if (withIndex) {
            this.slaves.at(index).patchValue(res);
          } else {
            this.slaves.push(this.fb.control(res));
          }
          this.masterFormGroup.markAsDirty();
        }
    });
  }

  deleteMapping($event: Event, index: number): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('gateway.delete-slave-title'),
      '',
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).pipe(take(1), takeUntil(this.destroy$)).subscribe((result) => {
      if (result) {
        this.slaves.removeAt(index);
        this.masterFormGroup.markAsDirty();
      }
    });
  }

  private updateTableData(data: SlaveConfig[], textSearch?: string): void {
    if (textSearch) {
      data = data.filter(item =>
        Object.values(item).some(value =>
          value.toString().toLowerCase().includes(textSearch.toLowerCase())
        )
      );
    }
    this.dataSource.loadData(data);
  }

  private pushDataAsFormArrays(slaves: SlaveConfig[]): void {
    if (slaves?.length) {
      slaves.forEach((slave: SlaveConfig) => this.slaves.push(this.fb.control(slave)));
    }
  }
}

export class SlavesDatasource extends TbTableDatasource<SlaveConfig> {
  constructor() {
    super();
  }
}
