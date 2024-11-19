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
  Component,
  Input,
  Renderer2,
  ViewContainerRef,
  DestroyRef,
  ChangeDetectionStrategy,
  EventEmitter,
  Output
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { TbPopoverService } from '@shared/components/popover.service';
import { MatButton } from '@angular/material/button';
import { DebugConfigPanelComponent } from './debug-config-panel.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { shareReplay, timer } from 'rxjs';
import { SECOND } from '@shared/models/time/time.models';
import { HasDebugConfig } from '@shared/models/entity.models';
import { map } from 'rxjs/operators';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';

@Component({
  selector: 'tb-debug-config-button',
  templateUrl: './debug-config-button.component.html',
  styleUrls: ['./debug-config-button.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    DurationLeftPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DebugConfigButtonComponent {

  @Input() debugFailures = false;
  @Input() debugAll = false;
  @Input() debugAllUntil = 0;
  @Input() disabled = false;
  @Input() minifyMode = false;
  @Input() debugLimitsConfiguration: string;

  @Output() onDebugConfigChanged = new EventEmitter<HasDebugConfig>();

  isDebugAllActive$ = timer(0, SECOND).pipe(map(() => this.debugAllUntil > new Date().getTime() || this.debugAll), shareReplay(1));

  readonly maxDebugModeDurationMinutes = getCurrentAuthState(this.store).maxDebugModeDurationMinutes;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private store: Store<AppState>,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef,
  ) {}

  openDebugStrategyPanel($event: Event, matButton: MatButton): void {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const debugStrategyPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, DebugConfigPanelComponent, 'bottom', true, null,
        {
          debugFailures: this.debugFailures,
          debugAll: this.debugAll,
          debugAllUntil: this.debugAllUntil,
          maxDebugModeDurationMinutes: this.maxDebugModeDurationMinutes,
          debugLimitsConfiguration: this.debugLimitsConfiguration
        },
        {},
        {}, {}, true);
      debugStrategyPopover.tbComponentRef.instance.popover = debugStrategyPopover;
      debugStrategyPopover.tbComponentRef.instance.onConfigApplied.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((config: HasDebugConfig) => {
        this.onDebugConfigChanged.emit(config);
        debugStrategyPopover.hide();
      });
    }
  }
}
