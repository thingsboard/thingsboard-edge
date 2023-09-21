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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { WidgetFontComponent } from '@home/components/widget/lib/settings/common/widget-font.component';
import { ValueSourceComponent } from '@home/components/widget/lib/settings/common/value-source.component';
import { LegendConfigComponent } from '@home/components/widget/lib/settings/common/legend-config.component';
import {
  ImageCardsSelectComponent,
  ImageCardsSelectOptionDirective
} from '@home/components/widget/lib/settings/common/image-cards-select.component';
import { FontSettingsComponent } from '@home/components/widget/lib/settings/common/font-settings.component';
import { FontSettingsPanelComponent } from '@home/components/widget/lib/settings/common/font-settings-panel.component';
import { ColorSettingsComponent } from '@home/components/widget/lib/settings/common/color-settings.component';
import {
  ColorSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/color-settings-panel.component';
import { CssUnitSelectComponent } from '@home/components/widget/lib/settings/common/css-unit-select.component';
import { DateFormatSelectComponent } from '@home/components/widget/lib/settings/common/date-format-select.component';
import {
  DateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/date-format-settings-panel.component';
import { BackgroundSettingsComponent } from '@home/components/widget/lib/settings/common/background-settings.component';
import {
  BackgroundSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/background-settings-panel.component';
import {
  CountWidgetSettingsComponent
} from "@home/components/widget/lib/settings/common/count-widget-settings.component";

@NgModule({
  declarations: [
    ImageCardsSelectOptionDirective,
    ImageCardsSelectComponent,
    FontSettingsComponent,
    FontSettingsPanelComponent,
    ColorSettingsComponent,
    ColorSettingsPanelComponent,
    CssUnitSelectComponent,
    DateFormatSelectComponent,
    DateFormatSettingsPanelComponent,
    BackgroundSettingsComponent,
    BackgroundSettingsPanelComponent,
    ValueSourceComponent,
    LegendConfigComponent,
    WidgetFontComponent,
    CountWidgetSettingsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule
  ],
  exports: [
    ImageCardsSelectOptionDirective,
    ImageCardsSelectComponent,
    FontSettingsComponent,
    FontSettingsPanelComponent,
    ColorSettingsComponent,
    ColorSettingsPanelComponent,
    CssUnitSelectComponent,
    DateFormatSelectComponent,
    DateFormatSettingsPanelComponent,
    BackgroundSettingsComponent,
    BackgroundSettingsPanelComponent,
    ValueSourceComponent,
    LegendConfigComponent,
    WidgetFontComponent,
    CountWidgetSettingsComponent
  ]
})
export class WidgetSettingsCommonModule {
}
