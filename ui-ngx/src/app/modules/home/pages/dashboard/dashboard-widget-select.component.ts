///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
  OnDestroy,
  OnInit,
  ViewEncapsulation,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { IAliasController } from '@core/api/widget-api.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { WidgetService } from '@core/http/widget.service';
import { widgetType, Widget } from '@shared/models/widget.models';
import { toWidgetInfo } from '@home/models/widget-component.models';
import { DashboardCallbacks } from '../../models/dashboard-component.models';

@Component({
  selector: 'tb-dashboard-widget-select',
  templateUrl: './dashboard-widget-select.component.html',
  styleUrls: ['./dashboard-widget-select.component.scss']
})
export class DashboardWidgetSelectComponent implements OnInit, OnChanges {

  @Input()
  widgetsBundle: WidgetsBundle;

  @Input()
  aliasController: IAliasController;

  @Output()
  widgetSelected: EventEmitter<Widget> = new EventEmitter<Widget>();

  timeseriesWidgetTypes: Array<Widget> = [];
  latestWidgetTypes: Array<Widget> = [];
  rpcWidgetTypes: Array<Widget> = [];
  alarmWidgetTypes: Array<Widget> = [];
  staticWidgetTypes: Array<Widget> = [];

  callbacks: DashboardCallbacks = {
    onWidgetClicked: this.onWidgetClicked.bind(this)
  };

  constructor(private widgetsService: WidgetService) {
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (change.currentValue !== change.previousValue && change.currentValue) {
        if (propName === 'widgetsBundle') {
          this.loadLibrary();
        }
      }
    }
  }

  private loadLibrary() {
    this.timeseriesWidgetTypes.length = 0;
    this.latestWidgetTypes.length = 0;
    this.rpcWidgetTypes.length = 0;
    this.alarmWidgetTypes.length = 0;
    this.staticWidgetTypes.length = 0;
    const bundleAlias = this.widgetsBundle.alias;
    const isSystem = this.widgetsBundle.tenantId.id === NULL_UUID;
    this.widgetsService.getBundleWidgetTypes(bundleAlias,
      isSystem).subscribe(
      (types) => {
        types = types.sort((a, b) => b.createdTime - a.createdTime);
        let top = 0;
        types.forEach((type) => {
          const widgetTypeInfo = toWidgetInfo(type);
          const widget: Widget = {
            typeId: type.id,
            isSystemType: isSystem,
            bundleAlias,
            typeAlias: widgetTypeInfo.alias,
            type: widgetTypeInfo.type,
            title: widgetTypeInfo.widgetName,
            sizeX: widgetTypeInfo.sizeX,
            sizeY: widgetTypeInfo.sizeY,
            row: top,
            col: 0,
            config: JSON.parse(widgetTypeInfo.defaultConfig)
          };
          widget.config.title = widgetTypeInfo.widgetName;
          switch (widgetTypeInfo.type) {
            case widgetType.timeseries:
              this.timeseriesWidgetTypes.push(widget);
              break;
            case widgetType.latest:
              this.latestWidgetTypes.push(widget);
              break;
            case widgetType.rpc:
              this.rpcWidgetTypes.push(widget);
              break;
            case widgetType.alarm:
              this.alarmWidgetTypes.push(widget);
              break;
            case widgetType.static:
              this.staticWidgetTypes.push(widget);
              break;
          }
          top += widget.sizeY;
        });
      }
    );
  }

  hasWidgetTypes() {
    return this.timeseriesWidgetTypes.length > 0 ||
           this.latestWidgetTypes.length > 0 ||
           this.rpcWidgetTypes.length > 0 ||
           this.alarmWidgetTypes.length > 0 ||
           this.staticWidgetTypes.length > 0;
  }

  private onWidgetClicked($event: Event, widget: Widget, index: number): void {
    this.widgetSelected.emit(widget);
  }

}
