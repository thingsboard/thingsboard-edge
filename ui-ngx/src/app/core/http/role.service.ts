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
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { forkJoin, Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { Role } from '@shared/models/role.models';
import { RoleType } from '@shared/models/security.models';
import { GroupPermission, GroupPermissionInfo } from '@shared/models/group-permission.models';

@Injectable({
  providedIn: 'root'
})
export class RoleService {

  constructor(
    private http: HttpClient
  ) { }

  public getRoles(pageLink: PageLink, type?: RoleType, config?: RequestConfig): Observable<PageData<Role>> {
    let url = `/api/roles${pageLink.toQuery()}`;
    if (type) {
      url += `&type=${type}`;
    }
    return this.http.get<PageData<Role>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getRolesByIds(roleIds: Array<string>, config?: RequestConfig): Observable<Array<Role>> {
    return this.http.get<Array<Role>>(`/api/roles?roleIds=${roleIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((roles) => sortEntitiesByIds(roles, roleIds))
    );
  }

  public getRole(roleId: string, config?: RequestConfig): Observable<Role> {
    return this.http.get<Role>(`/api/role/${roleId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveRole(role: Role, config?: RequestConfig): Observable<Role> {
    return this.http.post<Role>('/api/role', role, defaultHttpOptionsFromConfig(config));
  }

  public deleteRole(roleId: string, config?: RequestConfig) {
    return this.http.delete(`/api/role/${roleId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveGroupPermission(groupPermission: GroupPermission, config?: RequestConfig): Observable<GroupPermission> {
    return this.http.post<GroupPermission>('/api/groupPermission', groupPermission, defaultHttpOptionsFromConfig(config));
  }

  public deleteGroupPermissions(groupPermissionIds: string[], config?: RequestConfig): Observable<any> {
    const observables = groupPermissionIds.map((groupPermissionId) => this.deleteGroupPermission(groupPermissionId, config));
    return forkJoin(observables);
  }

  public deleteGroupPermission(groupPermissionId: string, config?: RequestConfig): Observable<any> {
    return this.http.delete(`/api/groupPermission/${groupPermissionId}`, defaultHttpOptionsFromConfig(config));
  }

  public getUserGroupPermissions(userGroupId: string, config?: RequestConfig): Observable<Array<GroupPermissionInfo>> {
    return this.http.get<Array<GroupPermissionInfo>>(`/api/userGroup/${userGroupId}/groupPermissions`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupPermissions(entityGroupId: string, config?: RequestConfig): Observable<Array<GroupPermissionInfo>> {
    return this.http.get<Array<GroupPermissionInfo>>(`/api/entityGroup/${entityGroupId}/groupPermissions`,
      defaultHttpOptionsFromConfig(config));
  }

  public getGroupPermissionInfo(groupPermissionId: string, isUserGroup: boolean, config?: RequestConfig): Observable<GroupPermissionInfo> {
    return this.http.get<GroupPermissionInfo>(`/api/groupPermission/info/${groupPermissionId}?isUserGroup=${isUserGroup}`,
      defaultHttpOptionsFromConfig(config));
  }

  public loadUserGroupPermissionInfos(groupPermissions: Array<GroupPermission>,
                                      config?: RequestConfig): Observable<Array<GroupPermissionInfo>> {
    return this.http.post<Array<GroupPermissionInfo>>('/api/userGroup/groupPermissions/info', groupPermissions,
      defaultHttpOptionsFromConfig(config));
  }
}
