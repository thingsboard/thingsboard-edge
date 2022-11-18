///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import {
  AdminSettings,
  AutoCommitSettings,
  JwtSettings,
  MailServerSettings,
  RepositorySettings,
  RepositorySettingsInfo,
  SecuritySettings,
  TestSmsRequest,
  UpdateMessage
} from '@shared/models/settings.models';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { tap } from 'rxjs/operators';
import { LoginResponse } from '@shared/models/login.models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  constructor(
    private http: HttpClient,
    private entitiesVersionControlService: EntitiesVersionControlService
  ) { }

  public getAdminSettings<T>(key: string, systemByDefault?: boolean, config?: RequestConfig): Observable<AdminSettings<T>> {
    return this.http.get<AdminSettings<T>>(`/api/admin/settings/${key}?systemByDefault=${systemByDefault ? 'true': 'false'}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveAdminSettings<T>(adminSettings: AdminSettings<T>,
                              config?: RequestConfig): Observable<AdminSettings<T>> {
    return this.http.post<AdminSettings<T>>('/api/admin/settings', adminSettings, defaultHttpOptionsFromConfig(config));
  }

  public sendTestMail(adminSettings: AdminSettings<MailServerSettings>,
                      config?: RequestConfig): Observable<void> {
    return this.http.post<void>('/api/admin/settings/testMail', adminSettings, defaultHttpOptionsFromConfig(config));
  }

  public sendTestSms(testSmsRequest: TestSmsRequest,
                     config?: RequestConfig): Observable<void> {
    return this.http.post<void>('/api/admin/settings/testSms', testSmsRequest, defaultHttpOptionsFromConfig(config));
  }

  public getSecuritySettings(config?: RequestConfig): Observable<SecuritySettings> {
    return this.http.get<SecuritySettings>(`/api/admin/securitySettings`, defaultHttpOptionsFromConfig(config));
  }

  public saveSecuritySettings(securitySettings: SecuritySettings,
                              config?: RequestConfig): Observable<SecuritySettings> {
    return this.http.post<SecuritySettings>('/api/admin/securitySettings', securitySettings,
      defaultHttpOptionsFromConfig(config));
  }

  public getJwtSettings(config?: RequestConfig): Observable<JwtSettings> {
    return this.http.get<JwtSettings>(`/api/admin/jwtSettings`, defaultHttpOptionsFromConfig(config));
  }

  public saveJwtSettings(jwtSettings: JwtSettings, config?: RequestConfig): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/admin/jwtSettings', jwtSettings, defaultHttpOptionsFromConfig(config));
  }

  public getRepositorySettings(config?: RequestConfig): Observable<RepositorySettings> {
    return this.http.get<RepositorySettings>(`/api/admin/repositorySettings`, defaultHttpOptionsFromConfig(config));
  }

  public saveRepositorySettings(repositorySettings: RepositorySettings,
                                config?: RequestConfig): Observable<RepositorySettings> {
    return this.http.post<RepositorySettings>('/api/admin/repositorySettings', repositorySettings,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap(() => {
        this.entitiesVersionControlService.clearBranchList();
      })
    );
  }

  public deleteRepositorySettings(config?: RequestConfig) {
    return this.http.delete('/api/admin/repositorySettings', defaultHttpOptionsFromConfig(config)).pipe(
      tap(() => {
        this.entitiesVersionControlService.clearBranchList();
      })
    );
  }

  public checkRepositoryAccess(repositorySettings: RepositorySettings,
                               config?: RequestConfig): Observable<void> {
    return this.http.post<void>('/api/admin/repositorySettings/checkAccess', repositorySettings, defaultHttpOptionsFromConfig(config));
  }

  public getRepositorySettingsInfo(config?: RequestConfig): Observable<RepositorySettingsInfo> {
    return this.http.get<RepositorySettingsInfo>('/api/admin/repositorySettings/info', defaultHttpOptionsFromConfig(config));
  }

  public getAutoCommitSettings(config?: RequestConfig): Observable<AutoCommitSettings> {
    return this.http.get<AutoCommitSettings>(`/api/admin/autoCommitSettings`, defaultHttpOptionsFromConfig(config));
  }

  public autoCommitSettingsExists(config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>('/api/admin/autoCommitSettings/exists', defaultHttpOptionsFromConfig(config));
  }

  public saveAutoCommitSettings(autoCommitSettings: AutoCommitSettings,
                                config?: RequestConfig): Observable<AutoCommitSettings> {
    return this.http.post<AutoCommitSettings>('/api/admin/autoCommitSettings', autoCommitSettings, defaultHttpOptionsFromConfig(config));
  }

  public deleteAutoCommitSettings(config?: RequestConfig) {
    return this.http.delete('/api/admin/autoCommitSettings', defaultHttpOptionsFromConfig(config));
  }

  public checkUpdates(config?: RequestConfig): Observable<UpdateMessage> {
    return this.http.get<UpdateMessage>(`/api/admin/updates`, defaultHttpOptionsFromConfig(config));
  }
}
