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
  ComponentFactory,
  ComponentFactoryResolver,
  ComponentRef,
  ElementRef,
  Inject,
  Injectable,
  Injector,
  Renderer2,
  Type,
  ViewContainerRef
} from '@angular/core';
import { PopoverPlacement, PopoverWithTrigger } from '@shared/components/popover.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { ComponentType } from '@angular/cdk/portal';
import { HELP_MARKDOWN_COMPONENT_TOKEN } from '@shared/components/tokens';
import { CdkOverlayOrigin } from '@angular/cdk/overlay';

@Injectable()
export class TbPopoverService {

  private popoverWithTriggers: PopoverWithTrigger[] = [];

  componentFactory: ComponentFactory<TbPopoverComponent> = this.resolver.resolveComponentFactory(TbPopoverComponent);

  constructor(private resolver: ComponentFactoryResolver,
              @Inject(HELP_MARKDOWN_COMPONENT_TOKEN) private helpMarkdownComponent: ComponentType<any>) {
  }

  hasPopover(trigger: Element): boolean {
    const res = this.findPopoverByTrigger(trigger);
    return res !== null;
  }

  hidePopover(trigger: Element): boolean {
    const component: TbPopoverComponent = this.findPopoverByTrigger(trigger);
    if (component && component.tbVisible) {
      component.hide();
      return true;
    } else {
      return false;
    }
  }

  createPopoverRef(hostView: ViewContainerRef): ComponentRef<TbPopoverComponent> {
    return hostView.createComponent(this.componentFactory);
  }

  displayPopover<T>(trigger: Element, renderer: Renderer2, hostView: ViewContainerRef,
                    componentType: Type<T>, preferredPlacement: PopoverPlacement = 'top', hideOnClickOutside = true,
                    injector?: Injector, context?: any, overlayStyle: any = {}, popoverStyle: any = {}, style?: any,
                    showCloseButton = true): TbPopoverComponent {
    const componentRef = this.createPopoverRef(hostView);
    return this.displayPopoverWithComponentRef(componentRef, trigger, renderer, componentType, preferredPlacement, hideOnClickOutside,
      injector, context, overlayStyle, popoverStyle, style, showCloseButton);
  }

  displayPopoverWithComponentRef<T>(componentRef: ComponentRef<TbPopoverComponent>, trigger: Element, renderer: Renderer2,
                                    componentType: Type<T>, preferredPlacement: PopoverPlacement = 'top',
                                    hideOnClickOutside = true, injector?: Injector, context?: any, overlayStyle: any = {},
                                    popoverStyle: any = {}, style?: any, showCloseButton = true): TbPopoverComponent {
    const component = componentRef.instance;
    this.popoverWithTriggers.push({
      trigger,
      popoverComponent: component
    });
    renderer.removeChild(
      renderer.parentNode(trigger),
      componentRef.location.nativeElement
    );
    const originElementRef = new ElementRef(trigger);
    component.setOverlayOrigin(new CdkOverlayOrigin(originElementRef));
    component.tbPlacement = preferredPlacement;
    component.tbComponentFactory = this.resolver.resolveComponentFactory(componentType);
    component.tbComponentInjector = injector;
    component.tbComponentContext = context;
    component.tbOverlayStyle = overlayStyle;
    component.tbPopoverInnerStyle = popoverStyle;
    component.tbComponentStyle = style;
    component.tbHideOnClickOutside = hideOnClickOutside;
    component.tbShowCloseButton = showCloseButton;
    component.tbVisibleChange.subscribe((visible: boolean) => {
      if (!visible) {
        componentRef.destroy();
      }
    });
    component.tbDestroy.subscribe(() => {
      this.removePopoverByComponent(component);
    });
    component.show();
    return component;
  }

  toggleHelpPopover(trigger: Element, renderer: Renderer2, hostView: ViewContainerRef, helpId = '',
                    helpContent = '',
                    visibleFn: (visible: boolean) => void = () => {},
                    readyFn: (ready: boolean) => void = () => {},
                    preferredPlacement: PopoverPlacement = 'bottom',
                    overlayStyle: any = {}, helpStyle: any = {}) {
    if (this.hasPopover(trigger)) {
      this.hidePopover(trigger);
    } else {
      readyFn(false);
      const injector = Injector.create({
        parent: hostView.injector, providers: []
      });
      const componentRef = hostView.createComponent(this.componentFactory);
      const component = componentRef.instance;
      this.popoverWithTriggers.push({
        trigger,
        popoverComponent: component
      });
      renderer.removeChild(
        renderer.parentNode(trigger),
        componentRef.location.nativeElement
      );
      const originElementRef = new ElementRef(trigger);
      component.tbAnimationState = 'void';
      component.tbOverlayStyle = {...overlayStyle, opacity: '0' };
      component.setOverlayOrigin(new CdkOverlayOrigin(originElementRef));
      component.tbPlacement = preferredPlacement;
      component.tbComponentFactory = this.resolver.resolveComponentFactory(this.helpMarkdownComponent);
      component.tbComponentInjector = injector;
      component.tbComponentContext = {
        helpId,
        helpContent,
        style: helpStyle,
        visible: true
      };
      component.tbHideOnClickOutside = true;
      component.tbVisibleChange.subscribe((visible: boolean) => {
        if (!visible) {
          visibleFn(false);
          componentRef.destroy();
        }
      });
      component.tbDestroy.subscribe(() => {
        this.removePopoverByComponent(component);
      });
      const showHelpMarkdownComponent = () => {
        component.tbOverlayStyle = {...component.tbOverlayStyle, opacity: '1' };
        component.tbAnimationState = 'active';
        component.updatePosition();
        readyFn(true);
        setTimeout(() => {
          component.updatePosition();
        });
      };
      const setupHelpMarkdownComponent = (helpMarkdownComponent: any) => {
        if (helpMarkdownComponent.isMarkdownReady) {
          showHelpMarkdownComponent();
        } else {
          helpMarkdownComponent.markdownReady.subscribe(() => {
            showHelpMarkdownComponent();
          });
        }
      };
      if (component.tbComponentRef) {
        setupHelpMarkdownComponent(component.tbComponentRef.instance);
      } else {
        component.tbComponentChange.subscribe((helpMarkdownComponentRef) => {
          setupHelpMarkdownComponent(helpMarkdownComponentRef.instance);
        });
      }
      component.show();
      visibleFn(true);
    }
  }

  private findPopoverByTrigger(trigger: Element): TbPopoverComponent | null {
    const res = this.popoverWithTriggers.find(val => this.elementsAreEqualOrDescendant(trigger, val.trigger));
    if (res) {
      return res.popoverComponent;
    } else {
      return null;
    }
  }

  private removePopoverByComponent(component: TbPopoverComponent): void {
    const index = this.popoverWithTriggers.findIndex(val => val.popoverComponent === component);
    if (index > -1) {
      this.popoverWithTriggers.splice(index, 1);
    }
  }

  private elementsAreEqualOrDescendant(element1: Element, element2: Element): boolean {
    return element1 === element2 || element1.contains(element2) || element2.contains(element1);
  }
}
