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

import { Component, forwardRef, Injector, Input, OnInit, StaticProvider, ViewContainerRef } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { UserId } from '@shared/models/id/user-id';
import { UserService } from '@core/http/user.service';
import { User, UserEmailInfo } from '@shared/models/user.model';
import { catchError, map, of } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  ALARM_ASSIGNEE_SELECT_PANEL_DATA,
  AlarmAssigneeSelectPanelComponent,
  AlarmAssigneeSelectPanelData
} from '@home/components/alarm/alarm-assignee-select-panel.component';

@Component({
  selector: 'tb-alarm-assignee-select',
  templateUrl: './alarm-assignee-select.component.html',
  styleUrls: ['./alarm-assignee.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AlarmAssigneeSelectComponent),
      multi: true
    }
  ]
})
export class AlarmAssigneeSelectComponent implements OnInit, ControlValueAccessor {

  @Input() disabled: boolean;

  assigneeFormGroup: UntypedFormGroup;
  assignee?: User | UserEmailInfo;

  private propagateChange = (_: any) => {};

  constructor(private utilsService: UtilsService,
              private overlay: Overlay,
              private fb: UntypedFormBuilder,
              private userService: UserService,
              private viewContainerRef: ViewContainerRef,
              private translateService: TranslateService) {
  }

  ngOnInit(): void {
    this.assigneeFormGroup = this.fb.group({
      assignee: [null, []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.assigneeFormGroup.disable({emitEvent: false});
    } else {
      this.assigneeFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(userId?: UserId): void {
    const userObservable = userId ? this.userService.getUser(userId.id, {ignoreErrors: true}).pipe(
      catchError(() => of(null))
    ) : of(null);
    userObservable.pipe(
      tap((user) => {
        this.assignee = user;
      }),
      map((user) => this.getAssignee(user))
    ).subscribe((assignee) => {
      this.assigneeFormGroup.get('assignee').patchValue(assignee, {emitEvent: false});
    });
  }

  private getAssignee(user?: User| UserEmailInfo): string {
    if (user) {
      return this.getUserDisplayName(user);
    } else {
      return this.translateService.instant('alarm.assignee-not-set');
    }
  }

  private getUserDisplayName(user?: User | UserEmailInfo): string {
    let displayName = '';
    if ((user?.firstName && user?.firstName.length > 0) ||
      (user?.lastName && user?.lastName.length > 0)) {
      if (user?.firstName) {
        displayName += user?.firstName;
      }
      if (user?.lastName) {
        if (displayName.length > 0) {
          displayName += ' ';
        }
        displayName += user?.lastName;
      }
    } else {
      displayName = user?.email;
    }
    return displayName;
  }

  getUserInitials(): string {
    let initials = '';
    if (this.assignee?.firstName && this.assignee?.firstName.length ||
      this.assignee?.lastName && this.assignee?.lastName.length) {
      if (this.assignee?.firstName) {
        initials += this.assignee?.firstName.charAt(0);
      }
      if (this.assignee?.lastName) {
        initials += this.assignee?.lastName.charAt(0);
      }
    } else {
      initials += this.assignee?.email.charAt(0);
    }
    return initials.toUpperCase();
  }

  getAvatarBgColor(): string {
    return this.utilsService.stringToHslColor(this.getUserDisplayName(this.assignee), 40, 60);
  }

  openAlarmAssigneeSelectPanel($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    if (!this.disabled) {
      const target = $event.currentTarget;
      const config = new OverlayConfig();
      config.backdropClass = 'cdk-overlay-transparent-backdrop';
      config.hasBackdrop = true;
      const connectedPosition: ConnectedPosition = {
        originX: 'center',
        originY: 'bottom',
        overlayX: 'center',
        overlayY: 'top'
      };
      config.positionStrategy = this.overlay.position().flexibleConnectedTo(target as HTMLElement)
        .withPositions([connectedPosition]);
      config.width = (target as HTMLElement).offsetWidth;
      const overlayRef = this.overlay.create(config);
      overlayRef.backdropClick().subscribe(() => {
        overlayRef.dispose();
      });
      const providers: StaticProvider[] = [
        {
          provide: ALARM_ASSIGNEE_SELECT_PANEL_DATA,
          useValue: {
            assigneeId: this.assignee?.id?.id
          } as AlarmAssigneeSelectPanelData
        },
        {
          provide: OverlayRef,
          useValue: overlayRef
        }
      ];
      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      const component = overlayRef.attach(new ComponentPortal(AlarmAssigneeSelectPanelComponent,
        this.viewContainerRef, injector));
      component.onDestroy(() => {
        if (component.instance.userSelected) {
          this.assignee = component.instance.result;
          this.assigneeFormGroup.get('assignee').patchValue(this.getAssignee(this.assignee), {emitEvent: false});
          this.propagateChange(this.assignee?.id);
        }
      });
    }
  }

}
