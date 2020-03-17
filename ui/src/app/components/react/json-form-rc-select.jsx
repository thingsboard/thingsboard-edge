/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import 'rc-select/assets/index.css';

import React from 'react';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import Select, {Option} from 'rc-select';

class ThingsboardRcSelect extends React.Component {

    constructor(props) {
        super(props);
        this.onSelect = this.onSelect.bind(this);
        this.onDeselect = this.onDeselect.bind(this);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.state = {
            currentValue: this.keyToCurrentValue(this.props.value, this.props.form.schema.type === 'array'),
            items: this.props.form.items,
            focused: false
        };
    }

    keyToCurrentValue(key, isArray) {
        var currentValue = isArray ? [] : null;
        if (isArray) {
            var keys = key;
            if (keys) {
                for (var i = 0; i < keys.length; i++) {
                    currentValue.push({key: keys[i], label: this.labelFromKey(keys[i])});
                }
            }
        } else {
            currentValue = {key: key, label: this.labelFromKey(key)};
        }
        return currentValue;
    }

    labelFromKey(key) {
        let label = key || '';
        if (key) {
            for (var i=0;i<this.props.form.items.length;i++) {
                var item = this.props.form.items[i];
                if (item.value === key) {
                    label = item.label;
                    break;
                }
            }
        }
        return label;
    }

    arrayValues(items) {
        var v = [];
        if (items) {
            for (var i = 0; i < items.length; i++) {
                v.push(items[i].key);
            }
        }
        return v;
    }

    keyIndex(values, key) {
        var index = -1;
        if (values) {
            for (var i = 0; i < values.length; i++) {
                if (values[i].key === key) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    onSelect(value, option) {
        if(this.props.form.schema.type === 'array') {
            let v = this.state.currentValue;
            v.push(this.keyToCurrentValue(value.key, false));
            this.setState({
                currentValue: v
            });
            this.props.onChangeValidate(this.arrayValues(v));
        } else {
            this.setState({currentValue: this.keyToCurrentValue(value.key, false)});
            this.props.onChangeValidate({target: {value: value.key}});
        }
    }

    onDeselect(value, option) {
        if (this.props.form.schema.type === 'array') {
            let v = this.state.currentValue;
            let index = this.keyIndex(v, value.key);
            if (index > -1) {
                v.splice(index, 1);
            }
            this.setState({
                currentValue: v
            });
            this.props.onChangeValidate(this.arrayValues(v));
        }
    }

    onBlur() {
        this.setState({ focused: false })
    }

    onFocus() {
        this.setState({ focused: true })
    }

    render() {
        let options = [];
        if(this.state.items && this.state.items.length > 0) {
            options = this.state.items.map((item, idx) => (
                <Option key={idx} value={item.value}>{item.label}</Option>
            ));
        }

        var labelClass = "tb-label";
        if (this.props.form.required) {
            labelClass += " tb-required";
        }
        if (this.props.form.readonly) {
            labelClass += " tb-readonly";
        }
        if (this.state.focused) {
            labelClass += " tb-focused";
        }

        return (
            <div className="tb-container">
                <label className={labelClass}>{this.props.form.title}</label>
                <Select
                    className={this.props.form.className}
                    dropdownClassName={this.props.form.dropdownClassName}
                    dropdownStyle={this.props.form.dropdownStyle}
                    dropdownMenuStyle={this.props.form.dropdownMenuStyle}
                    allowClear={this.props.form.allowClear}
                    tags={this.props.form.tags}
                    maxTagTextLength={this.props.form.maxTagTextLength}
                    multiple={this.props.form.multiple}
                    combobox={this.props.form.combobox}
                    disabled={this.props.form.readonly}
                    value={this.state.currentValue}
                    labelInValue={true}
                    onSelect={this.onSelect}
                    onDeselect={this.onDeselect}
                    onFocus={this.onFocus}
                    onBlur={this.onBlur}
                    style={this.props.form.style || {width: "100%"}}>
                    {options}
                </Select>
                <div className="json-form-error"
                     style={{opacity: this.props.valid ? '0' : '1',
                             bottom: '-5px'}}>{this.props.error}</div>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardRcSelect);
