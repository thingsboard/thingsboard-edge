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

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CircleSettings,
  defaultCircleSettings,
  defaultMapProviderSettings,
  defaultPolygonSettings,
  defaultTripAnimationCommonSettings,
  defaultTripAnimationMarkersSettings,
  defaultTripAnimationPathSettings,
  defaultTripAnimationPointSettings,
  defaultTripAnimationSettings,
  MapProviderSettings,
  PointsSettings,
  PolygonSettings,
  PolylineSettings,
  TripAnimationCommonSettings,
  TripAnimationMarkerSettings
} from 'src/app/modules/home/components/widget/lib/maps-legacy/map-models';
import { extractType } from '@core/utils';

@Component({
  selector: 'tb-trip-animation-widget-settings',
  templateUrl: './trip-animation-widget-settings.component.html',
  styleUrls: ['./../../widget-settings.scss']
})
export class TripAnimationWidgetSettingsComponent extends WidgetSettingsComponent {

  tripAnimationWidgetSettingsForm: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  protected settingsForm(): UntypedFormGroup {
    return this.tripAnimationWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return defaultTripAnimationSettings;
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.tripAnimationWidgetSettingsForm = this.fb.group({
      mapProviderSettings: [settings.mapProviderSettings, []],
      commonMapSettings: [settings.commonMapSettings, []],
      markersSettings: [settings.markersSettings, []],
      pathSettings: [settings.pathSettings, []],
      pointSettings: [settings.pointSettings, []],
      polygonSettings: [settings.polygonSettings, []],
      circleSettings: [settings.circleSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const mapProviderSettings = extractType<MapProviderSettings>(settings, Object.keys(defaultMapProviderSettings) as (keyof MapProviderSettings)[]);
    const commonMapSettings = extractType<TripAnimationCommonSettings>(settings, Object.keys(defaultTripAnimationCommonSettings) as (keyof TripAnimationCommonSettings)[]);
    const markersSettings = extractType<TripAnimationMarkerSettings>(settings, Object.keys(defaultTripAnimationMarkersSettings) as (keyof TripAnimationMarkerSettings)[]);
    const pathSettings = extractType<PolylineSettings>(settings, Object.keys(defaultTripAnimationPathSettings) as (keyof PolylineSettings)[]);
    const pointSettings = extractType<PointsSettings>(settings, Object.keys(defaultTripAnimationPointSettings) as (keyof PointsSettings)[]);
    const polygonSettings = extractType<PolygonSettings>(settings, Object.keys(defaultPolygonSettings) as (keyof PolygonSettings)[]);
    const circleSettings = extractType<CircleSettings>(settings, Object.keys(defaultCircleSettings) as (keyof CircleSettings)[]);
    return {
      mapProviderSettings,
      commonMapSettings,
      markersSettings,
      pathSettings,
      pointSettings,
      polygonSettings,
      circleSettings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.mapProviderSettings,
      ...settings.commonMapSettings,
      ...settings.markersSettings,
      ...settings.pathSettings,
      ...settings.pointSettings,
      ...settings.polygonSettings,
      ...settings.circleSettings,
    };
  }
}
