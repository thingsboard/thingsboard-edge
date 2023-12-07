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

import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  backgroundStyle,
  overlayStyle,
  BackgroundSettings,
  BackgroundType,
  backgroundTypeTranslations, ComponentStyle
} from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'tb-background-settings-panel',
  templateUrl: './background-settings-panel.component.html',
  providers: [],
  styleUrls: ['./background-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class BackgroundSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  backgroundSettings: BackgroundSettings;

  @Input()
  popover: TbPopoverComponent<BackgroundSettingsPanelComponent>;

  @Output()
  backgroundSettingsApplied = new EventEmitter<BackgroundSettings>();

  backgroundType = BackgroundType;

  backgroundTypes = Object.keys(BackgroundType) as BackgroundType[];

  backgroundTypeTranslationsMap = backgroundTypeTranslations;

  backgroundSettingsFormGroup: UntypedFormGroup;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};

  constructor(private fb: UntypedFormBuilder,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              protected store: Store<AppState>,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit(): void {
    this.backgroundSettingsFormGroup = this.fb.group(
      {
        type: [this.backgroundSettings?.type, []],
        imageUrl: [this.backgroundSettings?.imageUrl, []],
        color: [this.backgroundSettings?.color, []],
        overlay: this.fb.group({
          enabled: [this.backgroundSettings?.overlay?.enabled, []],
          color: [this.backgroundSettings?.overlay?.color, []],
          blur: [this.backgroundSettings?.overlay?.blur, []]
        })
      }
    );
    this.backgroundSettingsFormGroup.get('type').valueChanges.subscribe(() => {
      setTimeout(() => {this.popover?.updatePosition();}, 0);
    });
    this.backgroundSettingsFormGroup.get('overlay').get('enabled').valueChanges.subscribe(() => {
      this.updateValidators();
    });
    this.backgroundSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateBackgroundStyle();
    });
    this.updateValidators();
    this.updateBackgroundStyle();
  }

  cancel() {
    this.popover?.hide();
  }

  applyColorSettings() {
    const backgroundSettings = this.backgroundSettingsFormGroup.getRawValue();
    this.backgroundSettingsApplied.emit(backgroundSettings);
  }

  private updateValidators() {
    const overlayEnabled: boolean = this.backgroundSettingsFormGroup.get('overlay').get('enabled').value;
    if (overlayEnabled) {
      this.backgroundSettingsFormGroup.get('overlay').get('color').enable({emitEvent: false});
      this.backgroundSettingsFormGroup.get('overlay').get('blur').enable({emitEvent: false});
    } else {
      this.backgroundSettingsFormGroup.get('overlay').get('color').disable({emitEvent: false});
      this.backgroundSettingsFormGroup.get('overlay').get('blur').disable({emitEvent: false});
    }
    this.backgroundSettingsFormGroup.get('overlay').get('color').updateValueAndValidity({emitEvent: false});
    this.backgroundSettingsFormGroup.get('overlay').get('blur').updateValueAndValidity({emitEvent: false});
  }

  private updateBackgroundStyle() {
    const background: BackgroundSettings = this.backgroundSettingsFormGroup.value;
    this.backgroundStyle$ = backgroundStyle(background, this.imagePipe, this.sanitizer, true);
    this.overlayStyle = overlayStyle(background.overlay);
    this.cd.markForCheck();
  }

}
