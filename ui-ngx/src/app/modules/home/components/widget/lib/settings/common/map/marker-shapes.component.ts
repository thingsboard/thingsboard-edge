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
import { PageComponent } from '@shared/components/page.component';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  createColorMarkerShapeURI,
  MarkerShape, markerShapes,
  tripMarkerShapes
} from '@shared/models/widget/maps/marker-shape.models';
import { Observable } from 'rxjs';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MatIconRegistry } from '@angular/material/icon';
import tinycolor from 'tinycolor2';
import { map, share } from 'rxjs/operators';
import { coerceBoolean } from '@shared/decorators/coercion';

interface MarkerShapeInfo {
  shape: MarkerShape;
  url$: Observable<SafeUrl>;
}

@Component({
  selector: 'tb-marker-shapes',
  templateUrl: './marker-shapes.component.html',
  providers: [],
  styleUrls: ['./marker-shapes.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MarkerShapesComponent extends PageComponent implements OnInit {

  @Input()
  shape: MarkerShape;

  @Input()
  color: string;

  @Input()
  @coerceBoolean()
  trip = false;

  @Input()
  popover: TbPopoverComponent<MarkerShapesComponent>;

  @Output()
  markerShapeSelected = new EventEmitter<MarkerShape>();

  shapes: MarkerShapeInfo[];

  constructor(protected store: Store<AppState>,
              private iconRegistry: MatIconRegistry,
              private domSanitizer: DomSanitizer) {
    super(store);
  }

  ngOnInit(): void {
    this.shapes = (this.trip ? tripMarkerShapes : markerShapes).map((shape) => {
      return {
        shape,
        url$: createColorMarkerShapeURI(this.iconRegistry, this.domSanitizer, shape, tinycolor(this.color)).pipe(
          map((url) => {
            return this.domSanitizer.bypassSecurityTrustUrl(url);
          }),
          share()
        )
      };
    });
  }

  cancel() {
    this.popover?.hide();
  }

  selectShape(shape: MarkerShape) {
    this.markerShapeSelected.emit(shape);
  }
}
