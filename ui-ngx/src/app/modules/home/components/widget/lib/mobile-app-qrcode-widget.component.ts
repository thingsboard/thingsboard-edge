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

import { ChangeDetectorRef, Component, ElementRef, Input, NgZone, OnDestroy, OnInit, TemplateRef } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { BadgePosition, QrCodeSettings } from '@shared/models/mobile-app.models';
import { MobileApplicationService } from '@core/http/mobile-application.service';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import { Observable, Subject } from 'rxjs';
import { MINUTE } from '@shared/models/time/time.models';
import { isDefinedAndNotNull, mergeDeep, unwrapModule } from '@core/utils';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'tb-mobile-app-qrcode-widget',
  templateUrl: './mobile-app-qrcode-widget.component.html',
  styleUrls: ['./mobile-app-qrcode-widget.component.scss']
})
export class MobileAppQrcodeWidgetComponent extends PageComponent implements OnInit, OnDestroy {

  private readonly destroy$ = new Subject<void>();
  private widgetResize$: ResizeObserver;

  private mobileAppSettingsValue: QrCodeSettings;
  private deepLink: string;
  private deepLinkTTL: number;
  private deepLinkTTLTimeoutID: NodeJS.Timeout;

  googlePlayLink: string;
  appStoreLink: string;

  previewMode = false;

  badgePosition = BadgePosition;
  showBadgeContainer = true;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  qrCodeSVG = '';

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  @Input()
  set mobileAppSettings(settings: QrCodeSettings) {
    if (settings) {
      this.mobileAppSettingsValue = settings;
    }
  };

  get mobileAppSettings(): QrCodeSettings {
    return this.mobileAppSettingsValue;
  }

  constructor(protected store: Store<AppState>,
              protected cd: ChangeDetectorRef,
              private mobileAppService: MobileApplicationService,
              private utilsService: UtilsService,
              private elementRef: ElementRef,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private zone: NgZone) {
    super(store);
  }

  ngOnInit(): void {
    if (!this.mobileAppSettings) {
      this.mobileAppService.getMergedMobileAppSettings().subscribe((settings => {
        this.mobileAppSettings = settings;

        this.appStoreLink = this.mobileAppSettings.appStoreLink;
        this.googlePlayLink = this.mobileAppSettings.googlePlayLink;

        if (isDefinedAndNotNull(this.ctx.settings.useSystemSettings) && !this.ctx.settings.useSystemSettings) {
          this.mobileAppSettings = mergeDeep(this.mobileAppSettings, this.ctx.settings);
        }

        this.widgetResize$ = new ResizeObserver(() => {
          this.zone.run(() => {
            const showHideBadgeContainer = this.elementRef.nativeElement.offsetWidth > 250;
            if (showHideBadgeContainer !== this.showBadgeContainer) {
              this.showBadgeContainer = showHideBadgeContainer;
              this.cd.markForCheck();
            }
          });
        });

        this.widgetResize$.observe(this.elementRef.nativeElement);
        this.backgroundStyle$ = backgroundStyle(this.ctx.settings.background, this.imagePipe, this.sanitizer);
        this.overlayStyle = overlayStyle(this.ctx.settings.background.overlay);
        this.padding = this.ctx.settings.background.overlay.enabled ? undefined : this.ctx.settings.padding;
        this.cd.markForCheck();
      }));
    } else {
      this.previewMode = true;
    }
    this.initMobileAppQRCode();
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

  navigateByDeepLink($event: Event) {
    $event?.stopPropagation();
    if (this.ctx.isMobile) {
      window.open(this.deepLink, '_blank');
    }
  }

  private initMobileAppQRCode() {
    if (this.deepLinkTTLTimeoutID) {
      clearTimeout(this.deepLinkTTLTimeoutID);
      this.deepLinkTTLTimeoutID = null;
    }
    this.mobileAppService.getMobileAppDeepLink().subscribe(link => {
      this.deepLink = link;
      this.deepLinkTTL = Number(this.utilsService.getQueryParam('ttl', link)) * MINUTE;
      this.updateQRCode(link);
      this.deepLinkTTLTimeoutID = setTimeout(() => this.initMobileAppQRCode(), this.deepLinkTTL);
    });
  }

  private updateQRCode(link: string) {
    import('qrcode').then((QRCode) => {
      unwrapModule(QRCode).toString(link, (_err, svgElement) => {
        this.qrCodeSVG = svgElement;
        this.cd.markForCheck();
      })
    });
  }

}
