///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { catchError, mergeMap, tap } from 'rxjs/operators';
import { WhiteLabelingService } from '@core/http/white-labeling.service';

const NOT_FOUND_CONTENT = '## Not found';

@Injectable({
  providedIn: 'root'
})
export class HelpService {

  private helpCache: {[lang: string]: {[key: string]: string}} = {};

  constructor(
    private translate: TranslateService,
    private wl: WhiteLabelingService,
    private http: HttpClient
  ) {}

  getHelpContent(key: string): Observable<string> {
    const lang = this.translate.currentLang;
    if (this.helpCache[lang] && this.helpCache[lang][key]) {
      return of(this.helpCache[lang][key]);
    } else {
      return this.loadHelpContent(lang, key).pipe(
        catchError(() => {
          const defaultLang = this.translate.getDefaultLang();
          if (lang !== defaultLang) {
            return this.loadHelpContent(defaultLang, key).pipe(
              catchError(() => {
                return of(NOT_FOUND_CONTENT);
              })
            );
          } else {
            return of(NOT_FOUND_CONTENT);
          }
        }),
        mergeMap((content) => {
          return this.processIncludes(this.processVariables(content));
        }),
        tap((content) => {
          let langContent = this.helpCache[lang];
          if (!langContent) {
            langContent = {};
            this.helpCache[lang] = langContent;
          }
          langContent[key] = content;
        })
      );
    }
  }

  private loadHelpContent(lang: string, key: string): Observable<string> {
    return this.http.get(`/assets/help/${lang}/${key}.md`, {responseType: 'text'} );
  }

  private processVariables(content: string): string {
    const baseUrlReg = /\${baseUrl}/g;
    return content.replace(baseUrlReg, this.wl.getHelpLinkBaseUrl());
  }

  private processIncludes(content: string): Observable<string> {
    const includesRule = /{% include (.*) %}/;
    const match = includesRule.exec(content);
    if (match) {
      const key = match[1];
      return this.getHelpContent(key).pipe(
        mergeMap((include) => {
          content = content.replace(match[0], include);
          return this.processIncludes(content);
        })
      );
    } else {
      return of(content);
    }
  }

}
