///
/// Copyright © 2016-2021 ThingsBoard, Inc.
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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterModule, Routes } from '@angular/router';
import { MailServerComponent } from '@modules/home/pages/admin/mail-server.component';
import { SmsProviderComponent } from '@home/pages/admin/sms-provider.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { GeneralSettingsComponent } from '@home/pages/admin/general-settings.component';
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
import { SelfRegistrationComponent } from '@home/pages/admin/self-registration.component';
import { OAuth2SettingsComponent } from '@home/pages/admin/oauth2-settings.component';
import { OAuth2Service } from '@core/http/oauth2.service';

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

@Injectable()
export class OAuth2LoginProcessingUrlResolver implements Resolve<string> {

  constructor(private oauth2Service: OAuth2Service) {
  }

  resolve(): Observable<string> {
    return this.oauth2Service.getLoginProcessingUrl();
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
            SYS_ADMIN: '/settings/general',
            TENANT_ADMIN: '/settings/outgoing-mail',
            CUSTOMER_USER: '/settings/customTranslation'
          }
        }
      },
      {
        path: 'general',
        component: GeneralSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.general-settings',
          breadcrumb: {
            label: 'admin.general',
            icon: 'settings_applications'
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
        path: 'sms-provider',
        component: SmsProviderComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'admin.sms-provider-settings',
          breadcrumb: {
            label: 'admin.sms-provider',
            icon: 'sms'
          }
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
      },
      {
        path: 'selfRegistration',
        component: SelfRegistrationComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'self-registration.self-registration',
          breadcrumb: {
            label: 'self-registration.self-registration',
            icon: 'group_add'
          }
        }
      },
      {
        path: 'oauth2',
        component: OAuth2SettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.oauth2.oauth2',
          breadcrumb: {
            label: 'admin.oauth2.oauth2',
            icon: 'security'
          }
        },
        resolve: {
          loginProcessingUrl: OAuth2LoginProcessingUrlResolver
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
    CustomMenuResolver,
    OAuth2LoginProcessingUrlResolver
  ]
})
export class AdminRoutingModule { }
