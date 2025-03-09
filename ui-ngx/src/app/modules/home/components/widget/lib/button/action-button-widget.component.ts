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

import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { BasicActionWidgetComponent } from '@home/components/widget/lib/action/action-widget.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ValueType } from '@shared/models/constants';
import {
  actionButtonDefaultSettings,
  ActionButtonWidgetSettings
} from '@home/components/widget/lib/button/action-button-widget.models';
import { WidgetButtonAppearance } from '@shared/components/button/widget-button.models';

@Component({
  selector: 'tb-action-button-widget',
  templateUrl: './action-button-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './action-button-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ActionButtonWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  settings: ActionButtonWidgetSettings;

  disabled = false;
  activated = false;

  appearance: WidgetButtonAppearance;
  borderRadius = '4px';

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              protected cd: ChangeDetectorRef) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...actionButtonDefaultSettings, ...this.ctx.settings};

    this.appearance = this.settings.appearance;

    const activatedStateSettings =
      {...this.settings.activatedState, actionLabel: this.ctx.translate.instant('widgets.button-state.activated-state')};
    this.createValueGetter(activatedStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onActivated(value)
    });

    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.button-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onDisabled(value)
    });
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    this.borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.cd.detectChanges();
  }

  public onClick($event: MouseEvent) {
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      this.ctx.actionsApi.click($event);
    }
  }

  private onActivated(value: boolean): void {
    this.activated = !!value;
    this.cd.markForCheck();
  }

  private onDisabled(value: boolean): void {
    this.disabled = !!value;
    this.cd.markForCheck();
  }

}
