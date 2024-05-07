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

import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { BadgePosition, BadgeStyle, badgeStyleURLMap, MobileAppQRCodeSettings } from '@shared/models/mobile-app.models';
import { MobileAppService } from '@core/http/mobile-app.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import { interval, mergeMap, Observable, Subject, takeUntil } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';
import { coerceBoolean } from '@shared/decorators/coercion';
import { MobileAppQrCodeWidgetSettings } from '@home/components/widget/lib/cards/mobile-app-qr-code-widge.models';

@Component({
  selector: 'tb-mobile-app-qrcode-widget',
  templateUrl: './mobile-app-qrcode-widget.component.html',
  styleUrls: ['./mobile-app-qrcode-widget.component.scss']
})
export class MobileAppQrcodeWidgetComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input()
  ctx: WidgetContext;

  @Input()
  @coerceBoolean()
  previewMode: boolean;

  @Input()
  set mobileAppSettings(settings: MobileAppQRCodeSettings | MobileAppQrCodeWidgetSettings) {
    if (settings) {
      this.mobileAppSettingsValue = settings;
    }
  };

  get mobileAppSettings() {
    return this.mobileAppSettingsValue;
  }

  @ViewChild('canvas', {static: false}) canvasRef: ElementRef<HTMLCanvasElement>;

  private readonly destroy$ = new Subject<void>();

  badgeStyle = BadgeStyle;
  badgePosition = BadgePosition;
  badgeStyleURLMap = badgeStyleURLMap;

  private mobileAppSettingsValue: MobileAppQRCodeSettings | MobileAppQrCodeWidgetSettings;
  private deepLinkTTL: number;

  constructor(protected store: Store<AppState>,
              protected cd: ChangeDetectorRef,
              private mobileAppService: MobileAppService,
              private utilsService: UtilsService) {
    super(store);
  }

  ngOnInit(): void {
    if (this.ctx) {
      this.mobileAppSettings = this.ctx.settings;
    } else {
      this.mobileAppService.getMobileAppSettings().subscribe((settings => {
        this.mobileAppSettings = settings;
        this.cd.detectChanges();
      }));
    }
  }

  ngAfterViewInit(): void {
    this.getMobileAppDeepLink().subscribe(link => {
      this.deepLinkTTL = Number(this.utilsService.getQueryParam('ttl', link)) * MINUTE;
      this.updateQRCode(link);
      interval(this.deepLinkTTL).pipe(
        takeUntil(this.destroy$),
        mergeMap(() => this.getMobileAppDeepLink())
      ).subscribe(link => this.updateQRCode(link));
    });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  getMobileAppDeepLink(): Observable<string> {
    return this.mobileAppService.getMobileAppDeepLink();
  }

  updateQRCode(link: string) {
    import('qrcode').then((QRCode) => {
      QRCode.toCanvas(this.canvasRef.nativeElement, link, { width: 90 });
    });
  }

}
