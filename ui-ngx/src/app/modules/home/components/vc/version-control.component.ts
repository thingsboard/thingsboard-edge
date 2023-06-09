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

import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { selectHasRepository } from '@core/auth/auth.selectors';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { RepositorySettingsComponent } from '@home/components/vc/repository-settings.component';
import { UntypedFormGroup } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { Observable } from 'rxjs';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-version-control',
  templateUrl: './version-control.component.html',
  styleUrls: ['./version-control.component.scss']
})
export class VersionControlComponent implements OnInit, HasConfirmForm {

  @ViewChild('repositorySettingsComponent', {static: false}) repositorySettingsComponent: RepositorySettingsComponent;

  @Input()
  detailsMode = false;

  @Input()
  popoverComponent: TbPopoverComponent;

  @Input()
  active = true;

  @Input()
  singleEntityMode = false;

  @Input()
  externalEntityId: EntityId;

  @Input()
  entityId: EntityId;

  @Input()
  groupType: EntityType;

  @Input()
  entityName: string;

  @Input()
  onBeforeCreateVersion: () => Observable<any>;

  @Output()
  versionRestored = new EventEmitter<void>();

  hasRepository$ = this.store.pipe(select(selectHasRepository));

  constructor(private store: Store<AppState>) {

  }

  ngOnInit() {

  }

  confirmForm(): UntypedFormGroup {
    return this.repositorySettingsComponent?.repositorySettingsForm;
  }

}
