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
  ChangeDetectorRef,
  Component,
  EventEmitter,
  forwardRef,
  Input, OnChanges,
  OnInit,
  Output,
  Renderer2, SimpleChanges,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator,
  Validators
} from '@angular/forms';
import {
  CMItemType,
  CustomMenuItem,
  HomeMenuItem,
  HomeMenuItemType,
  isCustomMenuItem,
  isDefaultMenuItem,
  isHomeMenuItem,
  MenuItem
} from '@shared/models/custom-menu.models';
import { TbPopoverService } from '@shared/components/popover.service';
import { TranslateService } from '@ngx-translate/core';
import { MenuSection, menuSectionMap } from '@core/services/menu.models';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { coerceBoolean } from '@shared/decorators/coercion';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

@Component({
  selector: 'tb-custom-menu-item-row',
  templateUrl: './custom-menu-item-row.component.html',
  styleUrls: ['./custom-menu-item-row.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CustomMenuItemRowComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CustomMenuItemRowComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class CustomMenuItemRowComponent implements ControlValueAccessor, OnInit, Validator, OnChanges {

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  showHidden = true;

  @Input()
  @coerceBoolean()
  childDrag = false;

  @Input()
  level = 0;

  @Output()
  menuItemRemoved = new EventEmitter();

  menuItemRowFormGroup: UntypedFormGroup;

  modelValue: MenuItem;

  isDefaultMenuItem = false;

  isHomeMenuItem = false;

  isCustomMenuItem = false;

  isCustomSection = false;

  isCleanupEnabled = false;

  iconNameBlockWidth = '256px';

  private defaultItemName: string;

  private defaultMenuSection: MenuSection;

  get itemName(): string {
    return this.isDefaultMenuItem ? this.defaultItemName : this.customTranslate.transform(this.menuItemRowFormGroup.get('name').value);
  }

  get itemNamePlaceholder(): string {
    return this.isDefaultMenuItem ? this.defaultItemName : this.translate.instant('custom-menu.menu-item-name');
  }

  get pagesDragEnabled(): boolean {
    return !this.disabled && this.visiblePagesControls().length > 1;
  }

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef,
              private translate: TranslateService,
              private customTranslate: CustomTranslatePipe,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit() {
    this.menuItemRowFormGroup = this.fb.group({
      visible: [true, []],
      icon: [null, []],
      name: [null, []],
      pages: this.fb.array([])
    });
    this.menuItemRowFormGroup.valueChanges.subscribe(
      () => this.updateModel()
    );
    this.updateIconNameBlockWidth();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['level', 'childDrag'].includes(propName)) {
          this.updateIconNameBlockWidth();
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.menuItemRowFormGroup.disable({emitEvent: false});
    } else {
      this.menuItemRowFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: MenuItem): void {
    this.modelValue = value;
    this.menuItemRowFormGroup.patchValue(
      {
        visible: value.visible,
        icon: value.icon,
        name: value.name
      }, {emitEvent: false}
    );
    if (value.pages?.length) {
      this.menuItemRowFormGroup.setControl('pages', this.preparePagesFormArray(value.pages), {emitEvent: false});
    }
    if (isDefaultMenuItem(this.modelValue)) {
      this.defaultMenuSection = menuSectionMap.get(this.modelValue.id);
      this.defaultItemName = this.translate.instant(this.defaultMenuSection.name);
      if (!value.icon) {
        this.menuItemRowFormGroup.patchValue({
          icon: this.defaultMenuSection.icon
        }, {emitEvent: false});
      }
      this.isDefaultMenuItem = true;
      if (isHomeMenuItem(this.modelValue)) {
        this.isHomeMenuItem = true;
      }
    } else if (isCustomMenuItem(this.modelValue)) {
      this.isCustomMenuItem = true;
      this.menuItemRowFormGroup.get('name').setValidators([Validators.required]);
      if (this.modelValue.menuItemType === CMItemType.SECTION) {
        this.isCustomSection = true;
      }
    }
    this.updateCleanupState();
    this.cd.markForCheck();
  }

  public validate(_c: UntypedFormControl) {
    if (!this.menuItemRowFormGroup.valid) {
      return {
        invalidMenuItem: true
      };
    }
    return null;
  }

  cleanup() {
    this.menuItemRowFormGroup.patchValue(
      {
        visible: true,
        icon: this.defaultMenuSection.icon,
        name: null
      }, {emitEvent: false}
    );
    this.modelValue.visible = true;
    delete this.modelValue.icon;
    delete this.modelValue.name;
    if (this.isHomeMenuItem) {
      (this.modelValue as HomeMenuItem).homeType = HomeMenuItemType.DEFAULT;
      delete (this.modelValue as HomeMenuItem).dashboardId;
      delete (this.modelValue as HomeMenuItem).hideDashboardToolbar;
    }
    this.updateModel();
  }

  delete() {
    this.menuItemRemoved.emit();
  }

  pageDrop(event: CdkDragDrop<string[]>) {
    const pagesArray = this.pagesFormArray();
    const page = this.visiblePagesControls()[event.previousIndex];
    const previousIndex = this.actualPageIndex(event.previousIndex);
    const currentIndex = this.actualPageIndex(event.currentIndex);
    pagesArray.removeAt(previousIndex);
    pagesArray.insert(currentIndex, page);
  }

  visiblePagesControls(): Array<AbstractControl> {
    return this.pagesFormArray().controls.filter(c => this.showHidden || c.value.visible);
  }

  trackByPage(_index: number, pageControl: AbstractControl): any {
    return pageControl;
  }

  removeCustomPage(index: number) {
    this.pagesFormArray().removeAt(this.actualPageIndex(index));
  }

  addCustomPage() {
    const page: CustomMenuItem = {
      visible: true,
      name: 'Test custom sub item',
      icon: 'star',
      menuItemType: CMItemType.LINK
    };
    const pagesArray = this.pagesFormArray();
    const pageControl = this.fb.control(page, []);
    pagesArray.push(pageControl);
  }

  private updateIconNameBlockWidth() {
    let width = 256;
    if (this.childDrag) {
      width -= 32;
    }
    width -= this.level * 16;
    this.iconNameBlockWidth = width + 'px';
  }

  private updateCleanupState() {
    if (this.isDefaultMenuItem) {
      this.isCleanupEnabled = !this.modelValue.visible ||
        !!this.modelValue.name ||
        !!this.modelValue.icon ||
        (this.isHomeMenuItem && (this.modelValue as HomeMenuItem).homeType !== HomeMenuItemType.DEFAULT);
    }
  }

  private updateModel() {
    this.modelValue.visible = this.menuItemRowFormGroup.get('visible').value;
    const name = this.menuItemRowFormGroup.get('name').value;
    if (name) {
      this.modelValue.name = name;
    } else {
      delete this.modelValue.name;
    }
    let icon = this.menuItemRowFormGroup.get('icon').value;
    if (this.isDefaultMenuItem) {
      if (this.defaultMenuSection.icon === icon) {
        icon = null;
      }
    }
    if (icon) {
      this.modelValue.icon = icon;
    } else {
      delete this.modelValue.icon;
    }
    const pages: MenuItem[] = this.menuItemRowFormGroup.get('pages').value;
    if (pages?.length) {
      this.modelValue.pages = pages;
    } else {
      delete this.modelValue.pages;
    }
    this.updateCleanupState();
    this.propagateChange(this.modelValue);
  }

  private pagesFormArray(): UntypedFormArray {
    return this.menuItemRowFormGroup.get('pages') as UntypedFormArray;
  }

  private actualPageIndex(index: number): number {
    const page = this.visiblePagesControls()[index];
    return this.pagesFormArray().controls.indexOf(page);
  }

  private preparePagesFormArray(pages: MenuItem[]): UntypedFormArray {
    const pagesControls: Array<AbstractControl> = [];
    pages.forEach((page) => {
      pagesControls.push(this.fb.control(page, []));
    });
    return this.fb.array(pagesControls);
  }

}
