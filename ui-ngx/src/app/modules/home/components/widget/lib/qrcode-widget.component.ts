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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import QRCode from 'qrcode';
import {
  fillPattern,
  parseData,
  parseFunction,
  processPattern,
  safeExecute
} from '@home/components/widget/lib/maps/common-maps-utils';
import { FormattedData } from '@home/components/widget/lib/maps/map-models';
import { DatasourceData } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

interface QrCodeWidgetSettings {
  qrCodeTextPattern: string;
  useQrCodeTextFunction: boolean;
  qrCodeTextFunction: string;
}

type QrCodeTextFunction = (data: FormattedData) => string;

@Component({
  selector: 'tb-qrcode-widget',
  templateUrl: './qrcode-widget.component.html',
  styleUrls: []
})
export class QrCodeWidgetComponent extends PageComponent implements OnInit, AfterViewInit {

  settings: QrCodeWidgetSettings;
  qrCodeTextFunction: QrCodeTextFunction;

  @Input()
  ctx: WidgetContext;

  qrCodeText: string;

  private viewInited: boolean;
  private scheduleUpdateCanvas: boolean;

  @ViewChild('canvas', {static: false}) canvasRef: ElementRef<HTMLCanvasElement>;

  constructor(protected store: Store<AppState>,
              protected cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.qrCodeWidget = this;
    this.settings = this.ctx.settings;
    this.qrCodeTextFunction = this.settings.useQrCodeTextFunction ? parseFunction(this.settings.qrCodeTextFunction, ['data']) : null;
  }

  ngAfterViewInit(): void {
    this.viewInited = true;
    if (this.scheduleUpdateCanvas) {
      this.scheduleUpdateCanvas = false;
      this.updateCanvas();
    }
  }

  public onDataUpdated() {
    let initialData: DatasourceData[];
    let qrCodeText: string;
    if (this.ctx.data?.length) {
      initialData = this.ctx.data;
    } else if (this.ctx.datasources?.length) {
      initialData = [
        {
          datasource: this.ctx.datasources[0],
          dataKey: {
            type: DataKeyType.attribute,
            name: 'empty'
          },
          data: []
        }
      ];
    }
    if (initialData) {
      const data = parseData(initialData);
      const dataSourceData = data[0];
      const pattern = this.settings.useQrCodeTextFunction ?
        safeExecute(this.qrCodeTextFunction, [dataSourceData]) : this.settings.qrCodeTextPattern;
      const replaceInfo = processPattern(pattern, data);
      qrCodeText = fillPattern(pattern, replaceInfo, dataSourceData);
    }
    this.updateQrCodeText(qrCodeText);
  }

  private updateQrCodeText(newQrCodeText: string): void {
    if (this.qrCodeText !== newQrCodeText) {
      this.qrCodeText = newQrCodeText;
      if (this.qrCodeText) {
        this.updateCanvas();
      }
      this.cd.detectChanges();
    }
  }

  private updateCanvas() {
    if (this.viewInited) {
      QRCode.toCanvas(this.canvasRef.nativeElement, this.qrCodeText);
      this.canvasRef.nativeElement.style.width = 'auto';
      this.canvasRef.nativeElement.style.height = 'auto';
    } else {
      this.scheduleUpdateCanvas = true;
    }
  }

}
