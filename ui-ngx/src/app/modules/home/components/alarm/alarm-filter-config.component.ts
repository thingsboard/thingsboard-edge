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
  Component,
  ElementRef,
  forwardRef,
  Inject,
  InjectionToken,
  Input,
  OnDestroy,
  OnInit,
  Optional,
  TemplateRef,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { AlarmFilterConfig } from '@shared/models/query/query.models';
import { coerceBoolean } from '@shared/decorators/coerce-boolean';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { TemplatePortal } from '@angular/cdk/portal';
import {
  AlarmSearchStatus,
  alarmSearchStatusTranslations,
  AlarmSeverity,
  alarmSeverityTranslations
} from '@shared/models/alarm.models';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { TranslateService } from '@ngx-translate/core';

export const ALARM_FILTER_CONFIG_DATA = new InjectionToken<any>('AlarmFilterConfigData');

export interface AlarmFilterConfigData {
  panelMode: boolean;
  userMode: boolean;
  alarmFilterConfig: AlarmFilterConfig;
}

// @dynamic
@Component({
  selector: 'tb-alarm-filter-config',
  templateUrl: './alarm-filter-config.component.html',
  styleUrls: ['./alarm-filter-config.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmFilterConfigComponent),
      multi: true
    }
  ]
})
export class AlarmFilterConfigComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @ViewChild('alarmFilterPanel')
  alarmFilterPanel: TemplateRef<any>;

  @Input() disabled: boolean;

  @coerceBoolean()
  @Input()
  buttonMode = true;

  @coerceBoolean()
  @Input()
  userMode = false;

  @coerceBoolean()
  @Input()
  propagatedFilter = true;

  panelMode = false;

  readonly separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  alarmSearchStatuses = [AlarmSearchStatus.ACTIVE,
    AlarmSearchStatus.CLEARED,
    AlarmSearchStatus.ACK,
    AlarmSearchStatus.UNACK];

  alarmSearchStatusTranslationMap = alarmSearchStatusTranslations;

  alarmSeverities = Object.keys(AlarmSeverity);
  alarmSeverityEnum = AlarmSeverity;

  alarmSeverityTranslationMap = alarmSeverityTranslations;

  buttonDisplayValue = this.translate.instant('alarm.alarm-filter');

  alarmFilterConfigForm: UntypedFormGroup;

  alarmFilterOverlayRef: OverlayRef;

  panelResult: AlarmFilterConfig = null;

  private alarmFilterConfig: AlarmFilterConfig;

  private propagateChange = (_: any) => {};

  constructor(@Optional() @Inject(ALARM_FILTER_CONFIG_DATA)
              private data: AlarmFilterConfigData | undefined,
              @Optional()
              private overlayRef: OverlayRef,
              private fb: UntypedFormBuilder,
              private translate: TranslateService,
              private overlay: Overlay,
              private nativeElement: ElementRef,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit(): void {
    if (this.data) {
      this.panelMode = this.data.panelMode;
      this.userMode = this.data.userMode;
      this.alarmFilterConfig = this.data.alarmFilterConfig;
    }
    this.alarmFilterConfigForm = this.fb.group({
      statusList: [null, []],
      severityList: [null, []],
      typeList: [null, []],
      searchPropagatedAlarms: [false, []],
      assignedToCurrentUser: [false, []],
      assigneeId: [null, []]
    });
    this.alarmFilterConfigForm.valueChanges.subscribe(
      () => {
        this.updateValidators();
        if (!this.buttonMode) {
          this.alarmConfigUpdated(this.alarmFilterConfigForm.value);
        }
      }
    );
    if (this.panelMode) {
      this.updateAlarmConfigForm(this.alarmFilterConfig);
    }
  }

  ngOnDestroy(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.alarmFilterConfigForm.disable({emitEvent: false});
    } else {
      this.alarmFilterConfigForm.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(alarmFilterConfig?: AlarmFilterConfig): void {
    this.alarmFilterConfig = alarmFilterConfig;
    this.updateButtonDisplayValue();
    this.updateAlarmConfigForm(alarmFilterConfig);
  }

  private updateValidators() {
    const assignedToCurrentUser = this.alarmFilterConfigForm.get('assignedToCurrentUser').value;
    if (assignedToCurrentUser) {
      this.alarmFilterConfigForm.get('assigneeId').disable({emitEvent: false});
    } else {
      this.alarmFilterConfigForm.get('assigneeId').enable({emitEvent: false});
    }
    this.alarmFilterConfigForm.get('assigneeId').updateValueAndValidity({emitEvent: false});
  }

  toggleAlarmFilterPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const config = new OverlayConfig({
      panelClass: 'tb-alarm-filter-panel',
      backdropClass: 'cdk-overlay-transparent-backdrop',
      hasBackdrop: true,
      maxHeight: '80vh',
      height: 'min-content',
      minWidth: ''
    });
    config.hasBackdrop = true;
    const connectedPosition: ConnectedPosition = {
      originX: 'start',
      originY: 'bottom',
      overlayX: 'start',
      overlayY: 'top'
    };
    config.positionStrategy = this.overlay.position().flexibleConnectedTo(this.nativeElement)
      .withPositions([connectedPosition]);

    this.alarmFilterOverlayRef = this.overlay.create(config);
    this.alarmFilterOverlayRef.backdropClick().subscribe(() => {
      this.alarmFilterOverlayRef.dispose();
    });
    this.alarmFilterOverlayRef.attach(new TemplatePortal(this.alarmFilterPanel,
      this.viewContainerRef));
  }

  cancel() {
    this.updateAlarmConfigForm(this.alarmFilterConfig);
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.alarmFilterOverlayRef.dispose();
    }
  }

  update() {
    this.alarmConfigUpdated(this.alarmFilterConfigForm.value);
    if (this.panelMode) {
      this.panelResult = this.alarmFilterConfig;
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    } else {
      this.alarmFilterOverlayRef.dispose();
    }
  }

  public alarmTypeList(): string[] {
    return this.alarmFilterConfigForm.get('typeList').value;
  }

  public removeAlarmType(type: string): void {
    const types: string[] = this.alarmFilterConfigForm.get('typeList').value;
    const index = types.indexOf(type);
    if (index >= 0) {
      types.splice(index, 1);
      this.alarmFilterConfigForm.get('typeList').setValue(types);
      this.alarmFilterConfigForm.get('typeList').markAsDirty();
    }
  }

  public addAlarmType(event: MatChipInputEvent): void {
    const input = event.chipInput.inputElement;
    const value = event.value;

    let types: string[] = this.alarmFilterConfigForm.get('typeList').value;

    if ((value || '').trim()) {
      if (!types) {
        types = [];
      }
      types.push(value.trim());
      this.alarmFilterConfigForm.get('typeList').setValue(types);
      this.alarmFilterConfigForm.get('typeList').markAsDirty();
    }

    if (input) {
      input.value = '';
    }
  }

  private updateAlarmConfigForm(alarmFilterConfig?: AlarmFilterConfig) {
    this.alarmFilterConfigForm.patchValue({
      statusList: alarmFilterConfig?.statusList,
      severityList: alarmFilterConfig?.severityList,
      typeList: alarmFilterConfig?.typeList,
      searchPropagatedAlarms: alarmFilterConfig?.searchPropagatedAlarms,
      assignedToCurrentUser: alarmFilterConfig?.assignedToCurrentUser,
      assigneeId: alarmFilterConfig?.assigneeId
    }, {emitEvent: false});
    this.updateValidators();
  }

  private alarmConfigUpdated(alarmFilterConfig: AlarmFilterConfig) {
    this.alarmFilterConfig = alarmFilterConfig;
    this.updateButtonDisplayValue();
    this.propagateChange(this.alarmFilterConfig);
  }

  private updateButtonDisplayValue() {
    if (this.buttonMode) {
      const filterTextParts: string[] = [];
      if (this.alarmFilterConfig?.statusList?.length) {
        filterTextParts.push(this.alarmFilterConfig.statusList.map(s =>
          this.translate.instant(alarmSearchStatusTranslations.get(s))).join(', '));
      }
      if (this.alarmFilterConfig?.severityList?.length) {
        filterTextParts.push(this.alarmFilterConfig.severityList.map(s =>
          this.translate.instant(alarmSeverityTranslations.get(s))).join(', '));
      }
      if (this.alarmFilterConfig?.typeList?.length) {
        filterTextParts.push(this.alarmFilterConfig.typeList.join(', '));
      }
      if (this.alarmFilterConfig?.assignedToCurrentUser) {
        filterTextParts.push(this.translate.instant('alarm.assigned-to-me'));
      } else if (this.alarmFilterConfig?.assigneeId) {
        filterTextParts.push(this.translate.instant('alarm.assigned'));
      }
      if (!filterTextParts.length) {
        this.buttonDisplayValue = this.translate.instant('alarm.alarm-filter-title');
      } else {
        this.buttonDisplayValue = this.translate.instant('alarm.filter-title') + `: ${filterTextParts.join(', ')}`;
      }
    }
  }

}
