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

import { Title } from '@angular/platform-browser';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { MenuService } from '@core/services/menu.service';
import { MenuSection } from '@core/services/menu.models';
import { UtilsService } from '@core/services/utils.service';

@Injectable({
  providedIn: 'root'
})
export class TitleService {
  constructor(
    private translate: TranslateService,
    private menuService: MenuService,
    private utils: UtilsService,
    private whiteLabelingService: WhiteLabelingService,
    private title: Title
  ) {}

  setTitle(
    snapshot: ActivatedRouteSnapshot,
    lazyTranslate?: TranslateService
  ) {
    let lastChild = snapshot;
    while (lastChild.children.length) {
      lastChild = lastChild.children[0];
    }
    const { title, customTitle, customChildTitle } = lastChild.data;

    let section: MenuSection = null;
    if (customTitle || customChildTitle) {
      section = customChildTitle ? this.menuService.getCurrentCustomChildSection() : this.menuService.getCurrentCustomSection();
    }
    if (section) {
      const customSectionTitle = this.utils.customTranslation(section.name, section.name);
      this.title.setTitle(`${this.whiteLabelingService.appTitle()} | ${customSectionTitle}`);
    } else {
      const translate = lazyTranslate || this.translate;
      if (title) {
        translate
          .get(title)
          .pipe(filter(translatedTitle => translatedTitle !== title))
          .subscribe(translatedTitle =>
            this.title.setTitle(`${this.whiteLabelingService.appTitle()} | ${translatedTitle}`)
          );
      } else {
        this.title.setTitle(this.whiteLabelingService.appTitle());
      }
    }
  }
}
