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
import { BasicActionWidgetComponent, ValueSetter } from '@home/components/widget/lib/action/action-widget.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ValueType } from '@shared/models/constants';
import { WidgetButtonAppearance } from '@shared/components/button/widget-button.models';
import {
  toggleButtonDefaultSettings,
  ToggleButtonWidgetSettings
} from '@home/components/widget/lib/button/toggle-button-widget.models';
import { Observable } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';

@Component({
  selector: 'tb-toggle-button-widget',
  templateUrl: './toggle-button-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './toggle-button-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ToggleButtonWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  settings: ToggleButtonWidgetSettings;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  value = false;
  disabled = false;

  autoScale: boolean;
  horizontalFill: boolean;
  verticalFill: boolean;
  appearance: WidgetButtonAppearance;

  private checkValueSetter: ValueSetter<boolean>;
  private uncheckValueSetter: ValueSetter<boolean>;

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              protected cd: ChangeDetectorRef) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...toggleButtonDefaultSettings, ...this.ctx.settings};

    this.autoScale = this.settings.autoScale;
    this.horizontalFill = this.settings.horizontalFill;
    this.verticalFill = this.settings.verticalFill;

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    const getInitialStateSettings =
      {...this.settings.initialState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.initial-state')};
    this.createValueGetter(getInitialStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onValue(value)
    });

    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.button-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onDisabled(value)
    });

    const checkStateSettings = {...this.settings.checkState,
      actionLabel: this.ctx.translate.instant('widgets.toggle-button.check')};
    this.checkValueSetter = this.createValueSetter(checkStateSettings);

    const uncheckStateSettings = {...this.settings.uncheckState,
      actionLabel: this.ctx.translate.instant('widgets.toggle-button.uncheck')};
    this.uncheckValueSetter = this.createValueSetter(uncheckStateSettings);

    this.appearance = this.value ? this.settings.checkedAppearance : this.settings.uncheckedAppearance;
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  private onValue(value: boolean): void {
    const newValue = !!value;
    if (this.value !== newValue) {
      this.value = newValue;
      this.appearance = this.value ? this.settings.checkedAppearance : this.settings.uncheckedAppearance;
      this.cd.markForCheck();
    }
  }

  private onDisabled(value: boolean): void {
    const newDisabled = !!value;
    if (this.disabled !== newDisabled) {
      this.disabled = newDisabled;
      this.cd.markForCheck();
    }
  }

  public onClick(_$event: MouseEvent) {
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      this.onValue(!this.value);
      const targetValue = this.value;
      const targetSetter = targetValue ? this.checkValueSetter : this.uncheckValueSetter;
      this.updateValue(targetSetter, targetValue, {
        next: () => {
          this.onValue(targetValue);
        },
        error: () => {
          this.onValue(!targetValue);
        }
      });
    }
  }
}
