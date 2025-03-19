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

import {
  booleanAttribute,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { SECOND } from '@shared/models/time/time.models';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { of, shareReplay, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { distinctUntilChanged, map, startWith, switchMap, takeWhile } from 'rxjs/operators';
import { AdditionalDebugActionConfig } from '@home/components/entity/debug/entity-debug-settings.model';

@Component({
  selector: 'tb-entity-debug-settings-panel',
  templateUrl: './entity-debug-settings-panel.component.html',
  standalone: true,
  imports: [
    SharedModule,
    CommonModule,
    DurationLeftPipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDebugSettingsPanelComponent extends PageComponent implements OnInit {

  @Input({ transform: booleanAttribute }) failuresEnabled = false;
  @Input({ transform: booleanAttribute }) allEnabled = false;
  @Input() entityLabel: string;
  @Input() allEnabledUntil = 0;
  @Input() maxDebugModeDuration: number;
  @Input() debugLimitsConfiguration: string;
  @Input() additionalActionConfig: AdditionalDebugActionConfig;

  onFailuresControl = this.fb.control(false);
  debugAllControl = this.fb.control(false);

  maxMessagesCount: string;
  maxTimeFrameDuration: number;
  initialAllEnabled: boolean;

  isDebugAllActive$ = this.debugAllControl.valueChanges.pipe(
    startWith(this.debugAllControl.value),
    switchMap(value => {
      if (value) {
        return of(true);
      } else {
        return timer(0, SECOND).pipe(
          map(() => this.allEnabledUntil > new Date().getTime()),
          takeWhile(value => value, true)
        );
      }
    }),
    takeUntilDestroyed(),
    shareReplay(1),
  );

  onSettingsApplied = new EventEmitter<EntityDebugSettings>();

  constructor(private fb: FormBuilder,
              private cd: ChangeDetectorRef,
              private popover: TbPopoverComponent<EntityDebugSettingsPanelComponent>) {
    super();

    this.debugAllControl.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.allEnabled = value;
      this.cd.markForCheck();
    });

    this.isDebugAllActive$.pipe(
      distinctUntilChanged(),
      takeUntilDestroyed()
    ).subscribe(isDebugOn => this.debugAllControl.patchValue(isDebugOn, {emitEvent: false}))
  }

  ngOnInit(): void {
    this.maxMessagesCount = this.debugLimitsConfiguration?.split(':')[0];
    this.maxTimeFrameDuration = parseInt(this.debugLimitsConfiguration?.split(':')[1]) * SECOND;
    this.onFailuresControl.patchValue(this.failuresEnabled);
    this.debugAllControl.patchValue(this.allEnabled);
    this.initialAllEnabled = this.allEnabled || this.allEnabledUntil > new Date().getTime();
  }

  onCancel(): void {
    this.popover.hide();
  }

  onApply(): void {
    const isDebugAllChanged = this.initialAllEnabled !== this.debugAllControl.value || this.initialAllEnabled !== this.allEnabledUntil > new Date().getTime();
    if (isDebugAllChanged) {
      this.onSettingsApplied.emit({
        allEnabled: this.allEnabled,
        failuresEnabled: this.onFailuresControl.value,
        allEnabledUntil: 0,
      });
    } else {
      this.onSettingsApplied.emit({
        allEnabled: false,
        failuresEnabled: this.onFailuresControl.value,
        allEnabledUntil: this.allEnabledUntil,
      });
    }
  }

  onReset(): void {
    this.debugAllControl.patchValue(true);
    this.debugAllControl.markAsDirty();
    this.allEnabledUntil = 0;
    this.cd.markForCheck();
  }
}
