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

import {
  ChangeDetectorRef,
  Component,
  forwardRef,
  Input,
  OnInit,
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  BackgroundSettings,
  backgroundStyle,
  BackgroundType,
  ComponentStyle,
  overlayStyle, validateAndUpdateBackgroundSettings
} from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  BackgroundSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/background-settings-panel.component';
import { Observable, of } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'tb-background-settings',
  templateUrl: './background-settings.component.html',
  styleUrls: ['./background-settings.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => BackgroundSettingsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class BackgroundSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled = false;

  backgroundType = BackgroundType;

  modelValue: BackgroundSettings;

  backgroundStyle$: Observable<ComponentStyle>;

  overlayStyle: ComponentStyle = {};

  private propagateChange = null;

  constructor(private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private cd: ChangeDetectorRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (this.disabled !== isDisabled) {
      this.disabled = isDisabled;
      this.updateBackgroundStyle();
    }
  }

  writeValue(value: BackgroundSettings): void {
    this.modelValue = validateAndUpdateBackgroundSettings(value);
    this.updateBackgroundStyle();
  }

  openBackgroundSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        backgroundSettings: this.modelValue
      };
     const backgroundSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, BackgroundSettingsPanelComponent, 'left', true, null,
        ctx,
        {},
        {}, {}, true);
      backgroundSettingsPanelPopover.tbComponentRef.instance.popover = backgroundSettingsPanelPopover;
      backgroundSettingsPanelPopover.tbComponentRef.instance.backgroundSettingsApplied.subscribe((backgroundSettings) => {
        backgroundSettingsPanelPopover.hide();
        this.modelValue = backgroundSettings;
        this.updateBackgroundStyle();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateBackgroundStyle() {
    if (!this.disabled) {
      this.backgroundStyle$ = backgroundStyle(this.modelValue, this.imagePipe, this.sanitizer,  true);
      this.overlayStyle = overlayStyle(this.modelValue.overlay);
    } else {
      this.backgroundStyle$ = of({});
      this.overlayStyle = {};
    }
    this.cd.markForCheck();
  }

}
