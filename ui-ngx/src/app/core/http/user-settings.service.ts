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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DocumentationLink, DocumentationLinks,
  GettingStarted,
  initialUserSettings,
  UserSettings,
  UserSettingsType
} from '@shared/models/user-settings.models';
import { map } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';

@Injectable({
  providedIn: 'root'
})
export class UserSettingsService {

  constructor(
    private http: HttpClient
  ) {}

  public loadUserSettings(): Observable<UserSettings> {
    return this.http.get<UserSettings>('/api/user/settings', defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true})).pipe(
      map((settings) => (!settings || !Object.keys(settings).length) ? initialUserSettings : settings)
    );
  }

  public saveUserSettings(userSettings: UserSettings): Observable<UserSettings> {
    return this.http.post<UserSettings>('/api/user/settings', userSettings,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}));
  }

  public putUserSettings(userSettingsData: Partial<UserSettings>): Observable<void> {
    return this.http.put<void>('/api/user/settings', userSettingsData,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}));
  }

  public deleteUserSettings(paths: string[]) {
    return this.http.delete(`/api/user/settings/${paths.join(',')}`,
      defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true}));
  }

  public getDocumentationLinks(config?: RequestConfig): Observable<DocumentationLinks> {
    return this.http.get<DocumentationLinks>(`/api/user/settings/${UserSettingsType.DOC_LINKS}`,
      defaultHttpOptionsFromConfig(config));
  }

  public updateDocumentationLinks(documentationLinks: DocumentationLinks, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/user/settings/${UserSettingsType.DOC_LINKS}`, documentationLinks,
      defaultHttpOptionsFromConfig(config));
  }

  public getGettingStarted(config?: RequestConfig): Observable<GettingStarted> {
    return this.http.get<GettingStarted>(`/api/user/settings/${UserSettingsType.GETTING_STARTED}`,
      defaultHttpOptionsFromConfig(config));
  }

  public updateGettingStarted(gettingStarted: GettingStarted, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/user/settings/${UserSettingsType.GETTING_STARTED}`, gettingStarted,
      defaultHttpOptionsFromConfig(config));
  }

}
