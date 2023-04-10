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

import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { environment as env } from '@env/environment';
import { CustomTranslation } from '@shared/models/custom-translation.model';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ContentType } from '@shared/models/constants';

@Component({
  selector: 'tb-custom-translation',
  templateUrl: './custom-translation.component.html',
  styleUrls: ['./settings-card.scss']
})
export class CustomTranslationComponent extends PageComponent implements OnInit, HasDirtyFlag {

  isDirty = false;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  languageList: string[] = env.supportedLangs;

  currentLang = this.languageList[0];

  contentType = ContentType;

  customTranslation: CustomTranslation;

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

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private customTranslationService: CustomTranslationService,
              private utils: UtilsService,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit() {
    this.customTranslation = this.route.snapshot.data.customTranslation;
  }

  save() {
    if (this.validate()) {
      this.customTranslationService.saveCustomTranslation(this.customTranslation).subscribe(() => {
        this.isDirty = false;
      });
    }
  }

  private validate(): boolean {
    for (const lang of Object.keys(this.customTranslation.translationMap)) {
      const translation = this.customTranslation.translationMap[lang];
      if (translation) {
        try {
          JSON.parse(translation);
        } catch (e) {
          const details = this.utils.parseException(e);
          let errorInfo = `Error parsing JSON for ${this.translate.instant(`language.locales.${lang}`)} language:`;
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
      } else {
        delete this.customTranslation.translationMap[lang];
      }
    }
    return true;
  }

  downloadLocaleJson() {
    this.customTranslationService.downloadLocaleJson(this.currentLang).subscribe();
  }
}
