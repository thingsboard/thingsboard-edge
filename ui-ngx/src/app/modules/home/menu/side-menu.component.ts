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

import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { MenuService } from '@core/services/menu.service';
import { Observable } from 'rxjs';
import { MenuSection } from '@core/services/menu.models';
import { map, share } from 'rxjs/operators';

@Component({
  selector: 'tb-side-menu',
  templateUrl: './side-menu.component.html',
  styleUrls: ['./side-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SideMenuComponent implements OnInit {

  menuSections$: Observable<Array<MenuSection>>;

  constructor(private menuService: MenuService) {
    this.menuSections$ = this.menuService.menuSections().pipe(
      map((sections) => this.filterSections(sections)),
      share()
    );
  }

  trackByMenuSection(index: number, section: MenuSection){
    return section.id;
  }

  ngOnInit() {
  }

  private filterSections(sections: Array<MenuSection>): Array<MenuSection> {
    const enabledSections = sections.filter(section => !section.disabled);
    const filteredSections: MenuSection[] = [];
    for (const enabledSection of enabledSections) {
      const sectionPages = enabledSection.pages || [];
      if (!sectionPages.length || sectionPages.filter((page) => !page.disabled).length > 0) {
        filteredSections.push(enabledSection);
      }
    }
    return filteredSections;
  }
}
