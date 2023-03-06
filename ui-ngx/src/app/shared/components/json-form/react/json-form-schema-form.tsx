/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
import * as React from 'react';
import JsonFormUtils from './json-form-utils';

import ThingsboardArray from './json-form-array';
import ThingsboardJavaScript from './json-form-javascript';
import ThingsboardJson from './json-form-json';
import ThingsboardHtml from './json-form-html';
import ThingsboardCss from './json-form-css';
import ThingsboardColor from './json-form-color';
import ThingsboardRcSelect from './json-form-rc-select';
import ThingsboardNumber from './json-form-number';
import ThingsboardText from './json-form-text';
import ThingsboardSelect from './json-form-select';
import ThingsboardRadios from './json-form-radios';
import ThingsboardDate from './json-form-date';
import ThingsboardImage from './json-form-image';
import ThingsboardCheckbox from './json-form-checkbox';
import ThingsboardHelp from './json-form-help';
import ThingsboardFieldSet from './json-form-fieldset';
import ThingsboardIcon from './json-form-icon';
import {
  JsonFormData,
  JsonFormProps,
  onChangeFn,
  OnColorClickFn, onHelpClickFn,
  OnIconClickFn,
  onToggleFullscreenFn
} from './json-form.models';

import _ from 'lodash';
import * as tinycolor_ from 'tinycolor2';
import { GroupInfo } from '@shared/models/widget.models';
import ThingsboardMarkdown from '@shared/components/json-form/react/json-form-markdown';
import { MouseEvent } from 'react';

const tinycolor = tinycolor_;

class ThingsboardSchemaForm extends React.Component<JsonFormProps, any> {

  private hasConditions: boolean;
  private readonly mapper: {[type: string]: any};

  constructor(props) {
    super(props);

    this.mapper = {
      number: ThingsboardNumber,
      text: ThingsboardText,
      password: ThingsboardText,
      textarea: ThingsboardText,
      select: ThingsboardSelect,
      radios: ThingsboardRadios,
      date: ThingsboardDate,
      image: ThingsboardImage,
      checkbox: ThingsboardCheckbox,
      help: ThingsboardHelp,
      array: ThingsboardArray,
      javascript: ThingsboardJavaScript,
      json: ThingsboardJson,
      html: ThingsboardHtml,
      css: ThingsboardCss,
      markdown: ThingsboardMarkdown,
      color: ThingsboardColor,
      'rc-select': ThingsboardRcSelect,
      fieldset: ThingsboardFieldSet,
      icon: ThingsboardIcon
    };

    this.onChange = this.onChange.bind(this);
    this.onColorClick = this.onColorClick.bind(this);
    this.onIconClick = this.onIconClick.bind(this);
    this.onToggleFullscreen = this.onToggleFullscreen.bind(this);
    this.onHelpClick = this.onHelpClick.bind(this);
    this.hasConditions = false;
  }

  onChange(key: (string | number)[], val: any, forceUpdate?: boolean) {
    this.props.onModelChange(key, val, forceUpdate);
    if (this.hasConditions) {
      this.forceUpdate();
    }
  }

  onColorClick(key: (string | number)[], val: tinycolor.ColorFormats.RGBA,
               colorSelectedFn: (color: tinycolor.ColorFormats.RGBA) => void) {
    this.props.onColorClick(key, val, colorSelectedFn);
  }

  onIconClick(key: (string | number)[], val: string,
              iconSelectedFn: (icon: string) => void) {
    this.props.onIconClick(key, val, iconSelectedFn);
  }

  onToggleFullscreen(fullscreenFinishFn?: (el: Element) => void) {
    this.props.onToggleFullscreen(fullscreenFinishFn);
  }

  onHelpClick(event: MouseEvent, helpId: string, helpVisibleFn: (visible: boolean) => void, helpReadyFn: (ready: boolean) => void) {
    this.props.onHelpClick(event, helpId, helpVisibleFn, helpReadyFn);
  }


  builder(form: JsonFormData,
          model: any,
          index: number,
          onChange: onChangeFn,
          onColorClick: OnColorClickFn,
          onIconClick: OnIconClickFn,
          onToggleFullscreen: onToggleFullscreenFn,
          onHelpClick: onHelpClickFn,
          isHelpEnabled: boolean,
          mapper: {[type: string]: any}): JSX.Element {
    const type = form.type;
    const Field = this.mapper[type];
    if (!Field) {
      console.log('Invalid field: \"' + form.key[0] + '\"!');
      return null;
    }
    if (form.condition) {
      this.hasConditions = true;
      // tslint:disable-next-line:no-eval
      if (eval(form.condition) === false) {
        return null;
      }
    }
    return <Field model={model} form={form} key={index} onChange={onChange}
                  onColorClick={onColorClick}
                  onIconClick={onIconClick}
                  onToggleFullscreen={onToggleFullscreen}
                  onHelpClick={onHelpClick}
                  isHelpEnabled={isHelpEnabled}
                  mapper={mapper} builder={this.builder}/>;
  }

  createSchema(theForm: any[]): JSX.Element {
    const merged = JsonFormUtils.merge(this.props.schema, theForm, this.props.ignore, this.props.option);
    let mapper = this.mapper;
    if (this.props.mapper) {
      mapper = _.merge(this.mapper, this.props.mapper);
    }
    const forms = merged.map(function(form, index) {
      return this.builder(form, this.props.model, index, this.onChange, this.onColorClick,
        this.onIconClick, this.onToggleFullscreen, this.onHelpClick, this.props.isHelpEnabled, mapper);
    }.bind(this));

    let formClass = 'SchemaForm';
    if (this.props.isFullscreen) {
      formClass += ' SchemaFormFullscreen';
    }

    return (
      <div style={{width: '100%'}} className={formClass}>{forms}</div>
    );
  }

  render() {
    if (this.props.groupInfoes && this.props.groupInfoes.length > 0) {
      const content: JSX.Element[] = [];
      for (const info of this.props.groupInfoes) {
        const forms = this.createSchema(this.props.form[info.formIndex]);
        const item = <ThingsboardSchemaGroup key={content.length} forms={forms} info={info}></ThingsboardSchemaGroup>;
        content.push(item);
      }
      return (<div>{content}</div>);
    } else {
      return this.createSchema(this.props.form);
    }
  }
}
export default ThingsboardSchemaForm;

interface ThingsboardSchemaGroupProps {
  info: GroupInfo;
  forms: JSX.Element;
}

interface ThingsboardSchemaGroupState {
  showGroup: boolean;
}

class ThingsboardSchemaGroup extends React.Component<ThingsboardSchemaGroupProps, ThingsboardSchemaGroupState> {
  constructor(props) {
    super(props);
    this.state = {
      showGroup: true
    };
  }

  toogleGroup(index) {
    this.setState({
      showGroup: !this.state.showGroup
    });
  }

  render() {
    const theCla = 'pull-right fa fa-chevron-down tb-toggle-icon' + (this.state.showGroup ? '' : ' tb-toggled');
    return (<section className='mat-elevation-z1' style={{marginTop: '10px'}}>
      <div className='SchemaGroupname tb-button-toggle'
           onClick={this.toogleGroup.bind(this)}>{this.props.info.GroupTitle}<span className={theCla}></span></div>
      <div style={{padding: '20px'}} className={this.state.showGroup ? '' : 'invisible'}>{this.props.forms}</div>
    </section>);
  }
}
