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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ContentType } from '@shared/models/constants';
import { isEqual } from '@core/utils';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { coerceBoolean } from '@shared/decorators/coercion';
import { UtilsService } from '@core/services/utils.service';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { environment as env } from '@env/environment';
import { Observable } from 'rxjs';

@Component({
  selector : 'tb-translation-map-advanced',
  templateUrl: './translation-map-advanced.component.html',
  styleUrls: ['./translation-map-advanced.component.scss'],
})
export class TranslationMapAdvancedComponent {

  @Input()
  @coerceBoolean()
  readonly: boolean;

  @Input()
  localeName: string;

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

  private loadTranslations = '';

	constructor(private store: Store<AppState>,
              private utils: UtilsService,
              private customTranslationService: CustomTranslationService,
              private route: ActivatedRoute,
              private fb: FormBuilder,
              private translate: TranslateService) {
    this.localeCode = this.route.snapshot.paramMap.get('localeCode');
    this.translationForm = this.fb.group({
      translation: []
    });

    this.customTranslationService.getCustomTranslation(this.localeCode).subscribe(data => {
      if (!isEqual(data, {})) {
        this.loadTranslations = JSON.stringify(data, null, 2);
        this.translationForm.patchValue({translation: this.loadTranslations});
      }
    });
	}

  toggleFullscreen() {
    this.fullscreen = !this.fullscreen;
    this.changeFullscreen.emit(this.fullscreen);
  }

  save() {
    if (this.validate()) {
      const formValue = this.translationForm.value.translation;
      if (formValue) {
        const parseValue =  JSON.parse(formValue);
        if (isEqual(parseValue, {})) {
          this.deleteTranslation();
        } else {
          this.saveTranslations(parseValue);
        }
      } else {
        this.deleteTranslation();
      }
    }
  }

  private saveTranslations(translations: object) {
    this.customTranslationService.saveCustomTranslation(this.localeCode, translations).subscribe(() => {
      this.loadTranslations = this.translationForm.value.translation;
      this.translationForm.markAsPristine();
      this.translate.currentLoader.getTranslation(this.localeCode).subscribe(value => {
        this.translate.setTranslation(this.localeCode, value, true);
      });
    });
  }

  private deleteTranslation() {
    let removeRequest$: Observable<void>;
    if (env.supportedLangs.includes(this.localeCode)) {
      removeRequest$ = this.customTranslationService.deleteCustomTranslation(this.localeCode);
    } else {
      removeRequest$ = this.customTranslationService.saveCustomTranslation(this.localeCode, {});
    }
    removeRequest$.subscribe(() => {
      this.loadTranslations = '';
      this.translationForm.patchValue({translation: ''});
      this.translationForm.markAsPristine();
      this.translate.currentLoader.getTranslation(this.localeCode).subscribe(value => {
        this.translate.setTranslation(this.localeCode, value, true);
      });
    });
  }

  reset() {
    this.translationForm.reset({translation: this.loadTranslations});
    this.translationForm.markAsPristine();
  }

  private validate(): boolean {
    const translation = this.translationForm.value.translation;
    if (translation) {
      try {
        JSON.parse(translation);
      } catch (e) {
        const details = this.utils.parseException(e);
        let errorInfo = `Error parsing JSON for ${this.localeName} language:`;
        if (details.name) {
          errorInfo += ` ${details.name}:`;
        }
        if (details.message) {
          errorInfo += ` ${details.message}:`;
        }
        this.store.dispatch(new ActionNotificationShow(
          {
            message: errorInfo,
            type: 'error',
            verticalPosition: 'top',
            horizontalPosition: 'left',
            target: 'tb-custom-translation-panel'
          }));
        return false;
      }
    }
    return true;
  }
}
