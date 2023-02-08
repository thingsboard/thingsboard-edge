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

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { DAY, getDefaultTimezone, historyInterval } from '@shared/models/time/time.models';
import { ReportConfig, reportTypeNamesMap, reportTypes } from '@shared/models/report.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { UtilsService } from '@core/services/utils.service';
import { EntityType } from '@shared/models/entity-type.models';
import { MatDialog } from '@angular/material/dialog';
import {
  SelectDashboardStateDialogComponent,
  SelectDashboardStateDialogData
} from '@home/components/scheduler/config/select-dashboard-state-dialog.component';
import { PageComponent } from '@shared/components/page.component';
import { ReportService } from '@core/http/report.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';

@Component({
  selector: 'tb-report-config',
  templateUrl: './report-config.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ReportConfigComponent),
    multi: true
  }]
})
export class ReportConfigComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: ReportConfig | null;

  reportConfigFormGroup: UntypedFormGroup;

  @Input()
  reportsServerEndpointUrl: string;

  @Input()
  disabled: boolean;

  authUser = getCurrentAuthUser(this.store);

  isTenantAdmin = this.authUser.authority === Authority.TENANT_ADMIN;

  entityType = EntityType;

  reportTypesList = reportTypes;

  reportTypeNames = reportTypeNamesMap;

  private propagateChange = (v: any) => { };

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private reportService: ReportService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private fb: UntypedFormBuilder) {
    super(store);
    this.reportConfigFormGroup = this.fb.group({
      baseUrl: [null, [Validators.required]],
      dashboardId: [null, [Validators.required]],
      state: [null, []],
      timezone: [null, [Validators.required]],
      useDashboardTimewindow: [true, []],
      timewindow: [null, [Validators.required]],
      namePattern: [null, [Validators.required]],
      type: [null, [Validators.required]],
      useCurrentUserCredentials: [true, []],
      userId: [null, [Validators.required]],
    });

    this.reportConfigFormGroup.get('useDashboardTimewindow').valueChanges.subscribe(() => {
      this.updateEnabledState();
    });

    this.reportConfigFormGroup.get('useCurrentUserCredentials').valueChanges.subscribe((useCurrentUserCredentials: boolean) => {
      if (useCurrentUserCredentials) {
        this.reportConfigFormGroup.get('userId').patchValue(this.authUser.userId, {emitEvent: false});
      } else {
        this.reportConfigFormGroup.get('userId').patchValue(null, {emitEvent: false});
      }
      this.updateEnabledState();
    });

    this.reportConfigFormGroup.get('dashboardId').valueChanges.subscribe(() => {
      this.reportConfigFormGroup.get('state').patchValue('', {emitEvent: false});
    });

    this.reportConfigFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  private updateEnabledState() {
    if (this.disabled) {
      this.reportConfigFormGroup.disable({emitEvent: false});
    } else {
      this.reportConfigFormGroup.enable({emitEvent: false});
      const useDashboardTimewindow: boolean = this.reportConfigFormGroup.get('useDashboardTimewindow').value;
      const useCurrentUserCredentials: boolean = this.reportConfigFormGroup.get('useCurrentUserCredentials').value;
      if (useDashboardTimewindow) {
        this.reportConfigFormGroup.get('timewindow').disable({emitEvent: false});
      } else {
        this.reportConfigFormGroup.get('timewindow').enable({emitEvent: false});
      }
      if (useCurrentUserCredentials) {
        this.reportConfigFormGroup.get('userId').disable({emitEvent: false});
      } else {
        this.reportConfigFormGroup.get('userId').enable({emitEvent: false});
      }
    }
  }

  selectDashboardState() {
    this.dialog.open<SelectDashboardStateDialogComponent, SelectDashboardStateDialogData, string>(SelectDashboardStateDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
      data: {
        dashboardId: this.reportConfigFormGroup.get('dashboardId').value,
        state: this.reportConfigFormGroup.get('state').value
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res !== null) {
          this.reportConfigFormGroup.get('state').patchValue(res, {emitEvent: true});
        }
      }
    );
  }

  generateTestReport() {
    const progressText = this.translate.instant('dashboard.download-dashboard-progress', {reportType: this.modelValue.type});
    this.dialogService.progress(
      this.reportService.downloadTestReport(this.modelValue, this.reportsServerEndpointUrl), progressText).subscribe();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateEnabledState();
  }

  writeValue(value: ReportConfig | null): void {
    this.modelValue = value;
    if (!this.modelValue) {
      this.modelValue = this.createDefaultReportConfig();
      this.reportConfigFormGroup.reset(this.modelValue, {emitEvent: false});
      this.updateEnabledState();
      setTimeout(() => {
        this.updateModel();
      }, 0);
    } else {
      this.reportConfigFormGroup.reset(this.modelValue, {emitEvent: false});
      this.updateEnabledState();
    }
  }

  private createDefaultReportConfig(): ReportConfig {
    return {
      baseUrl: this.utils.baseUrl(),
      useDashboardTimewindow: true,
      timewindow: historyInterval(DAY),
      namePattern: 'report-%d{yyyy-MM-dd_HH:mm:ss}',
      type: 'pdf',
      timezone: getDefaultTimezone(),
      useCurrentUserCredentials: true,
      userId: this.authUser.userId,
      dashboardId: null,
      state: ''
    };
  }

  private updateModel() {
    if (this.reportConfigFormGroup.valid) {
      const value = this.reportConfigFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
