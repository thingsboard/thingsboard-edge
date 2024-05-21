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

import { ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { BadgePosition, MobileAppSettings } from '@shared/models/mobile-app.models';
import { MobileAppService } from '@core/http/mobile-app.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import { Subject } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';
import { MobileAppQrCodeWidgetSettings } from '@home/components/widget/lib/cards/mobile-app-qr-code-widget.models';
import { isDefinedAndNotNull } from '@core/utils';
import { ResizeObserver } from '@juggle/resize-observer';

@Component({
  selector: 'tb-mobile-app-qrcode-widget',
  templateUrl: './mobile-app-qrcode-widget.component.html',
  styleUrls: ['./mobile-app-qrcode-widget.component.scss']
})
export class MobileAppQrcodeWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  @Input()
  set mobileAppSettings(settings: MobileAppSettings | MobileAppQrCodeWidgetSettings) {
    if (settings) {
      this.mobileAppSettingsValue = settings;
    }
  };

  get mobileAppSettings(): MobileAppSettings | MobileAppQrCodeWidgetSettings {
    return this.mobileAppSettingsValue;
  }

  @ViewChild('canvas', {static: true}) canvasRef: ElementRef<HTMLCanvasElement>;

  private readonly destroy$ = new Subject<void>();
  private widgetResize$: ResizeObserver;

  badgePosition = BadgePosition;
  showBadgeContainer = true;

  private mobileAppSettingsValue: MobileAppSettings | MobileAppQrCodeWidgetSettings;
  private deepLinkTTL: number;
  private deepLinkTTLTimeoutID: NodeJS.Timeout;

  constructor(protected store: Store<AppState>,
              protected cd: ChangeDetectorRef,
              private mobileAppService: MobileAppService,
              private utilsService: UtilsService,
              private elementRef: ElementRef) {
    super(store);
  }

  ngOnInit(): void {
    if (!this.mobileAppSettings) {
      if (isDefinedAndNotNull(this.ctx.settings.useSystemSettings) && !this.ctx.settings.useSystemSettings) {
        this.mobileAppSettings = this.ctx.settings;
      } else {
        this.mobileAppService.getMergedMobileAppSettings().subscribe((settings => {
          this.mobileAppSettings = settings;
          this.cd.markForCheck();
        }));
      }
    }
    this.initMobileAppQRCode();
    this.widgetResize$ = new ResizeObserver(() => {
      const showHideBadgeContainer = this.elementRef.nativeElement.offsetWidth > 250;
      if (showHideBadgeContainer !== this.showBadgeContainer) {
        this.showBadgeContainer = showHideBadgeContainer;
        this.cd.markForCheck();
      }
    });
    this.widgetResize$.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy() {
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
    clearTimeout(this.deepLinkTTLTimeoutID);
  }

  private initMobileAppQRCode() {
    if (this.deepLinkTTLTimeoutID) {
      clearTimeout(this.deepLinkTTLTimeoutID);
      this.deepLinkTTLTimeoutID = null;
    }
    this.mobileAppService.getMobileAppDeepLink().subscribe(link => {
      this.deepLinkTTL = Number(this.utilsService.getQueryParam('ttl', link)) * MINUTE;
      this.updateQRCode(link);
      this.deepLinkTTLTimeoutID = setTimeout(() => this.initMobileAppQRCode(), this.deepLinkTTL);
    });
  }

  private updateQRCode(link: string) {
    import('qrcode').then((QRCode) => {
      QRCode.toCanvas(this.canvasRef.nativeElement, link, { width: 100 });
    });
  }

}
