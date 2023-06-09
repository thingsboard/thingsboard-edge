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
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ContentType } from '@shared/models/constants';
import { CustomMenu } from '@shared/models/custom-menu.models';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { Observable } from 'rxjs';
import { mergeMap, tap } from 'rxjs/operators';

@Component({
  selector: 'tb-custom-menu',
  templateUrl: './custom-menu.component.html',
  styleUrls: ['./settings-card.scss']
})
export class CustomMenuComponent extends PageComponent implements OnInit, HasDirtyFlag {

  isDirty = false;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  contentType = ContentType;

  customMenu: CustomMenu;
  customMenuJson: string;

  menuPlaceholder =
    '******* Example of custom menu ******** \n*\n' +
    '* menuItems - array of custom menu items\n' +
    '* disabledMenuItems - array of ThingsBoard menu items to be disabled, available menu items names:\n*\n' +
    '* "home", "tenants", "alarms", "rule_chains", "dashboards", "dashboard_all", "dashboard_groups", "solution_templates",\n' +
    '* "entities", "devices", "assets", "entity_views", "profiles", "device_profiles", "asset_profiles", "tenant_profiles",\n' +
    '* "customers", "customer_all", "customer_groups", "customers_hierarchy", "users", "user_all", "user_groups",\n' +
    '* "edge_management", "edges", "rulechain_templates", "integration_templates", "converter_templates",\n' +
    '* "integrations_center", "integrations", "converters", "features", "otaUpdates", "version_control", "scheduler",\n' +
    '* "white_labeling", "white_labeling_general", "login_white_labeling", "mail_templates", "custom_translation", "custom_menu",\n' +
    '* "notifications_center", "notification_inbox", "notification_sent", "notification_recipients", "notification_templates",\n' +
    '* "notification_rules", "settings", "general", "home_settings", "mail_server", "notification_settings", "repository_settings",\n' +
    '* "auto_commit_settings", "queues", "security_settings", "security_settings_general", "oauth2", "roles", "self_registration",\n' +
    '* "2fa", "resources", "widget_library", "resources_library", "api_usage", "audit_log"\n\n' +
    JSON.stringify(
      {
        disabledMenuItems: ['home'],
        menuItems: [
          {
            name:'My Custom Menu',
            iconUrl:null,
            materialIcon:'menu',
            iframeUrl:'https://thingsboard.io',
            dashboardId: '<YOUR DASHBOARD ID HERE>',
            hideDashboardToolbar: true,
            setAccessToken:false,
            childMenuItems:[

            ]
          },
          {
            name:'My Custom Menu 2',
            iconUrl:null,
            materialIcon:'menu',
            iframeUrl:'https://thingsboard.io',
            setAccessToken:false,
            childMenuItems:[
              {
                name:'My Child Menu 1',
                iconUrl:null,
                materialIcon:'menu',
                iframeUrl:'https://thingsboard.io',
                setAccessToken:false,
                childMenuItems:[

                ]
              },
              {
                name:'My Child Menu 2',
                iconUrl:null,
                materialIcon:'menu',
                iframeUrl:'https://thingsboard.io',
                setAccessToken:false,
                childMenuItems:[

                ]
              }
            ]
          }
        ]
      }, null, 2
    );

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private customMenuService: CustomMenuService,
              private utils: UtilsService,
              private translate: TranslateService,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit() {
    this.customMenu = this.route.snapshot.data.customMenu;
    this.customMenuJson = this.customMenu ? JSON.stringify(this.customMenu, null, 2) : null;
  }

  getCurrentCustomMenu(): Observable<any> {
    return this.customMenuService.getCurrentCustomMenu().pipe(
      tap((customMenu) => {
        this.customMenu = customMenu;
        this.customMenuJson = this.customMenu ? JSON.stringify(this.customMenu, null, 2) : null;
      })
    );
  }

  save() {
    if (this.parse()) {
      this.customMenuService.saveCustomMenu(this.customMenu).pipe(
        mergeMap(() => this.getCurrentCustomMenu()
      )).subscribe(() => {
        setTimeout(() => {
          this.isDirty = false;
        }, 0);
      })
    }
  }

  private parse(): boolean {
    if (this.customMenuJson) {
      try {
        this.customMenu = JSON.parse(this.customMenuJson);
      } catch (e) {
        const details = this.utils.parseException(e);
        let errorInfo = 'Error parsing JSON for custom menu:';
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
            target: 'tb-custom-menu-panel'
          }));
        return false;
      }
    } else {
      this.customMenu = null;
    }
    return true;
  }
}
