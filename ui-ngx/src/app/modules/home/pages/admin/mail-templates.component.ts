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
