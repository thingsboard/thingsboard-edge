///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
  booleanAttribute,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  forwardRef,
  inject,
  Input,
  numberAttribute,
  OnInit,
  Output,
  Renderer2,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Observable } from 'rxjs';
import {
  CustomMobilePage,
  DefaultMobilePage,
  defaultMobilePageMap,
  hideDefaultMenuItems,
  MobilePageType,
  mobilePageTypeTranslations
} from '@shared/models/mobile-app.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { deepClone } from '@core/utils';
import { TbPopoverService } from '@shared/components/popover.service';
import { CustomMobilePagePanelComponent } from '@home/pages/mobile/bundes/layout/custom-mobile-page-panel.component';
import { DefaultMobilePagePanelComponent } from '@home/pages/mobile/bundes/layout/default-mobile-page-panel.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-mobile-menu-item-row',
  templateUrl: './mobile-page-item-row.component.html',
  styleUrls: ['./mobile-page-item-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MobilePageItemRowComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MobilePageItemRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class MobilePageItemRowComponent implements ControlValueAccessor, OnInit, Validator {

  @Input({transform: booleanAttribute})
  disabled: boolean;

  @Input({transform: numberAttribute})
  maxIconNameBlockWidth = 256;

  @Input()
  hideItems: Observable<void>;

  @Output()
  pageRemoved = new EventEmitter();

  iconNameBlockWidth = '256px';
  itemInfo: string;

  isDefaultMenuItem = false;
  isCustomMenuItem = false;

  isCleanupEnabled = false;

  mobilePageRowForm = this.fb.group({
    visible: [true, []],
    icon: ['', []],
    label: ['', [Validators.pattern(/\S/), Validators.maxLength(255)]],
    type: [MobilePageType.DEFAULT]
  });

  private propagateChange = (_val: any) => {};
  private destroyRef = inject(DestroyRef);

  private defaultMobilePages: Omit<DefaultMobilePage, 'type' | 'visible'>;
  private defaultItemName: string;

  private modelValue: DefaultMobilePage | CustomMobilePage;

  constructor(private fb: FormBuilder,
              private cd: ChangeDetectorRef,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private translate: TranslateService) {
    this.mobilePageRowForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(
      () => this.updateModel()
    );
  }

  ngOnInit() {
    this.updateIconNameBlockWidth();
    if (this.hideItems) {
      this.hideItems.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        if (!this.disabled && this.modelValue.visible) {
          this.mobilePageRowForm.patchValue(
            {visible: false}, {emitEvent: true}
          );
        }
      });
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.mobilePageRowForm.disable({emitEvent: false});
    } else {
      this.mobilePageRowForm.enable({emitEvent: false});
    }
  }

  validate(): ValidationErrors | null {
    if (!this.mobilePageRowForm.valid) {
      return {
        invalidMobileMenuItem: true
      };
    }
    return null;
  }

  writeValue(value: DefaultMobilePage | CustomMobilePage) {
    this.modelValue = value;
    this.mobilePageRowForm.patchValue(
      {
        visible: value.visible,
        icon: value.icon,
        label: value.label
      }, {emitEvent: false}
    );
    if (this.modelValue.type === MobilePageType.DEFAULT) {
      this.isDefaultMenuItem = true;
      const defaultPage = this.modelValue as DefaultMobilePage;
      if (defaultMobilePageMap.has(defaultPage.id)) {
        this.defaultMobilePages = defaultMobilePageMap.get(defaultPage.id);
        this.defaultItemName = this.defaultMobilePages.label;
        if (!value.icon) {
          this.mobilePageRowForm.patchValue({
            icon: this.defaultMobilePages.icon
          }, {emitEvent: false});
        }
      }
    } else {
      this.isCustomMenuItem = true;
      this.mobilePageRowForm.get('label').addValidators([Validators.required]);
      this.mobilePageRowForm.get('label').updateValueAndValidity({emitEvent: false});
    }
    this.updateCleanupState();
    this.updateItemInfo();
    this.cd.markForCheck();
  }

  cleanup() {
    const visible = !hideDefaultMenuItems.includes((this.modelValue as DefaultMobilePage).id)
    this.mobilePageRowForm.patchValue(
      {
        visible: visible,
        icon: this.defaultMobilePages.icon,
        label: null
      }, {emitEvent: false}
    );
    this.modelValue.visible = visible;
    delete this.modelValue.icon;
    delete this.modelValue.label;
    this.updateModel();
  }

  delete() {
    this.pageRemoved.emit();
  }

  edit($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        disabled: this.disabled,
        pageItem: deepClone(this.modelValue)
      };
      if (this.isDefaultMenuItem) {
        const defaultMobilePagePanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
          this.viewContainerRef, DefaultMobilePagePanelComponent, ['right', 'bottom', 'top'], true, null,
          ctx,
          {},
          {}, {}, false, () => {}, {padding: '16px 24px'});
        defaultMobilePagePanelPopover.tbComponentRef.instance.popover = defaultMobilePagePanelPopover;
        defaultMobilePagePanelPopover.tbComponentRef.instance.defaultMobilePageApplied.subscribe((menuItem) => {
          defaultMobilePagePanelPopover.hide();
          this.afterPageEdit(menuItem);
        });
      } else {
        const customMobilePagePanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
          this.viewContainerRef, CustomMobilePagePanelComponent, ['right', 'bottom', 'top'], true, null,
          ctx,
          {},
          {}, {}, false, () => {}, {padding: '16px 24px'});
        customMobilePagePanelPopover.tbComponentRef.instance.popover = customMobilePagePanelPopover;
        customMobilePagePanelPopover.tbComponentRef.instance.customMobilePageApplied.subscribe((page) => {
          customMobilePagePanelPopover.hide();
          this.afterPageEdit(page);
        });
      }
    }
  }

  get itemName(): string {
    return this.isDefaultMenuItem ? this.defaultItemName : this.mobilePageRowForm.get('label').value;
  }

  get itemNamePlaceholder(): string {
    return this.isDefaultMenuItem ? this.defaultItemName : '';
  }

  private updateIconNameBlockWidth() {
    if (this.maxIconNameBlockWidth) {
      this.iconNameBlockWidth = `${this.maxIconNameBlockWidth}px`;
    } else {
      this.iconNameBlockWidth = '100%';
    }
  }

  private updateModel() {
    this.modelValue.visible = this.mobilePageRowForm.get('visible').value;
    const label = this.mobilePageRowForm.get('label').value;
    if (label?.trim()) {
      this.modelValue.label = label;
    } else {
      delete this.modelValue.label;
    }
    let icon = this.mobilePageRowForm.get('icon').value;
    if (this.isDefaultMenuItem) {
      if (this.defaultMobilePages.icon === icon) {
        icon = null;
      }
    }
    if (icon) {
      this.modelValue.icon = icon;
    } else {
      delete this.modelValue.icon;
    }
    this.updateCleanupState();
    this.updateItemInfo();
    this.propagateChange(this.modelValue);
  }

  private updateCleanupState() {
    if (this.isDefaultMenuItem) {
      this.isCleanupEnabled =
        (hideDefaultMenuItems.includes((this.modelValue as DefaultMobilePage).id) ? this.modelValue.visible : !this.modelValue.visible) ||
        !!this.modelValue.label ||
        !!this.modelValue.icon;
    }
  }

  private updateItemInfo() {
    if (this.isCustomMenuItem) {
      this.itemInfo = this.translate.instant(mobilePageTypeTranslations.get(this.modelValue.type));
    } else {
      this.itemInfo = '';
    }
  }

  private afterPageEdit(page: DefaultMobilePage | CustomMobilePage) {
    this.mobilePageRowForm.patchValue({
      visible: page.visible,
      icon: page.icon || (this.isDefaultMenuItem ? this.defaultMobilePages.icon : null),
      label: page.label
    }, {emitEvent: false});
    if (this.isCustomMenuItem) {
      this.modelValue = page;
    }
    this.updateModel();
  }
}
