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

import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute } from '@angular/router';
import { AdminService } from '@core/http/admin.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Authority } from '@shared/models/authority.enum';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import {
  AdminSettings,
  MailTemplate,
  MailTemplatesSettings,
  mailTemplateTranslations
} from '@shared/models/settings.models';
import { Operation, Resource } from '@shared/models/security.models';

@Component({
  selector: 'tb-mail-templates',
  templateUrl: './mail-templates.component.html',
  styleUrls: ['./mail-templates.component.scss', './settings-card.scss']
})
export class MailTemplatesComponent extends PageComponent implements OnInit, HasDirtyFlag {

  authState: AuthState = getCurrentAuthState(this.store);

  authUser: AuthUser = this.authState.authUser;

  adminSettings: AdminSettings<MailTemplatesSettings>;

  mailTemplateTypes = [];
  mailTemplateTranslationsMap = mailTemplateTranslations;

  mailTemplate: MailTemplate = MailTemplate.test;
  useSystemMailSettings = false;

  readonly = this.isTenantAdmin() && !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  isDirty = false;

  tinyMceOptions: Record<string, any>;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private adminService: AdminService,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit() {
    this.tinyMceOptions = {
      base_url: '/assets/tinymce',
      suffix: '.min'
    };

    if (this.readonly) {
      this.tinyMceOptions.plugins = [];
      this.tinyMceOptions.menubar = false;
      this.tinyMceOptions.toolbar = false;
      this.tinyMceOptions.statusbar = false;
      this.tinyMceOptions.height = 450;
      this.tinyMceOptions.autofocus = false;
      this.tinyMceOptions.branding = false;
      this.tinyMceOptions.resize = true;
      this.tinyMceOptions.readonly = 1;
      this.tinyMceOptions.setup = (ed) => {
        ed.on('PreInit', () => {
          const document = $(ed.iframeElement.contentDocument);
          const body = $('#tinymce', document);
          body.attr({contenteditable: false});
          body.css('pointerEvents', 'none');
          body.css('userSelect', 'none');
        });
      };
    } else {
      this.tinyMceOptions.plugins = ['link table image imagetools code fullscreen'];
      this.tinyMceOptions.menubar = 'edit insert tools view format table';
      this.tinyMceOptions.toolbar = 'fontselect fontsizeselect | formatselect | bold italic  strikethrough  forecolor backcolor ' +
        '| link | table | image | alignleft aligncenter alignright alignjustify  ' +
        '| numlist bullist outdent indent  | removeformat | code | fullscreen';
      this.tinyMceOptions.height = 450;
      this.tinyMceOptions.autofocus = false;
      this.tinyMceOptions.branding = false;
    }
    this.adminSettings = this.route.snapshot.data.adminSettings;
    this.mailTemplateTypes = Object.keys(MailTemplate).filter(type => Object.keys(this.adminSettings.jsonValue).includes(type));
    if (this.isTenantAdmin()) {
      this.useSystemMailSettings = this.adminSettings.jsonValue.useSystemMailSettings;
    }
  }

  public isTenantAdmin(): boolean {
    return this.authUser.authority === Authority.TENANT_ADMIN;
  }

  save() {
    if (this.isTenantAdmin()) {
      this.adminSettings.jsonValue.useSystemMailSettings = this.useSystemMailSettings;
    }
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.isDirty = false;
      }
    );
  }

}
