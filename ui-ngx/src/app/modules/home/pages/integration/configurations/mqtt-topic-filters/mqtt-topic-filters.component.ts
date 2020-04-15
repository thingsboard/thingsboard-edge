import { Component, OnInit, Input } from '@angular/core';
import { Observable } from 'rxjs';
import { mqttQoSTypes } from '../../integartion-forms-templates';
import { FormBuilder, FormArray, Validators } from '@angular/forms';

@Component({
  selector: 'tb-mqtt-topic-filters',
  templateUrl: './mqtt-topic-filters.component.html',
  styleUrls: ['./mqtt-topic-filters.component.scss']
})
export class MqttTopicFiltersComponent implements OnInit {

  @Input() topicFilters: FormArray;
  @Input() disableMqttTopics: boolean;
  @Input() defFilter='';

  mqttQoSTypes = mqttQoSTypes;

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
    this.topicFilters.setValidators(Validators.required)
  }

  addTopicFilter() {
    this.topicFilters.push(this.fb.group({
      filter: [''],
      qos: [0]
    }));
  }

}
