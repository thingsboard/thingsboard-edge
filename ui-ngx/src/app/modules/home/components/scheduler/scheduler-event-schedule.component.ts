///
/// Copyright © 2016-2021 ThingsBoard, Inc.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { AfterViewInit, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { PageComponent } from '@shared/components/page.component';
import {
  SchedulerEventSchedule,
  SchedulerRepeatType,
  schedulerRepeatTypeTranslationMap,
  SchedulerTimeUnit,
  schedulerTimeUnitTranslationMap
} from '@shared/models/scheduler-event.models';
import * as _moment from 'moment';
import { isDefined } from '@core/utils';
import { getMomentTz } from '@shared/models/time/time.models';

interface SchedulerEventScheduleConfig {
  timezone: string;
  startDate?: Date;
  repeat?: boolean;
  repeatType?: SchedulerRepeatType;
  weeklyRepeat?: boolean[];
  endsOnDate?: Date;
  timerRepeat?: {
    repeatInterval?: number;
    timeUnit?: SchedulerTimeUnit;
  };
}

@Component({
  selector: 'tb-scheduler-event-schedule',
  templateUrl: './scheduler-event-schedule.component.html',
  styleUrls: ['./scheduler-event-schedule.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SchedulerEventScheduleComponent),
    multi: true
  }]
})
export class SchedulerEventScheduleComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: SchedulerEventScheduleConfig | null;

  scheduleConfigFormGroup: FormGroup;

  schedulerRepeatTypes = Object.keys(SchedulerRepeatType);

  schedulerRepeatType = SchedulerRepeatType;

  schedulerRepeatTypeTranslations = schedulerRepeatTypeTranslationMap;

  schedulerTimeUnits = Object.keys(SchedulerTimeUnit);

  schedulerTimeUnitTranslations = schedulerTimeUnitTranslationMap;

  @Input()
  disabled: boolean;

  private lastAppliedTimezone: string;

  private propagateChange = (v: any) => { };

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
    this.scheduleConfigFormGroup = this.fb.group({
      timezone: [null, [Validators.required]],
      startDate: [null, [Validators.required]],
      repeat: [null, []],
      repeatType: [null, [Validators.required]],
      weeklyRepeat: this.fb.array(this.createDefaultWeeklyRepeat()),
      endsOnDate: [null, [Validators.required]],
      timerRepeat: this.fb.group(
        {
          repeatInterval: [null, [Validators.required, Validators.min(0)]],
          timeUnit: [null, [Validators.required]]
        }
      )
    });

    this.scheduleConfigFormGroup.get('timezone').valueChanges.subscribe((timezone: string) => {
      if (timezone !== this.lastAppliedTimezone && timezone) {
        getMomentTz().subscribe(
          (momentTz) => {
            let startDate: Date = this.scheduleConfigFormGroup.get('startDate').value;
            const startTime = this.dateTimeToUtcTime(momentTz, startDate, this.lastAppliedTimezone);
            startDate = this.dateFromUtcTime(momentTz, startTime, timezone);
            this.scheduleConfigFormGroup.get('startDate').patchValue(startDate, {emitEvent: false});
            let endsOnDate: Date = this.scheduleConfigFormGroup.get('endsOnDate').value;
            if (endsOnDate) {
              const endsOnTime = this.dateToUtcTime(momentTz, endsOnDate, this.lastAppliedTimezone);
              endsOnDate = this.dateFromUtcTime(momentTz, endsOnTime, timezone);
              this.scheduleConfigFormGroup.get('endsOnDate').patchValue(endsOnDate, {emitEvent: false});
            }
            this.lastAppliedTimezone = timezone;
          }
        );
      }
    });

    this.scheduleConfigFormGroup.get('repeat').valueChanges.subscribe((repeat: boolean) => {
      if (repeat) {
        this.scheduleConfigFormGroup.get('repeatType').patchValue(SchedulerRepeatType.DAILY, {emitEvent: false});
        const startDate: Date = this.scheduleConfigFormGroup.get('startDate').value;
        const endsOnDate = new Date(
          startDate.getFullYear(),
          startDate.getMonth(),
          startDate.getDate() + 5);
        this.scheduleConfigFormGroup.get('endsOnDate').patchValue(endsOnDate, {emitEvent: false});
      }
      this.updateEnabledState();
    });

    this.scheduleConfigFormGroup.get('repeatType').valueChanges.subscribe((repeatType: SchedulerRepeatType) => {
      if (repeatType === SchedulerRepeatType.WEEKLY) {
        const startDate: Date = this.scheduleConfigFormGroup.get('startDate').value;
        this.scheduleConfigFormGroup.get('weeklyRepeat').patchValue(this.createDefaultWeeklyRepeat(startDate), {emitEvent: false});
      }
      this.updateEnabledState();
    });

    this.scheduleConfigFormGroup.get('weeklyRepeat').valueChanges.subscribe((weeklyRepeat: boolean[]) => {
      const startDate: Date = this.scheduleConfigFormGroup.get('startDate').value;
      this.scheduleConfigFormGroup.get('weeklyRepeat').patchValue(this.createDefaultWeeklyRepeat(startDate, weeklyRepeat),
        {emitEvent: false});
    });

    this.scheduleConfigFormGroup.valueChanges.subscribe(() => {
      getMomentTz().subscribe(
        (momentTz) => {
          this.updateModel(momentTz);
        }
      );
    });
  }

  weeklyRepeatControl(index: number): FormControl {
    return (this.scheduleConfigFormGroup.get('weeklyRepeat') as FormArray).at(index) as FormControl;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateEnabledState();
  }

  private updateEnabledState() {
    if (this.disabled) {
      this.scheduleConfigFormGroup.disable({emitEvent: false});
    } else {
      this.scheduleConfigFormGroup.enable({emitEvent: false});
      const repeat: boolean = this.scheduleConfigFormGroup.get('repeat').value;
      if (!repeat) {
        this.scheduleConfigFormGroup.get('repeatType').disable({emitEvent: false});
        this.scheduleConfigFormGroup.get('weeklyRepeat').disable({emitEvent: false});
        this.scheduleConfigFormGroup.get('endsOnDate').disable({emitEvent: false});
        this.scheduleConfigFormGroup.get('timerRepeat').disable({emitEvent: false});
      } else {
        const repeatType: SchedulerRepeatType = this.scheduleConfigFormGroup.get('repeatType').value;
        if (repeatType !== SchedulerRepeatType.WEEKLY) {
          this.scheduleConfigFormGroup.get('weeklyRepeat').disable({emitEvent: false});
        }
        if (repeatType !== SchedulerRepeatType.TIMER) {
          this.scheduleConfigFormGroup.get('timerRepeat').disable({emitEvent: false});
        }
      }
    }
  }

  writeValue(value: SchedulerEventSchedule | null): void {
    getMomentTz().subscribe(
      (momentTz) => {
        this.modelValue = this.toSchedulerEventScheduleConfig(momentTz, value);
        let doUpdate = false;
        if (!this.modelValue) {
          this.modelValue = this.createDefaultSchedulerEventScheduleConfig(momentTz);
          doUpdate = true;
        } else if (this.modelValue.timezone !== value.timezone) {
          doUpdate = true;
        }
        this.lastAppliedTimezone = this.modelValue.timezone;
        this.scheduleConfigFormGroup.reset(this.modelValue, {emitEvent: false});
        this.updateEnabledState();
        if (doUpdate) {
          setTimeout(() => {
            this.updateModel(momentTz);
          }, 0);
        }
      }
    );
  }

  private toSchedulerEventScheduleConfig(momentTz: moment.MomentTimezone, value: SchedulerEventSchedule): SchedulerEventScheduleConfig {
    if (value) {
      const timezone = value.timezone || momentTz.guess();
      const config: SchedulerEventScheduleConfig = {
        timezone,
        startDate: this.dateFromUtcTime(momentTz, value.startTime, timezone)
      };
      if (value.repeat && value.repeat !== null) {
        config.repeat = true;
        config.repeatType = value.repeat.type;
        if (value.repeat.type === SchedulerRepeatType.WEEKLY && value.repeat.repeatOn) {
          config.weeklyRepeat = this.createDefaultWeeklyRepeat();
          value.repeat.repeatOn.forEach((repeatOn) => {
            config.weeklyRepeat[repeatOn] = true;
          });
        } else if (value.repeat.type === SchedulerRepeatType.TIMER) {
          config.timerRepeat = {
            repeatInterval: value.repeat.repeatInterval,
            timeUnit: value.repeat.timeUnit
          };
        }
        config.endsOnDate = this.dateFromUtcTime(momentTz, value.repeat.endsOn, timezone);
      } else {
        config.repeat = false;
      }
      return config;
    }
    return null;
  }

  private fromSchedulerEventScheduleConfig(momentTz: moment.MomentTimezone, value: SchedulerEventScheduleConfig): SchedulerEventSchedule {
    if (value) {
      const schedule: SchedulerEventSchedule = {
        timezone: value.timezone,
        startTime: this.dateTimeToUtcTime(momentTz, value.startDate, value.timezone)
      };
      if (value.repeat) {
        schedule.repeat = {
          type: value.repeatType,
          endsOn: this.dateToUtcTime(momentTz, value.endsOnDate, value.timezone)
        };
        if (value.repeatType === SchedulerRepeatType.WEEKLY) {
          schedule.repeat.repeatOn = [];
          for (let i = 0; i < 7; i++) {
            if (value.weeklyRepeat[i]) {
              schedule.repeat.repeatOn.push(i);
            }
          }
        } else if (value.repeatType === SchedulerRepeatType.TIMER) {
          schedule.repeat.repeatInterval = value.timerRepeat.repeatInterval;
          schedule.repeat.timeUnit = value.timerRepeat.timeUnit;
        }
      }
      return schedule;
    }
    return null;
  }

  private createDefaultSchedulerEventScheduleConfig(momentTz: moment.MomentTimezone): SchedulerEventScheduleConfig {
    const scheduleConfig: SchedulerEventScheduleConfig = {
      timezone: momentTz.guess()
    };
    const date = new Date();
    scheduleConfig.startDate = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate());
    scheduleConfig.repeat = false;
    return scheduleConfig;
  }

  private createDefaultWeeklyRepeat(startDate?: Date, weeklyRepeat?: boolean[]): boolean[] {
    if (!weeklyRepeat) {
      weeklyRepeat = [];
      for (let i = 0; i < 7; i++) {
        weeklyRepeat[i] = false;
      }
    }
    if (isDefined(startDate) && startDate !== null) {
      const setCurrentDate = weeklyRepeat.filter(repeat => repeat).length === 0;
      if (setCurrentDate) {
        const day = _moment(startDate).day();
        weeklyRepeat[day] = true;
      }
    }
    return weeklyRepeat;
  }

  private updateModel(momentTz: moment.MomentTimezone) {
    if (this.scheduleConfigFormGroup.valid) {
      this.modelValue = this.scheduleConfigFormGroup.value;
      this.propagateChange(this.fromSchedulerEventScheduleConfig(momentTz, this.modelValue));
    } else {
      this.propagateChange(null);
    }
  }

  private dateFromUtcTime(momentTz: moment.MomentTimezone, time: number, timezone: string): Date {
    const offset = momentTz.zone(timezone).utcOffset(time) * 60 * 1000;
    return new Date(time - offset + new Date(time).getTimezoneOffset() * 60 * 1000);
  }

  private dateTimeToUtcTime(momentTz: moment.MomentTimezone, date: Date, timezone: string): number {
    const ts = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate(),
      date.getHours(),
      date.getMinutes(),
      date.getSeconds(),
      date.getMilliseconds()
    ).getTime();
    const offset = momentTz.zone(timezone).utcOffset(ts) * 60 * 1000;
    return ts + offset - new Date(ts).getTimezoneOffset() * 60 * 1000;
  }

  private dateToUtcTime(momentTz: moment.MomentTimezone, date: Date, timezone: string): number {
    const ts = new Date(
      date.getFullYear(),
      date.getMonth(),
      date.getDate()
    ).getTime();
    const offset = momentTz.zone(timezone).utcOffset(ts) * 60 * 1000;
    return ts + offset - new Date(ts).getTimezoneOffset() * 60 * 1000;
  }

}
