import { Component, OnInit, Input } from '@angular/core';
import { FormGroup, FormArray, FormControl } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-ttn-integration-form',
  templateUrl: './ttn-integration-form.component.html',
  styleUrls: ['./ttn-integration-form.component.scss']
})
export class TtnIntegrationFormComponent implements OnInit {


  @Input() form: FormGroup;
  @Input() topicFilters: FormGroup;
  @Input() downlinkTopicPattern: FormControl;

  hostTypes = {
    region: 'Region',
    custom: 'Custom'
  }

  constructor() { }

  ngOnInit(): void {
    this.form.get('credentials').get('username').valueChanges.subscribe(name => {
      this.downlinkTopicPattern.patchValue(name + '/devices/${devId}/down');
    })
  }

  buildHostName() {
    const hostRegionSuffix = '.thethings.network';
    const formValue = this.form.getRawValue();
    this.form.get('host').patchValue((this.form.get('currentHostType').value === this.hostTypes.region)
      ? (formValue.host + hostRegionSuffix) : formValue.host);
    this.form.get('customHost').patchValue(formValue.currentHostType === this.hostTypes.custom);
  }

}
