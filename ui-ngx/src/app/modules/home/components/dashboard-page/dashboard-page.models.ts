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

import { Dashboard, DashboardLayoutId, GridSettings, WidgetLayouts } from '@app/shared/models/dashboard.models';
import { Widget, WidgetPosition } from '@app/shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { ILayoutController } from './layout/layout.models';
import { DashboardContextMenuItem, WidgetContextMenuItem } from '@home/models/dashboard-component.models';
import { Observable } from 'rxjs';
import { EntityGroupInfo } from '@shared/models/entity-group.models';

export declare type DashboardPageScope = 'tenant' | 'customer';

export interface DashboardPageInitData {
  dashboard: Dashboard;
  currentDashboardId?: string;
  widgetEditMode?: boolean;
  singlePageMode?: boolean;
  entityGroup?: EntityGroupInfo;
  customerId?: string;
}

export interface DashboardContext {
  instanceId: string;
  state: string;
  getDashboard: () => Dashboard;
  dashboardTimewindow: Timewindow;
  aliasController: IAliasController;
  stateController: IStateController;
  stateChanged: Observable<string>;
  runChangeDetection: () => void;
}

export interface IDashboardController {
  dashboardCtx: DashboardContext;
  openRightLayout();
  openDashboardState(stateId: string, openRightLayout: boolean);
  addWidget($event: Event, layoutCtx: DashboardPageLayoutContext);
  editWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  exportWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  removeWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  widgetMouseDown($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  widgetClicked($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  prepareDashboardContextMenu(layoutCtx: DashboardPageLayoutContext): Array<DashboardContextMenuItem>;
  prepareWidgetContextMenu(layoutCtx: DashboardPageLayoutContext, widget: Widget): Array<WidgetContextMenuItem>;
  copyWidget($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  copyWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, widget: Widget);
  pasteWidget($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition);
  pasteWidgetReference($event: Event, layoutCtx: DashboardPageLayoutContext, pos: WidgetPosition);
}

export interface DashboardPageLayoutContext {
  id: DashboardLayoutId;
  widgets: LayoutWidgetsArray;
  widgetLayouts: WidgetLayouts;
  gridSettings: GridSettings;
  ctrl: ILayoutController;
  dashboardCtrl: IDashboardController;
  ignoreLoading: boolean;
}

export interface DashboardPageLayout {
  show: boolean;
  layoutCtx: DashboardPageLayoutContext;
}

export declare type DashboardPageLayouts = {[key in DashboardLayoutId]: DashboardPageLayout};

export class LayoutWidgetsArray implements Iterable<Widget> {

  private widgetIds: string[] = [];

  private loaded = false;

  constructor(private dashboardCtx: DashboardContext) {
  }

  size() {
    return this.widgetIds.length;
  }

  isLoading() {
    return !this.loaded;
  }

  isEmpty() {
    return this.loaded && this.widgetIds.length === 0;
  }

  setWidgetIds(widgetIds: string[]) {
    this.widgetIds = widgetIds;
    this.loaded = true;
  }

  addWidgetId(widgetId: string) {
    this.widgetIds.push(widgetId);
  }

  removeWidgetId(widgetId: string): boolean {
    const index = this.widgetIds.indexOf(widgetId);
    if (index > -1) {
      this.widgetIds.splice(index, 1);
      return true;
    }
    return false;
  }

  [Symbol.iterator](): Iterator<Widget> {
    let pointer = 0;
    const widgetIds = this.widgetIds;
    const dashboard = this.dashboardCtx.getDashboard();
    return {
      next(value?: any): IteratorResult<Widget> {
        if (pointer < widgetIds.length) {
          const widgetId = widgetIds[pointer++];
          const widget = dashboard.configuration.widgets[widgetId];
          return {
            done: false,
            value: widget
          };
        } else {
          return {
            done: true,
            value: null
          };
        }
      }
    };
  }

  public widgetByIndex(index: number): Widget {
    const widgetId = this.widgetIds[index];
    if (widgetId) {
      return this.widgetById(widgetId);
    } else {
      return null;
    }
  }

  private widgetById(widgetId: string): Widget {
    return this.dashboardCtx.getDashboard().configuration.widgets[widgetId];
  }

}
