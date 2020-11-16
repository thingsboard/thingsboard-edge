///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, ReplaySubject, Subject } from 'rxjs';
import { CustomTranslation } from '@shared/models/custom-translation.model';
import { TranslateService } from '@ngx-translate/core';
import { map, mergeMap } from 'rxjs/operators';
import { mergeDeep } from '@core/utils';
import { WINDOW } from '@core/services/window.service';
import { DOCUMENT } from '@angular/common';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class CustomTranslationService {

  private updateTranslationSubjects: Subject<any>[] = [];
  private translateLoadObservable: Observable<CustomTranslation> = null;

  private translationMap: {[key: string]: string} = null;

  constructor(
    @Inject(WINDOW) private window: Window,
    @Inject(DOCUMENT) private document: Document,
    private http: HttpClient,
    private translate: TranslateService
  ) {}

  private loadCustomTranslation(): Observable<CustomTranslation> {
    return this.http.get<CustomTranslation>('/api/customTranslation/customTranslation');
  }

  public updateCustomTranslations(forceUpdate?: boolean): Observable<any> {
    const updateCustomTranslationSubject = new ReplaySubject<any>();
    if (this.translateLoadObservable) {
      this.updateTranslationSubjects.push(updateCustomTranslationSubject);
      return updateCustomTranslationSubject.asObservable();
    }
    this.translateLoadObservable = this.translationMap && !forceUpdate
      ? of({ translationMap: this.translationMap})
      : this.loadCustomTranslation();
    this.translateLoadObservable.subscribe((customTranslation) => {
      this.translationMap = customTranslation.translationMap;
      const langKey = this.translate.currentLang;
      let translationMap: {[key: string]: string};
      if (customTranslation.translationMap[langKey]) {
        try {
          translationMap = JSON.parse(customTranslation.translationMap[langKey]);
        } catch (e) {}
      }
      const reloadObservable = forceUpdate ? this.translate.reloadLang(langKey) : of(null);
      reloadObservable.subscribe(() => {
        if (translationMap) {
          this.translate.setTranslation(langKey, translationMap, true);
        } else {
          this.translate.onTranslationChange.emit({ lang: langKey, translations: this.translate.translations[langKey] });
        }
        this.translateLoadObservable = null;
        updateCustomTranslationSubject.next();
        updateCustomTranslationSubject.complete();
        this.updateTranslationSubjects.forEach(subject => {
          subject.next();
        });
        this.updateTranslationSubjects = [];
      });
    },
    (e) => {
      this.translateLoadObservable = null;
      updateCustomTranslationSubject.error(e);
      this.updateTranslationSubjects.forEach(subject => {
        subject.error(e);
      });
      this.updateTranslationSubjects = [];
    });
    return updateCustomTranslationSubject.asObservable();
  }

  public getCurrentCustomTranslation(): Observable<CustomTranslation> {
    return this.http.get<CustomTranslation>('/api/customTranslation/currentCustomTranslation');
  }

  public saveCustomTranslation(customTranslation: CustomTranslation): Observable<any> {
    return this.http.post<CustomTranslation>('/api/customTranslation/customTranslation', customTranslation).pipe(
      mergeMap(() => {
        return this.updateCustomTranslations(true);
      })
    );
  }

  public downloadLocaleJson(langKey: string): Observable<any> {
    const engLocale = './assets/locale/locale.constant-en_US.json';
    return this.http.get<any>(engLocale).pipe(
      mergeMap((engJson) => {
        const targetLocale = `./assets/locale/locale.constant-${langKey}.json`;
        const targetLocaleObservable = langKey === 'en_US' ? of(engJson) : this.http.get<any>(targetLocale);
        return targetLocaleObservable.pipe(
          map((targetJson) => {
            try {
              let localeJson = mergeDeep(engJson, targetJson);
              if (this.translationMap && this.translationMap[langKey]) {
                let translationMap: any;
                try {
                  translationMap = JSON.parse(this.translationMap[langKey]);
                } catch (e) {
                }
                if (translationMap) {
                  localeJson = mergeDeep(localeJson, translationMap);
                }
              }
              const data = JSON.stringify(localeJson, null, 2);
              const fileName = `locale-${langKey}`;
              this.downloadJson(data, fileName);
              return null;
            } catch (e) {
              throw e;
            }
          }
        ));
      }
    ));
  }

  private downloadJson(data: any, filename: string) {
    if (!filename) {
      filename = 'download';
    }
    filename += '.' + 'json';
    const blob = new Blob([data], {type: 'text/json'});
    if (this.window.navigator && this.window.navigator.msSaveOrOpenBlob) {
      this.window.navigator.msSaveOrOpenBlob(blob, filename);
    } else {
      const e = this.document.createEvent('MouseEvents');
      const a = this.document.createElement('a');
      a.download = filename;
      a.href = URL.createObjectURL(blob);
      a.dataset.downloadurl = ['text/json', a.download, a.href].join(':');
      // @ts-ignore
      e.initEvent('click', true, false, this.window,
        0, 0, 0, 0, 0, false, false, false, false, 0, null);
      a.dispatchEvent(e);
    }
  }
}
