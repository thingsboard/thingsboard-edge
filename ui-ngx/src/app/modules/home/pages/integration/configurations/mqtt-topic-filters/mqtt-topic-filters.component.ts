import { Component, Input, OnInit } from '@angular/core';
import { mqttQoSTypes } from '../../integration-forms-templates';
import { FormArray, FormBuilder } from '@angular/forms';

@Component({
  selector: 'tb-mqtt-topic-filters',
  templateUrl: './mqtt-topic-filters.component.html',
  styleUrls: ['./mqtt-topic-filters.component.scss']
})
export class MqttTopicFiltersComponent implements OnInit {

  @Input() topicFilters: FormArray;
  @Input() disableMqttTopics: boolean;


  mqttQoSTypes = mqttQoSTypes;

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
  }

  addTopicFilter() {
    this.topicFilters.push(this.fb.group({
      filter: [''],
      qos: [0]
    }));
  }

}
