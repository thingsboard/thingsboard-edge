import { Component, OnInit, Input } from '@angular/core';
import { FormArray, FormBuilder } from '@angular/forms';

@Component({
  selector: 'tb-opc-ua-subscription-tags',
  templateUrl: './opc-ua-subscription-tags.component.html',
  styleUrls: ['./opc-ua-subscription-tags.component.scss']
})
export class OpcUaSubscriptionTagsComponent implements OnInit {

  @Input() subscriptionTagsForm: FormArray;

  constructor(private fb: FormBuilder) { }

  ngOnInit(): void {
  }

  addSubscriptionTag() {
    this.subscriptionTagsForm.push(this.fb.group(
      {
        key: [''],
        path: [''],
        required: [false]
      }
    ));    
  }

}
