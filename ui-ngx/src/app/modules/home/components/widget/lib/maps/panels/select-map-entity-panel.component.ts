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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UnplacedMapDataItem } from '@home/components/widget/lib/maps/data-layer/latest-map-data-layer';

@Component({
  selector: 'tb-select-map-entity-panel',
  templateUrl: './select-map-entity-panel.component.html',
  providers: [],
  styleUrls: ['./select-map-entity-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class SelectMapEntityPanelComponent implements OnInit {

  @Input()
  entities: UnplacedMapDataItem[];

  @Output()
  entitySelected = new EventEmitter<UnplacedMapDataItem>();

  selectEntityFormGroup: UntypedFormGroup;

  selectedEntity: UnplacedMapDataItem = null;

  constructor(private fb: UntypedFormBuilder,
              private popover: TbPopoverComponent) {
  }

  ngOnInit(): void {
    this.selectEntityFormGroup = this.fb.group(
      {
        entity: ['', Validators.required]
      }
    );
    this.popover.tbDestroy.subscribe(() => {
      this.entitySelected.emit(this.selectedEntity);
    });
  }

  cancel() {
    this.popover.hide();
  }

  selectEntity() {
    this.selectedEntity = this.selectEntityFormGroup.value.entity;
    this.popover.hide();
  }
}
