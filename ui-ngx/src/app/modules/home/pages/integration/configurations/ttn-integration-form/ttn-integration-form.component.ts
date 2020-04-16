import { Component, OnInit, Input, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup, FormArray, FormControl, FormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-ttn-integration-form',
  templateUrl: './ttn-integration-form.component.html',
  styleUrls: ['./ttn-integration-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class TtnIntegrationFormComponent implements OnInit {
  @Input() form: FormGroup;
  @Input() topicFilters: FormGroup;
  @Input() downlinkTopicPattern: FormControl;

  hostTypes = ['Region', 'Custom'];
  hostRegion: FormControl;
  hostCustom: FormControl;
  currentHostType: FormControl;

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
    this.form.get('host').setValidators(Validators.required);
    this.form.get('credentials').get('username').valueChanges.subscribe(name => {
      this.downlinkTopicPattern.patchValue(name + '/devices/${devId}/down');
    });
    this.hostRegion = this.fb.control('');
    this.hostCustom = this.fb.control('');
    this.currentHostType = this.fb.control('Region');
  }

  buildHostName() {
    const hostRegionSuffix = '.thethings.network';
    this.form.get('host').patchValue((this.currentHostType.value === 'Region')
      ? (this.hostRegion.value + hostRegionSuffix) : this.hostCustom.value);
    this.form.get('customHost').patchValue(this.currentHostType.value === 'Custom');
  }

}
