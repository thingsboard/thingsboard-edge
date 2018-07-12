/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
import React from 'react';
import { utils } from 'react-schema-form';

import ThingsboardArray from './json-form-array.jsx';
import ThingsboardJavaScript from './json-form-javascript.jsx';
import ThingsboardJson from './json-form-json.jsx';
import ThingsboardHtml from './json-form-html.jsx';
import ThingsboardCss from './json-form-css.jsx';
import ThingsboardColor from './json-form-color.jsx'
import ThingsboardRcSelect from './json-form-rc-select.jsx';
import ThingsboardNumber from './json-form-number.jsx';
import ThingsboardText from './json-form-text.jsx';
import Select from 'react-schema-form/lib/Select';
import Radios from 'react-schema-form/lib/Radios';
import ThingsboardDate from './json-form-date.jsx';
import ThingsboardImage from './json-form-image.jsx';
import ThingsboardCheckbox from './json-form-checkbox.jsx';
import Help from 'react-schema-form/lib/Help';
import ThingsboardFieldSet from './json-form-fieldset.jsx';

import _ from 'lodash';

class ThingsboardSchemaForm extends React.Component {

    constructor(props) {
        super(props);

        this.mapper = {
            'number': ThingsboardNumber,
            'text': ThingsboardText,
            'password': ThingsboardText,
            'textarea': ThingsboardText,
            'select': Select,
            'radios': Radios,
            'date': ThingsboardDate,
            'image': ThingsboardImage,
            'checkbox': ThingsboardCheckbox,
            'help': Help,
            'array': ThingsboardArray,
            'javascript': ThingsboardJavaScript,
            'json': ThingsboardJson,
            'html': ThingsboardHtml,
            'css': ThingsboardCss,
            'color': ThingsboardColor,
            'rc-select': ThingsboardRcSelect,
            'fieldset': ThingsboardFieldSet
        };

        this.onChange = this.onChange.bind(this);
        this.onColorClick = this.onColorClick.bind(this);
    }

    onChange(key, val) {
        //console.log('SchemaForm.onChange', key, val);
        this.props.onModelChange(key, val);
    }

    onColorClick(event, key, val) {
        this.props.onColorClick(event, key, val);
    }

    builder(form, model, index, onChange, onColorClick, mapper) {
        var type = form.type;
        let Field = this.mapper[type];
        if(!Field) {
            console.log('Invalid field: \"' + form.key[0] + '\"!');
            return null;
        }
        if(form.condition && eval(form.condition) === false) {
            return null;
        }
        return <Field model={model} form={form} key={index} onChange={onChange} onColorClick={onColorClick} mapper={mapper} builder={this.builder}/>
    }

    render() {
        let merged = utils.merge(this.props.schema, this.props.form, this.props.ignore, this.props.option);
        let mapper = this.mapper;
        if(this.props.mapper) {
            mapper = _.merge(this.mapper, this.props.mapper);
        }
        let forms = merged.map(function(form, index) {
            return this.builder(form, this.props.model, index, this.onChange, this.onColorClick, mapper);
        }.bind(this));

        return (
            <div style={{width: '100%'}} className='SchemaForm'>{forms}</div>
        );
    }
}
export default ThingsboardSchemaForm;
