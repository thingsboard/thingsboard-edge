///
/// Copyright © 2016-2021 The Thingsboard Authors
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
