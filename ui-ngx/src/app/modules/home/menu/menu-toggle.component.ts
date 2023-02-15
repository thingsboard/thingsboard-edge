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

import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { Observable, combineLatest } from 'rxjs';
import { distinctUntilChanged, filter, map, share, startWith } from 'rxjs/operators';
import { MenuService } from '@core/services/menu.service';
import { UtilsService } from '@core/services/utils.service';
import { ActivationEnd, Router } from '@angular/router';

@Component({
  selector: 'tb-menu-toggle',
  templateUrl: './menu-toggle.component.html',
  styleUrls: ['./menu-toggle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MenuToggleComponent implements OnInit {

  @Input() section: MenuSection;

  sectionPages$: Observable<Array<MenuSection>>;
  sectionHeight$: Observable<string>;

  constructor(public utils: UtilsService,
              private menuService: MenuService,
              private router: Router) {
  }

  ngOnInit() {
    this.sectionPages$ = this.section.asyncPages.pipe(
      map((pages) => {
          return pages.filter((page) => !page.disabled);
        }
      ),
      share()
    );

    this.sectionHeight$ = combineLatest([
      this.sectionPages$,
      this.router.events.pipe(filter(event => event instanceof ActivationEnd), startWith(ActivationEnd))
    ]).pipe(
      map((pages) => {
        if (this.sectionActive()) {
          return pages[0].length * 40 + 'px';
        }
        return '0px';
      }),
      distinctUntilChanged(),
      share()
    );
  }

  sectionActive(): boolean {
    return  this.menuService.sectionActive(this.section);
  }

  trackBySectionPages(index: number, section: MenuSection){
    return section.id;
  }
}
