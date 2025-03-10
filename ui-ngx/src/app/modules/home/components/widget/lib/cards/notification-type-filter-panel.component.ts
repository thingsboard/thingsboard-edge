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

import { Component, ElementRef, Inject, InjectionToken, OnInit, ViewChild } from '@angular/core';
import { NotificationTemplateTypeTranslateMap, NotificationType } from '@shared/models/notification.models';
import { MatChipInputEvent } from '@angular/material/chips';
import { COMMA, ENTER, SEMICOLON } from '@angular/cdk/keycodes';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { Observable } from 'rxjs';
import { FormControl } from '@angular/forms';
import { debounceTime, map } from 'rxjs/operators';
import { OverlayRef } from '@angular/cdk/overlay';

export const NOTIFICATION_TYPE_FILTER_PANEL_DATA = new InjectionToken<any>('NotificationTypeFilterPanelData');

export interface NotificationTypeFilterPanelData {
  notificationTypes: Array<NotificationType>;
  notificationTypesUpdated: (notificationTypes: Array<NotificationType>) => void;
}

@Component({
  selector: 'tb-notification-type-filter-panel',
  templateUrl: './notification-type-filter-panel.component.html',
  styleUrls: ['notification-type-filter-panel.component.scss']
})
export class NotificationTypeFilterPanelComponent implements OnInit{

  @ViewChild('searchInput') searchInputField: ElementRef;

  searchText = '';
  searchControlName = new FormControl('');

  filteredNotificationTypesList: Observable<Array<NotificationType>>;
  selectedNotificationTypes: Array<NotificationType> = [];
  notificationTypesTranslateMap = NotificationTemplateTypeTranslateMap;

  separatorKeysCodes: number[] = [ENTER, COMMA, SEMICOLON];

  private notificationType = NotificationType;
  private notificationTypes = Object.keys(NotificationType) as Array<NotificationType>;

  private dirty = false;

  @ViewChild('notificationTypeInput') notificationTypeInput: ElementRef<HTMLInputElement>;

  constructor(@Inject(NOTIFICATION_TYPE_FILTER_PANEL_DATA) public data: NotificationTypeFilterPanelData,
              private overlayRef: OverlayRef) {
    this.selectedNotificationTypes = this.data.notificationTypes;
    this.dirty = true;
  }

  ngOnInit() {
    this.filteredNotificationTypesList = this.searchControlName.valueChanges.pipe(
      debounceTime(150),
      map(value => {
        this.searchText = value;
        return this.notificationTypes.filter(type => !this.selectedNotificationTypes.includes(type))
          .filter(type => value ? type.toUpperCase().startsWith(value.toUpperCase()) : true);
      })
    );
  }

  public update() {
    this.data.notificationTypesUpdated(this.selectedNotificationTypes);
    if (this.overlayRef) {
      this.overlayRef.dispose();
    }
  }

  cancel() {
    if (this.overlayRef) {
      this.overlayRef.dispose();
    }
  }

  public reset() {
    this.selectedNotificationTypes.length = 0;
    this.searchControlName.updateValueAndValidity({emitEvent: true});
  }

  remove(type: NotificationType) {
    const index = this.selectedNotificationTypes.indexOf(type);
    if (index >= 0) {
      this.selectedNotificationTypes.splice(index, 1);
      this.searchControlName.updateValueAndValidity({emitEvent: true});
    }
  }

  onFocus() {
    if (this.dirty) {
      this.searchControlName.updateValueAndValidity({emitEvent: true});
      this.dirty = false;
    }
  }

  private add(type: NotificationType): void {
    this.selectedNotificationTypes.push(type);
  }

  chipAdd(event: MatChipInputEvent): void {
    const value = (event.value || '').trim();
    if (value && this.notificationType[value]) {
      this.add(this.notificationType[value]);
      this.clear('');
    }
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    if (this.notificationType[event.option.value]) {
      this.add(this.notificationType[event.option.value]);
    }
    this.clear('');
  }

  clear(value: string = '') {
    this.notificationTypeInput.nativeElement.value = value;
    this.searchControlName.patchValue(value, {emitEvent: true});
    setTimeout(() => {
      this.notificationTypeInput.nativeElement.blur();
      this.notificationTypeInput.nativeElement.focus();
    }, 0);
  }

  displayTypeFn(type?: string): string | undefined {
    return type ? type : undefined;
  }
}
