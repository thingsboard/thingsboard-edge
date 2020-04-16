import { Component, OnInit, Input, ChangeDetectionStrategy } from '@angular/core';
import { FormGroup, FormControl } from '@angular/forms';
import { Observable } from 'rxjs';


@Component({
  selector: 'tb-ibm-watson-iot-integration-form',
  templateUrl: './ibm-watson-iot-integration-form.component.html',
  styleUrls: ['./ibm-watson-iot-integration-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.Default
})
export class IbmWatsonIotIntegrationFormComponent implements OnInit {

  @Input() form: FormGroup;
  @Input() topicFilters: FormGroup;
  @Input() downlinkTopicPattern: FormControl;

  constructor() { }

  ngOnInit(): void {
  }

}
