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

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable, of, ReplaySubject, Subject } from 'rxjs';
import { CustomTranslation } from '@shared/models/custom-translation.model';
import { TranslateService } from '@ngx-translate/core';
import { map, mergeMap, tap } from 'rxjs/operators';
import { isEqual, isObject, mergeDeep, removeEmptyObjects } from '@core/utils';
import { WINDOW } from '@core/services/window.service';
import { DOCUMENT } from '@angular/common';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class CustomTranslationService {

  private updateTranslationSubjects: Subject<void>[] = [];
  private translateLoadObservable: Observable<CustomTranslation> = null;

  private translationMap: {[key: string]: string} = null;
  private prevTranslationMap: {[key: string]: string} = null;
  private modifyLang = new Set<string>();

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
    const updateCustomTranslationSubject = new ReplaySubject<void>();
    if (this.translateLoadObservable) {
      this.updateTranslationSubjects.push(updateCustomTranslationSubject);
      return updateCustomTranslationSubject.asObservable();
    }
    this.translateLoadObservable = this.translationMap && !forceUpdate
      ? of(null)
      : this.loadCustomTranslation();
    this.translateLoadObservable.subscribe((customTranslation) => {
      let changeCustomTranslation = false;
      if (customTranslation) {
        changeCustomTranslation = !isEqual(this.translationMap, customTranslation.translationMap);
        this.prevTranslationMap = changeCustomTranslation ? this.translationMap : null;
        this.translationMap = customTranslation.translationMap;
      }
      const langKey = this.translate.currentLang;
      let translationMap: {[key: string]: string};
      if (this.translationMap[langKey]) {
        try {
          translationMap = JSON.parse(this.translationMap[langKey]);
        } catch (e) {}
      }
      const reloadObservable = forceUpdate && changeCustomTranslation ? this.resetTranslation() : of(null);
      reloadObservable.subscribe(() => {
        if (translationMap) {
          this.translate.setTranslation(langKey, translationMap, true);
          this.modifyLang.add(langKey);
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

  private deletePropertyWithObject(source: object, removeObject: object) {
    for (const key of Object.keys(removeObject)) {
      if (isObject(removeObject[key]) && isObject(source[key])) {
        this.deletePropertyWithObject(source[key], removeObject[key]);
      }
      delete source[key];
    }
  }

  private resetTranslation(): Observable<any> {
    if (!this.modifyLang.size) {
      return of(null);
    }
    const tasks = [];
    const originalLangTranslations = Array.from(this.modifyLang.values());
    originalLangTranslations.forEach((lang) => {
      tasks.push(this.http.get<any>(`./assets/locale/locale.constant-${lang}.json`));
    });
    return forkJoin(tasks).pipe(tap(translations => {
      originalLangTranslations.forEach((lang, index) => {
        this.deletePropertyWithObject(this.translate.translations[lang], JSON.parse(this.prevTranslationMap[lang]));
        removeEmptyObjects(this.translate.translations[lang]);
        this.translate.setTranslation(lang, translations[index], true);
      });
      this.modifyLang.clear();
      this.prevTranslationMap = null;
    }));
  }

  public getCurrentCustomTranslation(): Observable<CustomTranslation> {
    return this.http.get<CustomTranslation>('/api/customTranslation/currentCustomTranslation');
  }

  public saveCustomTranslation(customTranslation: CustomTranslation): Observable<any> {
    return this.http.post<CustomTranslation>('/api/customTranslation/customTranslation', customTranslation).pipe(
      mergeMap(() => this.updateCustomTranslations(true))
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
    if (this.window.navigator && (this.window.navigator as any).msSaveOrOpenBlob) {
      (this.window.navigator as any).msSaveOrOpenBlob(blob, filename);
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
