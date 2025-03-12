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

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {
  createMap,
  mapWidgetDefaultSettings,
  MapWidgetSettings
} from '@home/components/widget/lib/maps/map-widget.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { Observable } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';
import { TbMap } from '@home/components/widget/lib/maps/map';
import { MapSetting } from '@shared/models/widget/maps/map.models';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'tb-map-widget',
  templateUrl: './map-widget.component.html',
  styleUrls: ['./map-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MapWidgetComponent implements OnInit, OnDestroy {

  @ViewChild('mapElement', {static: false})
  mapElement: ElementRef<HTMLElement>;

  settings: MapWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  private map: TbMap<MapSetting>;

  constructor(public widgetComponent: WidgetComponent,
              private imagePipe: ImagePipe,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.mapWidget = this;
    this.settings = {...mapWidgetDefaultSettings, ...this.ctx.settings};

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;
  }

  ngOnDestroy() {
    if (this.map) {
      this.map.destroy();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
    this.map = createMap(this.ctx, this.settings, this.mapElement.nativeElement);
  }

}
