///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
  OnInit,
  Output
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { DashboardWidget, DashboardWidgets } from '@home/models/dashboard-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { SafeStyle } from '@angular/platform-browser';
import { WidgetExportType, widgetExportTypeTranslationMap } from '@shared/models/widget.models';

export enum WidgetComponentActionType {
  MOUSE_DOWN,
  CLICKED,
  CONTEXT_MENU,
  EDIT,
  EXPORT,
  REMOVE
}

export class WidgetComponentAction {
  event: MouseEvent;
  actionType: WidgetComponentActionType;
}

@Component({
  selector: 'tb-widget-container',
  templateUrl: './widget-container.component.html',
  styleUrls: ['./widget-container.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class WidgetContainerComponent extends PageComponent implements OnInit {

  @Input()
  widget: DashboardWidget;

  @Input()
  dashboardStyle: {[klass: string]: any};

  @Input()
  backgroundImage: SafeStyle | string;

  @Input()
  isEdit: boolean;

  @Input()
  isMobile: boolean;

  @Input()
  dashboardWidgets: DashboardWidgets;

  @Input()
  isEditActionEnabled: boolean;

  @Input()
  isExportActionEnabled: boolean;

  @Input()
  isRemoveActionEnabled: boolean;

  @Input()
  disableWidgetInteraction = false;

  @Output()
  widgetFullscreenChanged: EventEmitter<boolean> = new EventEmitter<boolean>();

  @Output()
  widgetComponentAction: EventEmitter<WidgetComponentAction> = new EventEmitter<WidgetComponentAction>();

  widgetExportType = WidgetExportType;
  widgetExportTypeTranslations = widgetExportTypeTranslationMap;

  constructor(protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.widget.widgetContext.containerChangeDetector = this.cd;
  }

  isHighlighted(widget: DashboardWidget) {
    return this.dashboardWidgets.isHighlighted(widget);
  }

  isNotHighlighted(widget: DashboardWidget) {
    return this.dashboardWidgets.isNotHighlighted(widget);
  }

  onFullscreenChanged(expanded: boolean) {
    this.widgetFullscreenChanged.emit(expanded);
  }

  onMouseDown(event: MouseEvent) {
    this.widgetComponentAction.emit({
      event,
      actionType: WidgetComponentActionType.MOUSE_DOWN
    });
  }

  onClicked(event: MouseEvent) {
    this.widgetComponentAction.emit({
      event,
      actionType: WidgetComponentActionType.CLICKED
    });
  }

  onContextMenu(event: MouseEvent) {
    this.widgetComponentAction.emit({
      event,
      actionType: WidgetComponentActionType.CONTEXT_MENU
    });
  }

  onEdit(event: MouseEvent) {
    this.widgetComponentAction.emit({
      event,
      actionType: WidgetComponentActionType.EDIT
    });
  }

  onExport(event: MouseEvent) {
    this.widgetComponentAction.emit({
      event,
      actionType: WidgetComponentActionType.EXPORT
    });
  }

  onRemove(event: MouseEvent) {
    this.widgetComponentAction.emit({
      event,
      actionType: WidgetComponentActionType.REMOVE
    });
  }

}
