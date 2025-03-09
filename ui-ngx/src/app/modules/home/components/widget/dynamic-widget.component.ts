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

import { PageComponent } from '@shared/components/page.component';
import { Directive, inject, Injector, OnDestroy, OnInit } from '@angular/core';
import {
  IDynamicWidgetComponent,
  widgetContextToken,
  widgetErrorMessagesToken,
  widgetTitlePanelToken
} from '@home/models/widget-component.models';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RafService } from '@core/services/raf.service';
import {
  NotificationHorizontalPosition,
  NotificationType,
  NotificationVerticalPosition
} from '@core/notification/notification.models';
import { UntypedFormBuilder, Validators } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { CustomerService } from '@core/http/customer.service';
import { DashboardService } from '@core/http/dashboard.service';
import { UserService } from '@core/http/user.service';
import { AttributeService } from '@core/http/attribute.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { EntityService } from '@core/http/entity.service';
import { AuthService } from '@core/auth/auth.service';
import { DialogService } from '@core/services/dialog.service';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { ResourceService } from '@core/http/resource.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { EntityGroupService } from '@core/http/entity-group.service';
import { DomSanitizer } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { ReportService } from '@core/http/report.service';
import { MillisecondsToTimeStringPipe } from '@shared/pipe/milliseconds-to-time-string.pipe';
import { UserSettingsService } from '@core/http/user-settings.service';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { UtilsService } from '@core/services/utils.service';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export class DynamicWidgetComponent extends PageComponent implements IDynamicWidgetComponent, OnInit, OnDestroy {

  executingRpcRequest: boolean;
  rpcEnabled: boolean;
  rpcErrorText: string;
  rpcRejection: HttpErrorResponse | Error;

  [key: string]: any;

  validators = Validators;

  public raf = inject(RafService);
  public fb = inject(UntypedFormBuilder);
  public readonly $injector = inject(Injector);
  public readonly ctx = inject(widgetContextToken);
  public readonly errorMessages = inject(widgetErrorMessagesToken);
  public readonly widgetTitlePanel = inject(widgetTitlePanelToken);

  constructor() {
    super();
    this.ctx.$injector = this.$injector;
    this.ctx.deviceService = this.$injector.get(DeviceService);
    this.ctx.assetService = this.$injector.get(AssetService);
    this.ctx.entityViewService = this.$injector.get(EntityViewService);
    this.ctx.customerService = this.$injector.get(CustomerService);
    this.ctx.dashboardService = this.$injector.get(DashboardService);
    this.ctx.userService = this.$injector.get(UserService);
    this.ctx.attributeService = this.$injector.get(AttributeService);
    this.ctx.entityRelationService = this.$injector.get(EntityRelationService);
    this.ctx.entityService = this.$injector.get(EntityService);
    this.ctx.entityGroupService = this.$injector.get(EntityGroupService);
    this.ctx.authService = this.$injector.get(AuthService);
    this.ctx.dialogs = this.$injector.get(DialogService);
    this.ctx.customDialog = this.$injector.get(CustomDialogService);
    this.ctx.resourceService = this.$injector.get(ResourceService);
    this.ctx.userSettingsService = this.$injector.get(UserSettingsService);
    this.ctx.utilsService = this.$injector.get(UtilsService);
    this.ctx.telemetryWsService = this.$injector.get(TelemetryWebsocketService);
    this.ctx.date = this.$injector.get(DatePipe);
    this.ctx.imagePipe = this.$injector.get(ImagePipe);
    this.ctx.milliSecondsToTimeString = this.$injector.get(MillisecondsToTimeStringPipe);
    this.ctx.translate = this.$injector.get(TranslateService);
    this.ctx.http = this.$injector.get(HttpClient);
    this.ctx.sanitizer = this.$injector.get(DomSanitizer);
    this.ctx.router = this.$injector.get(Router);
    this.ctx.reportService = this.$injector.get(ReportService);
    this.ctx.wl = this.$injector.get(WhiteLabelingService);

    this.ctx.$scope = this;
    if (this.ctx.defaultSubscription) {
      this.executingRpcRequest = this.ctx.defaultSubscription.executingRpcRequest;
      this.rpcEnabled = this.ctx.defaultSubscription.rpcEnabled;
      this.rpcErrorText = this.ctx.defaultSubscription.rpcErrorText;
      this.rpcRejection = this.ctx.defaultSubscription.rpcRejection;
    }
  }

  ngOnInit() {

  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.ctx.telemetrySubscribers) {
      this.ctx.telemetrySubscribers.forEach(item =>  item.unsubscribe());
    }
  }

  clearRpcError() {
    if (this.widgetContext.defaultSubscription) {
      this.widgetContext.defaultSubscription.clearRpcError();
    }
  }

  showSuccessToast(message: string, duration: number = 1000,
                   verticalPosition: NotificationVerticalPosition = 'bottom',
                   horizontalPosition: NotificationHorizontalPosition = 'left',
                   target?: string) {
    this.ctx.showSuccessToast(message, duration, verticalPosition, horizontalPosition, target);
  }

  showErrorToast(message: string,
                 verticalPosition: NotificationVerticalPosition = 'bottom',
                 horizontalPosition: NotificationHorizontalPosition = 'left',
                 target?: string) {
    this.ctx.showErrorToast(message, verticalPosition, horizontalPosition, target);
  }

  showToast(type: NotificationType, message: string, duration: number,
            verticalPosition: NotificationVerticalPosition = 'bottom',
            horizontalPosition: NotificationHorizontalPosition = 'left',
            target?: string) {
    this.ctx.showToast(type, message, duration, verticalPosition, horizontalPosition, target);
  }

}
