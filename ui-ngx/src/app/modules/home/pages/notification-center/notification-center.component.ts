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

import { Component, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MatTab, MatTabGroup } from '@angular/material/tabs';
import {
  NotificationTableComponent
} from '@home/pages/notification-center/notification-table/notification-table.component';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-notification-center',
  templateUrl: './notification-center.component.html',
  styleUrls: ['notification-center.component.scss']
})
export class NotificationCenterComponent extends PageComponent {

  entityType = EntityType;

  @ViewChild('matTabGroup', {static: true}) matTabs: MatTabGroup;
  @ViewChild('requestTab', {static: true}) requestTab: MatTab;
  @ViewChild('notificationRequest', {static: true}) notificationRequestTable: NotificationTableComponent;
  @ViewChildren(NotificationTableComponent) tableComponent: QueryList<NotificationTableComponent>;


  constructor(
    protected store: Store<AppState>) {
    super(store);
  }

  updateData() {
    this.currentTableComponent?.updateData();
  }

  openSetting() {

  }

  private get currentTableComponent(): NotificationTableComponent {
    return this.tableComponent.get(this.matTabs.selectedIndex);
  }

  sendNotification($event: Event) {
    this.notificationRequestTable.entityTableConfig.onEntityAction({event: $event, action: this.requestTab.isActive ? 'add' : 'add-without-update', entity: null});
  }
}
