///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { Component } from '@angular/core';
import { ContentType } from '@shared/models/constants';
import { Operation, Resource } from '@shared/models/security.models';
import { ActivatedRoute, Router } from '@angular/router';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { FormBuilder } from '@angular/forms';

@Component({
  selector: 'tb-custom-translation',
  templateUrl: './custom-translation.component.html',
  styleUrls: ['../admin/settings-card.scss', './custom-translation.component.scss']
})
export class CustomTranslationComponent {

  isDirty = false;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  contentType = ContentType;
  mode = this.fb.control('basic');

  translation: object;

  tableFullScreen = false;
  editorFullScreen = false;

  localeCode: string;
  localeName: string;
  countryName: string;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private userPermissionsService: UserPermissionsService,
              private fb: FormBuilder) {
    this.localeCode = this.route.snapshot.paramMap.get('localeCode');
    this.localeName = decodeURIComponent(this.route.snapshot.queryParamMap.get('name'));
    this.countryName = decodeURIComponent(this.route.snapshot.queryParamMap.get('country'));
  }

  goBack() {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  changeTableFullScreen($event: boolean) {
    this.tableFullScreen = $event;
  }

  changeditorFullScreen($event: boolean) {
    this.editorFullScreen = $event;
  }
}
