///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ContentType } from '@shared/models/constants';
import { isDefinedAndNotNull, isEqual } from '@core/utils';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector : 'tb-translation-map-advanced',
  templateUrl: './translation-map-advanced.component.html',
  styleUrls: ['./translation-map-advanced.component.scss'],
})
export class TranslationMapAdvancedComponent {

  @Input()
  @coerceBoolean()
  readonly: boolean;

  @Output()
  changeFullscreen = new EventEmitter<boolean>();

  fullscreen = false;
  ContentType = ContentType;

  translationForm: FormGroup;

  translationPlaceholder =
    '******* Example of custom translation ******** \n\n' +
    JSON.stringify(
      {
        home: {
          home: 'My Home'
        },
        custom: {
          'my-dashboard': {
            title: 'This is custom dashboard title',
            comment: 'You can use it in your dashboard title using the following expression: {i18n:custom.my-dashboard.title}'
          },
          'my-widget': {
            'legend-text': 'This is custom legend text',
            comment: 'You can use it in your dashboard widgets using the following expression: {i18n:custom.my-widget.legend-text}'
          }
        }
      },
      null, 2
    );

  private readonly localeCode: string;

	constructor(private customTranslationService: CustomTranslationService,
              private route: ActivatedRoute,
              private fb: FormBuilder,
              private translate: TranslateService) {
    this.localeCode = this.route.snapshot.paramMap.get('localeCode');
    this.translationForm = this.fb.group({
      translation: []
    });

    this.customTranslationService.getCustomTranslation(this.localeCode).subscribe(data => {
      if (!isEqual(data, {})) {
        this.translationForm.patchValue({translation: JSON.stringify(data, null, 2)});
      }
    });
	}

  toggleFullscreen() {
    this.fullscreen = !this.fullscreen;
    this.changeFullscreen.emit(this.fullscreen);
  }

  changeContent() {
    this.translationForm.get('translation').updateValueAndValidity();
    if (this.translationForm.valid) {
      this.customTranslationService.saveCustomTranslation(this.localeCode, JSON.parse(this.translationForm.value.translation))
        .subscribe(() => {
          if (this.translate.currentLang === this.localeCode) {
            this.translate.reloadLang(this.localeCode).subscribe(() => {
              this.translate.use(this.localeCode);
            });
          } else if (isDefinedAndNotNull(this.translate.translations[this.localeCode])) {
            this.translate.resetLang(this.localeCode);
          }
        });
    }
  }
}
