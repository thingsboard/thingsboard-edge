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

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { GpioItem, gpioItemValidator } from '@home/components/widget/lib/settings/gpio/gpio-item.component';

@Component({
  selector: 'tb-gpio-panel-widget-settings',
  templateUrl: './gpio-panel-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class GpioPanelWidgetSettingsComponent extends WidgetSettingsComponent {

  gpioPanelWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.gpioPanelWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ledPanelBackgroundColor: '#008a00',
      gpioList: []
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gpioPanelWidgetSettingsForm = this.fb.group({

      // Panel settings

      ledPanelBackgroundColor: [settings.ledPanelBackgroundColor, [Validators.required]],

      // --> GPIO leds

      gpioList: this.prepareGpioListFormArray(settings.gpioList),

    });
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
    settingsForm.setControl('gpioList', this.prepareGpioListFormArray(settings.gpioList), {emitEvent: false});
  }

  private prepareGpioListFormArray(gpioList: GpioItem[] | undefined): UntypedFormArray {
    const gpioListControls: Array<AbstractControl> = [];
    if (gpioList) {
      gpioList.forEach((gpioItem) => {
        gpioListControls.push(this.fb.control(gpioItem, [gpioItemValidator(true)]));
      });
    }
    return this.fb.array(gpioListControls, [(control: AbstractControl) => {
      const gpioItems = control.value;
      if (!gpioItems || !gpioItems.length) {
        return {
          gpioItems: true
        };
      }
      return null;
    }]);
  }

  gpioListFormArray(): UntypedFormArray {
    return this.gpioPanelWidgetSettingsForm.get('gpioList') as UntypedFormArray;
  }

  public trackByGpioItem(index: number, gpioItemControl: AbstractControl): any {
    return gpioItemControl;
  }

  public removeGpioItem(index: number) {
    (this.gpioPanelWidgetSettingsForm.get('gpioList') as UntypedFormArray).removeAt(index);
  }

  public addGpioItem() {
    const gpioItem: GpioItem = {
      pin: null,
      label: null,
      row: null,
      col: null,
      color: null
    };
    const gpioListArray = this.gpioPanelWidgetSettingsForm.get('gpioList') as UntypedFormArray;
    const gpioItemControl = this.fb.control(gpioItem, [gpioItemValidator(true)]);
    (gpioItemControl as any).new = true;
    gpioListArray.push(gpioItemControl);
    this.gpioPanelWidgetSettingsForm.updateValueAndValidity();
    if (!this.gpioPanelWidgetSettingsForm.valid) {
      this.onSettingsChanged(this.gpioPanelWidgetSettingsForm.value);
    }
  }
}
