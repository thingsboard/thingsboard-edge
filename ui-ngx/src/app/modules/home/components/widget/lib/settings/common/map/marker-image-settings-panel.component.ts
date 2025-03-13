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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { WidgetService } from '@core/http/widget.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MarkerImageSettings, MarkerImageType } from '@shared/models/widget/maps/map.models';

@Component({
  selector: 'tb-marker-image-settings-panel',
  templateUrl: './marker-image-settings-panel.component.html',
  providers: [],
  styleUrls: ['./marker-image-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MarkerImageSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  markerImageSettings: MarkerImageSettings;

  @Input()
  popover: TbPopoverComponent<MarkerImageSettingsPanelComponent>;

  @Output()
  markerImageSettingsApplied = new EventEmitter<MarkerImageSettings>();

  MarkerImageType = MarkerImageType;

  markerImageSettingsFormGroup: UntypedFormGroup;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  constructor(private fb: UntypedFormBuilder,
              private widgetService: WidgetService,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.markerImageSettingsFormGroup = this.fb.group(
      {
        type: [this.markerImageSettings?.type || MarkerImageType.image, []],
        image: [this.markerImageSettings?.image, [Validators.required]],
        imageSize: [this.markerImageSettings?.imageSize, [Validators.min(1)]],
        imageFunction: [this.markerImageSettings?.imageFunction, [Validators.required]],
        images: [this.markerImageSettings?.images, []]
      }
    );
    this.markerImageSettingsFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
    this.updateValidators();
  }

  cancel() {
    this.popover?.hide();
  }

  applyMarkerImageSettings() {
    const markerImageSettings: MarkerImageSettings = this.markerImageSettingsFormGroup.value;
    this.markerImageSettingsApplied.emit(markerImageSettings);
  }

  private updateValidators() {
    const type: MarkerImageType = this.markerImageSettingsFormGroup.get('type').value;
    if (type === MarkerImageType.image) {
      this.markerImageSettingsFormGroup.get('image').enable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('imageSize').enable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('imageFunction').disable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('images').disable({emitEvent: false});
    } else {
      this.markerImageSettingsFormGroup.get('image').disable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('imageSize').disable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('imageFunction').enable({emitEvent: false});
      this.markerImageSettingsFormGroup.get('images').enable({emitEvent: false});
    }
  }

}
