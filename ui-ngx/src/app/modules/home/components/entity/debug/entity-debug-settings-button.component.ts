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
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { MatButton } from '@angular/material/button';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, of, shareReplay, timer } from 'rxjs';
import { SECOND, MINUTE } from '@shared/models/time/time.models';
import { AdditionalDebugActionConfig, EntityDebugSettings } from '@shared/models/entity.models';
import { map, switchMap, takeWhile } from 'rxjs/operators';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { ControlValueAccessor, FormBuilder, NG_VALUE_ACCESSOR } from '@angular/forms';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';

@Component({
  selector: 'tb-entity-debug-settings-button',
  templateUrl: './entity-debug-settings-button.component.html',
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    DurationLeftPipe,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityDebugSettingsButtonComponent),
      multi: true
    },
    EntityDebugSettingsService
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDebugSettingsButtonComponent implements ControlValueAccessor {

  @Input() debugLimitsConfiguration: string;
  @Input() entityLabel: string;
  @Input() additionalActionConfig: AdditionalDebugActionConfig;

  debugSettingsFormGroup = this.fb.group({
    failuresEnabled: [false],
    allEnabled: [false],
    allEnabledUntil: []
  });

  disabled = false;
  private allEnabledSubject = new BehaviorSubject(false);
  allEnabled$ = this.allEnabledSubject.asObservable();

  isDebugAllActive$ = this.allEnabled$.pipe(
    switchMap((value) => {
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
    shareReplay(1)
  );

  readonly maxDebugModeDuration = getCurrentAuthState(this.store).maxDebugModeDurationMinutes * MINUTE;

  private propagateChange: (settings: EntityDebugSettings) => void = () => {};

  constructor(private store: Store<AppState>,
              private fb: FormBuilder,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private cd : ChangeDetectorRef,
  ) {
    this.debugSettingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.propagateChange(value);
    });

    this.debugSettingsFormGroup.get('allEnabled').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => this.allEnabledSubject.next(value));
  }

  get failuresEnabled(): boolean {
    return this.debugSettingsFormGroup.get('failuresEnabled').value;
  }

  get allEnabledUntil(): number {
    return this.debugSettingsFormGroup.get('allEnabledUntil').value;
  }

  onOpenDebugStrategyPanel($event: Event, matButton: MatButton): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.entityDebugSettingsService.openDebugStrategyPanel({
      debugSettings: this.debugSettingsFormGroup.value,
      debugConfig: {
        maxDebugModeDuration: this.maxDebugModeDuration,
        debugLimitsConfiguration: this.debugLimitsConfiguration,
        entityLabel: this.entityLabel,
        additionalActionConfig: this.additionalActionConfig,
      },
      onSettingsAppliedFn: settings => {
        this.debugSettingsFormGroup.patchValue(settings);
        this.cd.markForCheck();
      }
    }, matButton._elementRef.nativeElement);
  }

  registerOnChange(fn: (settings: EntityDebugSettings) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_: () => void): void {}

  writeValue(settings: EntityDebugSettings): void {
    this.debugSettingsFormGroup.patchValue(settings, {emitEvent: false});
    this.allEnabledSubject.next(settings?.allEnabled);
    this.debugSettingsFormGroup.get('allEnabled').updateValueAndValidity({onlySelf: true});
    this.cd.markForCheck();
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.debugSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.debugSettingsFormGroup.enable({emitEvent: false});
    }
  }
}
