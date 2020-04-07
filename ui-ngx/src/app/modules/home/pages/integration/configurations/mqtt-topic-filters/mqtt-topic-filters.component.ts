import { Component, OnInit, Input } from '@angular/core';
import { Observable } from 'rxjs';

@Component({
  selector: 'tb-mqtt-topic-filters',
  templateUrl: './mqtt-topic-filters.component.html',
  styleUrls: ['./mqtt-topic-filters.component.scss']
})
export class MqttTopicFiltersComponent implements OnInit {

  @Input() topicFilters;
  @Input() disableMqttTopics;
  @Input() isLoading$: Observable<boolean>;
  @Input() isEdit: boolean;

  constructor() { }

  ngOnInit(): void {
  }

  addTopicFilter = () => {
 /*   if (!scope.topicFilters) {
        scope.topicFilters = [];
    }
    scope.topicFilters.push(
        {
            filter: '',
            qos: 0
        }
    );
    ngModelCtrl.$setDirty();
    scope.updateValidity();*/
}

removeTopicFilter = (index) => {
  /*if (index > -1) {
      scope.topicFilters.splice(index, 1);
      ngModelCtrl.$setDirty();
      scope.updateValidity();
  }*/
};

}
