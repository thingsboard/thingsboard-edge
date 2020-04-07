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

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityGroupStateInfo, GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-group-entities-table',
  templateUrl: './group-entities-table.component.html',
  styleUrls: ['./group-entities-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupEntitiesTableComponent extends PageComponent implements AfterViewInit, OnInit, OnDestroy, OnChanges {

  @Input()
  entityGroup: EntityGroupStateInfo<BaseData<HasId>>;

  entityGroupConfig: GroupEntityTableConfig<BaseData<HasId>>;

  private rxSubscriptions = new Array<Subscription>();

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              public translate: TranslateService,
              public dialog: MatDialog) {
    super(store);
  }


  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'entityGroup' && change.currentValue) {
          this.init(change.currentValue);
        }
      }
    }
  }

  ngOnInit(): void {
    if (this.entityGroup) {
      this.init(this.entityGroup);
    } else {
      this.rxSubscriptions.push(this.route.data.subscribe(
        (data) => {
          this.init(data.entityGroup);
        }
      ));
    }
  }

  ngOnDestroy(): void {
    this.rxSubscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.rxSubscriptions.length = 0;
  }

  private init(entityGroup: EntityGroupStateInfo<BaseData<HasId>>) {
    this.entityGroup = entityGroup;
    this.entityGroupConfig = entityGroup.entityGroupConfig;
  }

  ngAfterViewInit(): void {
  }

}
