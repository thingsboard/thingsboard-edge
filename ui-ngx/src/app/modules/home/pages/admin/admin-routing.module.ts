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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterModule, Routes } from '@angular/router';
import { MailServerComponent } from '@modules/home/pages/admin/mail-server.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { SecuritySettingsComponent } from '@modules/home/pages/admin/security-settings.component';
import { MailTemplatesComponent } from '@home/pages/admin/mail-templates.component';
import { Observable } from 'rxjs';
import { AdminSettings, MailTemplatesSettings } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { map } from 'rxjs/operators';
import { isDefined } from '@core/utils';
import { CustomTranslation } from '@shared/models/custom-translation.model';
import { CustomTranslationService } from '@core/http/custom-translation.service';
import { CustomTranslationComponent } from '@home/pages/admin/custom-translation.component';
import { CustomMenuComponent } from '@home/pages/admin/custom-menu.component';
import { CustomMenu } from '@shared/models/custom-menu.models';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { WhiteLabelingComponent } from '@home/pages/admin/white-labeling.component';

@Injectable()
export class MailTemplateSettingsResolver implements Resolve<AdminSettings<MailTemplatesSettings>> {

  constructor(private adminService: AdminService,
              private store: Store<AppState>) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<AdminSettings<MailTemplatesSettings>> {
    return this.adminService.getAdminSettings<MailTemplatesSettings>('mailTemplates', true).pipe(
      map((adminSettings) => {
        let useSystemMailSettings = false;
        if (getCurrentAuthUser(this.store).authority === Authority.TENANT_ADMIN) {
          useSystemMailSettings = isDefined(adminSettings.jsonValue.useSystemMailSettings) ?
            adminSettings.jsonValue.useSystemMailSettings : true;
        }
        adminSettings.jsonValue.useSystemMailSettings = useSystemMailSettings;
        return adminSettings;
      })
    );
  }
}

@Injectable()
export class CustomTranslationResolver implements Resolve<CustomTranslation> {

  constructor(private customTranslationService: CustomTranslationService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<CustomTranslation> {
    return this.customTranslationService.getCurrentCustomTranslation();
  }
}

@Injectable()
export class CustomMenuResolver implements Resolve<CustomMenu> {

  constructor(private customMenuService: CustomMenuService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<CustomMenu> {
    return this.customMenuService.getCurrentCustomMenu();
  }
}

const routes: Routes = [
  {
    path: 'settings',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        label: 'admin.system-settings',
        icon: 'settings'
      }
    },
    children: [
      {
        path: '',
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: {
            SYS_ADMIN: '/settings/outgoing-mail',
            TENANT_ADMIN: '/settings/outgoing-mail',
            CUSTOMER_USER: '/settings/customTranslation'
          }
        }
      },
      {
        path: 'outgoing-mail',
        component: MailServerComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'admin.outgoing-mail-settings',
          breadcrumb: {
            label: 'admin.outgoing-mail',
            icon: 'mail'
          }
        }
      },
      {
        path: 'mail-template',
        component: MailTemplatesComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'admin.mail-template-settings',
          breadcrumb: {
            label: 'admin.mail-templates',
            icon: 'format_shapes'
          }
        },
        resolve: {
          adminSettings: MailTemplateSettingsResolver
        }
      },
      {
        path: 'whiteLabel',
        component: WhiteLabelingComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'white-labeling.white-labeling',
          isLoginWl: false,
          breadcrumb: {
            label: 'white-labeling.white-labeling',
            icon: 'format_paint'
          }
        }
      },
      {
        path: 'loginWhiteLabel',
        component: WhiteLabelingComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'white-labeling.login-white-labeling',
          isLoginWl: true,
          breadcrumb: {
            label: 'white-labeling.login-white-labeling',
            icon: 'format_paint'
          }
        }
      },
      {
        path: 'customTranslation',
        component: CustomTranslationComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'custom-translation.custom-translation',
          breadcrumb: {
            label: 'custom-translation.custom-translation',
            icon: 'language'
          }
        },
        resolve: {
          customTranslation: CustomTranslationResolver
        }
      },
      {
        path: 'customMenu',
        component: CustomMenuComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'custom-menu.custom-menu',
          breadcrumb: {
            label: 'custom-menu.custom-menu',
            icon: 'list'
          }
        },
        resolve: {
          customMenu: CustomMenuResolver
        }
      },
      {
        path: 'security-settings',
        component: SecuritySettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.security-settings',
          breadcrumb: {
            label: 'admin.security-settings',
            icon: 'security'
          }
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    MailTemplateSettingsResolver,
    CustomTranslationResolver,
    CustomMenuResolver
  ]
})
export class AdminRoutingModule { }
