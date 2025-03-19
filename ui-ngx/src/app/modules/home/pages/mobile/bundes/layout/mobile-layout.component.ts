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

import { booleanAttribute, Component, ElementRef, forwardRef, Input, ViewChild } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator
} from '@angular/forms';
import {
  CustomMobilePage,
  getDefaultMobileMenuItem,
  isDefaultMobilePagesConfig,
  MobileLayoutConfig,
  mobileMenuDividers,
  MobilePage,
  MobilePageType
} from '@shared/models/mobile-app.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { deepClone, isDefined } from '@core/utils';
import { Subject } from 'rxjs';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { MatDialog } from '@angular/material/dialog';
import { AddMobilePageDialogComponent } from '@home/pages/mobile/bundes/layout/add-mobile-page-dialog.component';

@Component({
  selector: 'tb-mobile-layout',
  templateUrl: './mobile-layout.component.html',
  styleUrls: ['./mobile-layout.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MobileLayoutComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MobileLayoutComponent),
      multi: true
    }
  ],
})
export class MobileLayoutComponent implements ControlValueAccessor, Validator {

  @Input({transform: booleanAttribute})
  readonly!: boolean

  @ViewChild('mobilePagesContainer')
  mobilePagesContainer: ElementRef<HTMLElement>;

  pagesForm = this.fb.group({
    pages: this.fb.array<MobilePage>([])
  });

  maxIconNameBlockWidth = 256;

  showHiddenPages = new FormControl(true);

  private hideItemsSubject = new Subject<void>();
  hideItems$ = this.hideItemsSubject.asObservable();

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private breakpointObserver: BreakpointObserver,
              private dialog: MatDialog,
              ) {

    this.pagesForm.valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(
      () => this.updateModel()
    );

    this.breakpointObserver.observe([MediaBreakpoints.xs, MediaBreakpoints['gt-xs'], MediaBreakpoints['gt-sm']]).pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.computeMaxIconNameBlockWidth();
    });
    this.computeMaxIconNameBlockWidth();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.pagesForm.disable({emitEvent: false});
    } else {
      this.pagesForm.enable({emitEvent: false});
    }
  }

  writeValue(layout: MobileLayoutConfig) {
    const processLayout = this.prepareMobilePages(layout);
    this.pagesForm.setControl('pages', this.prepareMobilePagesFormArray(processLayout));
  }

  validate(_c: FormControl) {
    if (!this.pagesForm.valid) {
      return {
        invalidLayoutForm: true
      };
    }
    return null;
  }

  hideAll() {
    this.hideItemsSubject.next();
    if (this.showHiddenPages.value) {
      this.showHiddenPages.setValue(false);
    }
  }

  resetToDefault() {
    if (!isDefaultMobilePagesConfig(this.pagesForm.value.pages as MobilePage[])) {
      const processLayout = this.prepareMobilePages(null);
      this.pagesForm.setControl('pages', this.prepareMobilePagesFormArray(processLayout));
    }
  }

  get dragEnabled(): boolean {
    return !this.readonly && this.visibleMobilePagesControls().length > 1;
  }

  visibleMobilePagesControls(): Array<AbstractControl> {
    return this.pagesFormArray().controls.filter(c => this.showHiddenPages.value || c.value.visible);
  }

  mobileItemDrop(event: CdkDragDrop<string[]>) {
    const menuItemsArray = this.pagesFormArray();
    const menuItem = this.visibleMobilePagesControls()[event.previousIndex];
    const previousIndex = this.actualMobilePageIndex(event.previousIndex);
    const currentIndex = this.actualMobilePageIndex(event.currentIndex);
    menuItemsArray.removeAt(previousIndex);
    menuItemsArray.insert(currentIndex, menuItem);
    this.pagesForm.markAsDirty();
  }

  trackByMenuItem(_index: number, menuItemControl: AbstractControl): any {
    return menuItemControl;
  }

  addCustomMobilePage(index?: number) {
    this.dialog.open<AddMobilePageDialogComponent, null,
      CustomMobilePage>(AddMobilePageDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog']
    }).afterClosed().subscribe((menuItem) => {
      if (menuItem) {
        const menuItemsArray = this.pagesFormArray();
        const menuItemControl = this.fb.control(menuItem, []);
        if (isDefined(index)) {
          const insertIndex = this.actualMobilePageIndex(index) + 1;
          menuItemsArray.insert(insertIndex, menuItemControl);
        } else {
          menuItemsArray.push(menuItemControl);
          setTimeout(() => {
            this.mobilePagesContainer.nativeElement.scrollTop = this.mobilePagesContainer.nativeElement.scrollHeight;
          }, 0);
        }
        this.pagesForm.markAsDirty();
      }
    });
  }

  isCustom(menuItemControl: AbstractControl): boolean {
    return menuItemControl.value.type !== MobilePageType.DEFAULT;
  }

  removeCustomPage(index: number) {
    this.pagesFormArray().removeAt(this.actualMobilePageIndex(index));
    this.pagesForm.markAsDirty();
  }

  showMenuDivider(index: number): boolean {
    return mobileMenuDividers.has(index);
  }

  getDividerLabel(index: number): string {
    return mobileMenuDividers.get(index);
  }

  private updateModel() {
    if (isDefaultMobilePagesConfig(this.pagesForm.value.pages as MobilePage[])) {
      this.propagateChange(null);
    } else {
      this.propagateChange(this.pagesForm.value);
    }
  }

  private prepareMobilePages(layout: MobileLayoutConfig) {
    if (!layout?.pages?.length) {
      return getDefaultMobileMenuItem();
    }
    return layout.pages;
  }

  private prepareMobilePagesFormArray(items: MobilePage[]): FormArray<FormControl<MobilePage>> {
    const menuItemsControls: Array<FormControl<MobilePage>> = [];
    items.forEach((item) => {
      menuItemsControls.push(this.fb.control(deepClone(item)));
    });
    return this.fb.array(menuItemsControls);
  }

  private computeMaxIconNameBlockWidth() {
    if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm'])) {
      this.maxIconNameBlockWidth = 256;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-xs'])) {
      this.maxIconNameBlockWidth = 200;
    } else {
      this.maxIconNameBlockWidth = 0;
    }
  }

  private pagesFormArray(): FormArray {
    return this.pagesForm.get('pages') as FormArray;
  }

  private actualMobilePageIndex(index: number): number {
    const menuItem = this.visibleMobilePagesControls()[index];
    return this.pagesFormArray().controls.indexOf(menuItem);
  }

}
