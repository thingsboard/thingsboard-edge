///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
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
