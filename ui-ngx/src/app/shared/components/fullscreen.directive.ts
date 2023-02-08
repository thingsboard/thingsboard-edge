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

import {
  Directive,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  Renderer2,
  SecurityContext,
  SimpleChanges,
  ViewContainerRef
} from '@angular/core';
import { Overlay, OverlayConfig, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';

@Directive({
  selector: '[tb-fullscreen]'
})
export class FullscreenDirective implements OnChanges, OnDestroy {

  fullscreenValue = false;

  private overlayRef: OverlayRef;
  private parentElement: HTMLElement;

  @Input()
  fullscreen: boolean;

  @Input()
  fullscreenElement: HTMLElement;

  @Input()
  fullscreenBackgroundStyle: {[klass: string]: any};

  @Input()
  fullscreenBackgroundImage: SafeStyle | string;

  @Output()
  fullscreenChanged = new EventEmitter<boolean>();

  constructor(public elementRef: ElementRef,
              private renderer: Renderer2,
              private sanitizer: DomSanitizer,
              private viewContainerRef: ViewContainerRef,
              private overlay: Overlay) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    let updateFullscreen = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'fullscreen') {
          updateFullscreen = true;
        }
      }
    }
    if (updateFullscreen) {
      if (this.fullscreen) {
        this.enterFullscreen();
      } else {
        this.exitFullscreen();
      }
    }
  }

  ngOnDestroy(): void {
    if (this.fullscreen) {
      this.exitFullscreen();
    }
  }

  enterFullscreen() {
    const targetElement: HTMLElement = this.fullscreenElement || this.elementRef.nativeElement;
    this.parentElement = targetElement.parentElement;
    this.parentElement.removeChild(targetElement);
    targetElement.classList.add('tb-fullscreen');
    const position = this.overlay.position();
    const config = new OverlayConfig({
      hasBackdrop: false,
      panelClass: 'tb-fullscreen-parent'
    });
    config.minWidth = '100%';
    config.minHeight = '100%';
    config.positionStrategy = position.global().top('0%').left('0%')
      .right('0%').bottom('0%');

    this.overlayRef = this.overlay.create(config);
    this.overlayRef.attach(new EmptyPortal());
    if (this.fullscreenBackgroundStyle) {
      for (const key of Object.keys(this.fullscreenBackgroundStyle)) {
        this.setStyle(this.overlayRef.overlayElement, key, this.fullscreenBackgroundStyle[key]);
      }
    }
    if (this.fullscreenBackgroundImage) {
      this.setStyle(this.overlayRef.overlayElement, 'backgroundImage', this.fullscreenBackgroundImage);
    }
    this.overlayRef.overlayElement.appendChild( targetElement );
    this.fullscreenChanged.emit(true);
  }

  private setStyle(el: any, nameAndUnit: string, value: any): void {
    const [name, unit] = nameAndUnit.split('.');
    let renderValue: string|null =
      this.sanitizer.sanitize(SecurityContext.STYLE, value as{} | string);
    if (renderValue != null) {
      renderValue = renderValue.toString();
    }
    renderValue = renderValue != null && unit ? `${renderValue}${unit}` : renderValue;
    if (renderValue != null) {
      this.renderer.setStyle(this.overlayRef.overlayElement, name, renderValue);
    } else {
      this.renderer.removeStyle(this.overlayRef.overlayElement, name);
    }
  }

  exitFullscreen() {
    const targetElement: HTMLElement = this.fullscreenElement || this.elementRef.nativeElement;
    if (this.parentElement) {
      this.overlayRef.overlayElement.removeChild( targetElement );
      this.parentElement.appendChild(targetElement);
      this.parentElement = null;
    }
    targetElement.classList.remove('tb-fullscreen');
    if (this.elementRef) {
      this.elementRef.nativeElement.classList.remove('tb-fullscreen');
    }
    if (this.overlayRef) {
      this.overlayRef.dispose();
    }
    this.fullscreenChanged.emit(false);
  }
}

class EmptyPortal extends ComponentPortal<TbAnchorComponent> {

  constructor() {
    super(TbAnchorComponent);
  }

}
