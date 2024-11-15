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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { MINUTE, SECOND } from '@shared/models/time/time.models';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { shareReplay, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HasDebugConfig } from '@shared/models/entity.models';
import { distinctUntilChanged, map, tap } from 'rxjs/operators';

@Component({
  selector: 'tb-debug-config-panel',
  templateUrl: './debug-config-panel.component.html',
  standalone: true,
  imports: [
    SharedModule,
    CommonModule,
    DurationLeftPipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DebugConfigPanelComponent extends PageComponent implements OnInit {

  @Input() popover: TbPopoverComponent<DebugConfigPanelComponent>;
  @Input() debugFailures = false;
  @Input() debugAll = false;
  @Input() debugAllUntil = 0;
  @Input() maxDebugModeDurationMinutes: number;
  @Input() debugLimitsConfiguration: string;

  onFailuresControl = this.fb.control(false);
  debugAllControl = this.fb.control(false);

  maxMessagesCount: string;
  maxTimeFrameSec: string;

  isDebugAllActive$ = timer(0, SECOND).pipe(
    map(() => {
      this.cd.markForCheck();
      return this.debugAllUntil > new Date().getTime();
    }),
    distinctUntilChanged(),
    tap(isDebugOn => this.debugAllControl.patchValue(isDebugOn, { emitEvent: false })),
    shareReplay(1),
  );

  onConfigApplied = new EventEmitter<HasDebugConfig>();

  constructor(private fb: UntypedFormBuilder, private cd: ChangeDetectorRef) {
    super();

    this.observeDebugAllChange();
  }

  ngOnInit(): void {
    this.maxMessagesCount = this.debugLimitsConfiguration?.split(':')[0];
    this.maxTimeFrameSec = this.debugLimitsConfiguration?.split(':')[1];
    this.onFailuresControl.patchValue(this.debugFailures);
  }

  onCancel(): void {
    this.popover?.hide();
  }

  onApply(): void {
    this.onConfigApplied.emit({
      debugAll: this.debugAll,
      debugFailures: this.onFailuresControl.value,
      debugAllUntil: this.debugAllUntil
    });
  }

  onReset(): void {
    this.debugAll = true;
    this.debugAllUntil = new Date().getTime() + this.maxDebugModeDurationMinutes * MINUTE;
    this.cd.markForCheck();
  }

  private observeDebugAllChange(): void {
    this.debugAllControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(value => {
      this.debugAllUntil = value? new Date().getTime() + this.maxDebugModeDurationMinutes * MINUTE : 0;
      this.debugAll = value;
      this.cd.markForCheck();
    });
  }
}
