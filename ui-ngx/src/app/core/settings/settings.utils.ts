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

import { environment as env } from '@env/environment';
import { TranslateService } from '@ngx-translate/core';
import { mergeMap } from 'rxjs/operators';
import _moment from 'moment';
import { Observable } from 'rxjs';

export function updateUserLang(translate: TranslateService, document: Document, userLang: string,
                               translations = env.supportedLangs, reload = false): Observable<any> {
  let targetLang = userLang;
  if (!translations) {
    translations = env.supportedLangs;
  }
  if (!env.production) {
    console.log(`User lang: ${targetLang}`);
  }
  if (!targetLang) {
    targetLang = translate.getBrowserCultureLang();
    if (!env.production) {
      console.log(`Fallback to browser lang: ${targetLang}`);
    }
  }
  const detectedSupportedLang = detectSupportedLang(targetLang, translations);
  if (!env.production) {
    console.log(`Detected supported lang: ${detectedSupportedLang}`);
  }
  document.documentElement.lang = detectedSupportedLang.replace('_', '-');
  _moment.locale([detectedSupportedLang]);
  if (reload) {
    translate.addLangs(translations);
    if (translate.translations[detectedSupportedLang]) {
      return translate.currentLoader.getTranslation(detectedSupportedLang).pipe(
        mergeMap((value) => {
          translate.setTranslation(detectedSupportedLang, value, true);
          if (translate.currentLang !== detectedSupportedLang) {
            const currentLanguage = translate.currentLang;
            translate.currentLoader.getTranslation(currentLanguage).subscribe(currentLangValue => {
              translate.setTranslation(currentLanguage, currentLangValue, true);
            });
          }
          return translate.use(detectedSupportedLang);
        })
      );
    } else {
      return translate.use(detectedSupportedLang);
    }
  } else {
    if (detectedSupportedLang === env.defaultLang && translate.translations[detectedSupportedLang]) {
      return translate.currentLoader.getTranslation(detectedSupportedLang).pipe(
        mergeMap((value) => {
          translate.setTranslation(detectedSupportedLang, value, true);
          return translate.use(detectedSupportedLang);
        })
      );
    } else {
      return translate.use(detectedSupportedLang);
    }
  }
}

function detectSupportedLang(targetLang: string, translations: string[]): string {
  const langTag = (targetLang || '').split('-').join('_');
  if (langTag.length) {
    if (translations.indexOf(langTag) > -1) {
      return langTag;
    } else {
      const parts = langTag.split('_');
      let lang: string;
      if (parts.length === 2) {
        lang = parts[0];
      } else {
        lang = langTag;
      }
      const foundLangs = translations.filter(
        (supportedLang: string) => {
          const supportedLangParts = supportedLang.split('_');
          return supportedLangParts[0] === lang;
        }
      );
      if (foundLangs.length) {
        return foundLangs[0];
      }
    }
  }
  return env.defaultLang;
}
