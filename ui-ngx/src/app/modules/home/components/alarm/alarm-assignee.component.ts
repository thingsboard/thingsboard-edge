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

import { Component, EventEmitter, Injector, Input, Output, StaticProvider, ViewContainerRef } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { AlarmAssignee, AlarmInfo, getUserDisplayName, getUserInitials } from '@shared/models/alarm.models';
import {
  ALARM_ASSIGNEE_PANEL_DATA,
  AlarmAssigneePanelComponent,
  AlarmAssigneePanelData
} from '@home/components/alarm/alarm-assignee-panel.component';
import { ConnectedPosition, Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { TranslateService } from '@ngx-translate/core';
import { isNotEmptyStr } from '@core/utils';

@Component({
  selector: 'tb-alarm-assignee',
  templateUrl: './alarm-assignee.component.html',
  styleUrls: ['./alarm-assignee.component.scss']
})
export class AlarmAssigneeComponent {
  @Input()
  alarm: AlarmInfo;

  @Input()
  allowAssign: boolean;

  @Output()
  alarmReassigned = new EventEmitter<boolean>();

  userAssigned: boolean;

  constructor(private utilsService: UtilsService,
              private overlay: Overlay,
              private viewContainerRef: ViewContainerRef,
              private translateService: TranslateService) {
  }

  getAssignee() {
    if (this.alarm) {
      this.userAssigned = this.alarm.assigneeId && ((isNotEmptyStr(this.alarm.assignee?.firstName) ||
        isNotEmptyStr(this.alarm.assignee?.lastName)) || isNotEmptyStr(this.alarm.assignee?.email));
      if (this.userAssigned) {
        return getUserDisplayName(this.alarm.assignee);
      } else {
        return this.translateService.instant(this.alarm.assigneeId ? 'alarm.user-deleted' : 'alarm.unassigned');
      }
    }
  }

  getUserInitials(alarmAssignee: AlarmAssignee): string {
    return getUserInitials(alarmAssignee);
  }

  getAvatarBgColor(entity: AlarmAssignee) {
    return this.utilsService.stringToHslColor(getUserDisplayName(entity), 40, 60);
  }

  openAlarmAssigneePanel($event: Event, alarm: AlarmInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    if (this.allowAssign) {
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
          provide: ALARM_ASSIGNEE_PANEL_DATA,
          useValue: {
            alarmId: alarm.id.id,
            assigneeId: alarm.assigneeId?.id
          } as AlarmAssigneePanelData
        },
        {
          provide: OverlayRef,
          useValue: overlayRef
        }
      ];
      const injector = Injector.create({parent: this.viewContainerRef.injector, providers});
      overlayRef.attach(new ComponentPortal(AlarmAssigneePanelComponent,
        this.viewContainerRef, injector)).onDestroy(() => this.alarmReassigned.emit(true));
    }
  }

}
