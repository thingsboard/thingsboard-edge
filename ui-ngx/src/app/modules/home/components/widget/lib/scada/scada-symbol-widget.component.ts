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
  AfterViewInit,
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
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ScadaSymbolObject, ScadaSymbolObjectCallbacks } from '@home/components/widget/lib/scada/scada-symbol.models';
import {
  scadaSymbolWidgetDefaultSettings,
  ScadaSymbolWidgetSettings
} from '@home/components/widget/lib/scada/scada-symbol-widget.models';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';
import { ImageService } from '@core/http/image.service';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { isDefinedAndNotNull, mergeDeep } from '@core/utils';
import { WidgetContext } from '@home/models/widget-component.models';
import { catchError, share } from 'rxjs/operators';
import { MatIconRegistry } from '@angular/material/icon';
import { RafService } from '@core/services/raf.service';

@Component({
  selector: 'tb-scada-symbol-widget',
  templateUrl: './scada-symbol-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './scada-symbol-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolWidgetComponent implements OnInit, AfterViewInit, OnDestroy, ScadaSymbolObjectCallbacks {

  @ViewChild('scadaSymbolShape', {static: false})
  scadaSymbolShape: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  private loadingSubject = new BehaviorSubject(false);
  private settings: ScadaSymbolWidgetSettings;
  private scadaSymbolContent$: Observable<string>;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  loading$ = this.loadingSubject.asObservable().pipe(share());

  scadaSymbolObject: ScadaSymbolObject;
  noScadaSymbol = false;

  constructor(public widgetComponent: WidgetComponent,
              protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              private imageService: ImageService,
              private iconRegistry: MatIconRegistry,
              private raf: RafService,
              protected cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.actionWidget = this;
    this.settings = mergeDeep({} as ScadaSymbolWidgetSettings, scadaSymbolWidgetDefaultSettings, this.ctx.settings || {});

    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;

    if (this.settings.scadaSymbolContent) {
      this.scadaSymbolContent$ = of(this.settings.scadaSymbolContent);
    } else if (this.settings.scadaSymbolUrl) {
      this.scadaSymbolContent$ = this.imageService.getImageString(this.settings.scadaSymbolUrl)
      .pipe(catchError(() => of('empty')));
    } else {
      this.scadaSymbolContent$ = of('empty');
    }
  }

  ngAfterViewInit(): void {
    this.scadaSymbolContent$.subscribe((content) => {
      this.initObject(this.scadaSymbolShape.nativeElement, content);
    });
  }

  ngOnDestroy() {
    if (this.scadaSymbolObject) {
      this.scadaSymbolObject.destroy();
    }
    this.loadingSubject.complete();
    this.loadingSubject.unsubscribe();
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  onScadaSymbolObjectLoadingState(loading: boolean) {
    this.loadingSubject.next(loading);
    this.cd.detectChanges();
  }

  onScadaSymbolObjectError(error: string) {
    this.ctx.showErrorToast(error, 'bottom', 'center', this.ctx.toastTargetId, true);
  }

  onScadaSymbolObjectMessage(message: string) {
    this.ctx.showSuccessToast(message, 3000, 'bottom', 'center', this.ctx.toastTargetId, true);
  }

  private initObject(rootElement: HTMLElement,
                     content: string) {
    const simulated = this.ctx.utilsService.widgetEditMode ||
      this.ctx.isPreview || (isDefinedAndNotNull(this.settings.simulated) ? this.settings.simulated : false);
    if (content.startsWith('<parsererror')) {
      rootElement.innerHTML = content;
    } else if (content === 'empty') {
      this.noScadaSymbol = true;
      this.cd.markForCheck();
    } else {
      this.scadaSymbolObject = new ScadaSymbolObject(rootElement, this.ctx, this.iconRegistry, this.raf,
        content,
        this.settings.scadaSymbolObjectSettings, this, simulated);
    }
  }

}
