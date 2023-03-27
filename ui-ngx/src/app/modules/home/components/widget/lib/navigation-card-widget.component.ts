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

import { PageComponent } from '@shared/components/page.component';
import { Component, Input, NgZone, OnInit } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { UtilsService } from '@core/services/utils.service';

interface NavigationCardWidgetSettings {
  name: string;
  icon: string;
  path: string;
}

@Component({
  selector: 'tb-navigation-card-widget',
  templateUrl: './navigation-card-widget.component.html',
  styleUrls: ['./navigation-card-widget.component.scss']
})
export class NavigationCardWidgetComponent extends PageComponent implements OnInit {

  settings: NavigationCardWidgetSettings;

  translatedName: string;

  @Input()
  ctx: WidgetContext;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private ngZone: NgZone,
              private router: Router) {
    super(store);
  }

  ngOnInit(): void {
    this.ctx.$scope.navigationCardWidget = this;
    this.settings = this.ctx.settings;
    this.translatedName = this.utils.customTranslation(this.settings.name, this.settings.name);
  }


  navigate($event: Event, path: string) {
    $event.preventDefault();
    this.ngZone.run(() => {
      this.router.navigateByUrl(path);
    });
  }

}
