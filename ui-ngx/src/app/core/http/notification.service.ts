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
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  Notification,
  NotificationRequest,
  NotificationRequestInfo,
  NotificationRequestPreview,
  NotificationRule,
  NotificationSettings,
  NotificationTarget,
  NotificationTemplate,
  NotificationType,
  SlackChanelType,
  SlackConversation
} from '@shared/models/notification.models';
import { User } from '@shared/models/user.model';
import { isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(
    private http: HttpClient
  ) {
  }

  public getNotifications(pageLink: PageLink, unreadOnly = false, config?: RequestConfig): Observable<PageData<Notification>> {
    return this.http.get<PageData<Notification>>(`/api/notifications${pageLink.toQuery()}&unreadOnly=${unreadOnly}`,
                                                  defaultHttpOptionsFromConfig(config));
  }

  public deleteNotification(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/notification/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public markNotificationAsRead(id: string, config?: RequestConfig): Observable<void> {
    return this.http.put<void>(`/api/notification/${id}/read`, defaultHttpOptionsFromConfig(config));
  }

  public markAllNotificationsAsRead(config?: RequestConfig): Observable<void> {
    return this.http.put<void>('/api/notifications/read', defaultHttpOptionsFromConfig(config));
  }

  public createNotificationRequest(notification: NotificationRequest, config?: RequestConfig): Observable<NotificationRequest> {
    return this.http.post<NotificationRequest>('/api/notification/request', notification, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationRequestById(id: string, config?: RequestConfig): Observable<NotificationRequest> {
    return this.http.get<NotificationRequest>(`/api/notification/request/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteNotificationRequest(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/notification/request/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationRequestPreview(notification: NotificationRequest, config?: RequestConfig): Observable<NotificationRequestPreview> {
    return this.http.post<NotificationRequestPreview>('/api/notification/request/preview',
                                                       notification, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationRequests(pageLink: PageLink, config?: RequestConfig): Observable<PageData<NotificationRequestInfo>> {
    return this.http.get<PageData<NotificationRequestInfo>>(`/api/notification/requests${pageLink.toQuery()}`,
                                                        defaultHttpOptionsFromConfig(config));
  }

  public getNotificationSettings(config?: RequestConfig): Observable<NotificationSettings> {
    return this.http.get<NotificationSettings>('/api/notification/settings', defaultHttpOptionsFromConfig(config));
  }

  public saveNotificationSettings(notificationSettings: NotificationSettings, config?: RequestConfig): Observable<NotificationSettings> {
    return this.http.post<NotificationSettings>('/api/notification/settings', notificationSettings, defaultHttpOptionsFromConfig(config));
  }

  public listSlackConversations(type: SlackChanelType, config?: RequestConfig): Observable<Array<SlackConversation>> {
    return this.http.get<Array<SlackConversation>>(`/api/notification/slack/conversations?type=${type}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveNotificationRule(notificationRule: NotificationRule, config?: RequestConfig): Observable<NotificationRule> {
    return this.http.post<NotificationRule>('/api/notification/rule', notificationRule, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationRuleById(id: string, config?: RequestConfig): Observable<NotificationRule> {
    return this.http.get<NotificationRule>(`/api/notification/rule/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteNotificationRule(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/notification/rule/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationRules(pageLink: PageLink, config?: RequestConfig): Observable<PageData<NotificationRule>> {
    return this.http.get<PageData<NotificationRule>>(`/api/notification/rules${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public saveNotificationTarget(notificationTarget: NotificationTarget, config?: RequestConfig): Observable<NotificationTarget> {
    return this.http.post<NotificationTarget>('/api/notification/target', notificationTarget, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationTargetById(id: string, config?: RequestConfig): Observable<NotificationTarget> {
    return this.http.get<NotificationTarget>(`/api/notification/target/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteNotificationTarget(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/notification/target/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationTargetsByIds(ids: string[], config?: RequestConfig): Observable<Array<NotificationTarget>> {
    return this.http.get<Array<NotificationTarget>>(`/api/notification/targets?ids=${ids.join(',')}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getNotificationTargets(pageLink: PageLink, type?: NotificationType,
                                config?: RequestConfig): Observable<PageData<NotificationTarget>> {
    let url = `/api/notification/targets${pageLink.toQuery()}`;
    if (isNotEmptyStr(type)) {
      url += `&notificationType=${type}`;
    }
    return this.http.get<PageData<NotificationTarget>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getRecipientsForNotificationTargetConfig(notificationTarget: NotificationTarget, pageLink: PageLink,
                                                  config?: RequestConfig): Observable<PageData<User>> {
    return this.http.post<PageData<User>>(`/api/notification/target/recipients${pageLink.toQuery()}`, notificationTarget,
                                          defaultHttpOptionsFromConfig(config));
  }

  public saveNotificationTemplate(notificationTarget: NotificationTemplate, config?: RequestConfig): Observable<NotificationTemplate> {
    return this.http.post<NotificationTemplate>('/api/notification/template', notificationTarget, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationTemplateById(id: string, config?: RequestConfig): Observable<NotificationTemplate> {
    return this.http.get<NotificationTemplate>(`/api/notification/template/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteNotificationTemplate(id: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/notification/template/${id}`, defaultHttpOptionsFromConfig(config));
  }

  public getNotificationTemplates(pageLink: PageLink, notificationTypes?: NotificationType,
                                  config?: RequestConfig): Observable<PageData<NotificationTemplate>> {
    let url = `/api/notification/templates${pageLink.toQuery()}`;
    if (isDefinedAndNotNull(notificationTypes)) {
      url += `&notificationTypes=${notificationTypes}`;
    }
    return this.http.get<PageData<NotificationTemplate>>(url, defaultHttpOptionsFromConfig(config));
  }
}
