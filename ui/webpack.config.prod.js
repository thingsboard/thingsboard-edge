/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
/* eslint-disable */

const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const CompressionPlugin = require('compression-webpack-plugin');
const webpack = require('webpack');
const path = require('path');
const dirTree = require('directory-tree');
const jsonminify = require("jsonminify");
const UglifyJsPlugin = require('uglifyjs-webpack-plugin');
const HappyPack = require('happypack');

const PUBLIC_RESOURCE_PATH = '/static/';

var langs = [];
dirTree('./src/app/locale/', {extensions:/\.json$/}, (item) => {
    /* It is expected what the name of a locale file has the following format: */
    /* 'locale.constant-LANG_CODE[_REGION_CODE].json', e.g. locale.constant-es.json or locale.constant-zh_CN.json*/
    langs.push(item.name.slice(item.name.lastIndexOf('-') + 1, -5));
});

var happyThreadPool = HappyPack.ThreadPool({ size: 3 });

module.exports = {
    devtool: 'source-map',
    entry: [
        './src/app/app.js',
        'webpack-material-design-icons'
    ],
    output: {
        path: path.resolve(__dirname, 'target/generated-resources/public/static'),
        publicPath: PUBLIC_RESOURCE_PATH,
        filename: 'bundle.[hash].js',
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
            "window.jQuery": "jquery",
            tinycolor: "tinycolor2",
            tv4: "tv4",
            moment: "moment"
        }),
        new CopyWebpackPlugin([
            {
                from: './src/thingsboard.ico',
                to: 'thingsboard.ico'
            },
            {
                from: './src/app/locale',
                to: 'locale',
                ignore: [ '*.js' ],
                transform: function(content, path) {
                    return Buffer.from(jsonminify(content.toString()));
                }
            }
        ]),
        new HtmlWebpackPlugin({
            template: './src/index.html',
            filename: '../index.html',
            title: 'ThingsBoard',
            inject: 'body',
        }),
        new webpack.optimize.OccurrenceOrderPlugin(),
        new webpack.NoErrorsPlugin(),
        new webpack.optimize.DedupePlugin(),
        new webpack.optimize.CommonsChunkPlugin('vendor', 'vendor.[hash].js'),
        new ExtractTextPlugin('style.[contentHash].css', {
            allChunks: true,
        }),
        new webpack.DefinePlugin({
            THINGSBOARD_VERSION: JSON.stringify(require('./package.json').version),
            '__DEVTOOLS__': false,
            'process.env': {
                NODE_ENV: JSON.stringify('production'),
            },
            PUBLIC_PATH: PUBLIC_RESOURCE_PATH,
            SUPPORTED_LANGS: JSON.stringify(langs)
        }),
        new CompressionPlugin({
            asset: "[path].gz[query]",
            algorithm: "gzip",
            test: /\.js$|\.css$|\.svg$|\.ttf$|\.woff$|\.woff2$|\.eot$|\.json$/,
            threshold: 10240,
            minRatio: 0.8
        }),
        new UglifyJsPlugin({
            cache: true,
            parallel: true
        }),
        new HappyPack({
            threadPool: happyThreadPool,
            id: 'cached-babel',
            loaders: ["babel-loader?cacheDirectory=true"]
        }),
        new HappyPack({
            threadPool: happyThreadPool,
            id: 'eslint',
            loaders: ["eslint-loader?{parser: 'babel-eslint'}"]
        }),
        new HappyPack({
            threadPool: happyThreadPool,
            id: 'ng-annotate-and-cached-babel-loader',
            loaders: ['ng-annotate', 'babel-loader?cacheDirectory=true']
        })
    ],
    node: {
        tls: "empty",
        fs: "empty"
    },
    module: {
        loaders: [
            {
                test: /\.jsx$/,
                loader: 'happypack/loader?id=cached-babel',
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                loaders: ['happypack/loader?id=ng-annotate-and-cached-babel-loader'],
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                loaders: ['happypack/loader?id=eslint'],
                // loader: "eslint-loader?{parser: 'babel-eslint'}",
                exclude: /node_modules|vendor/,
                include: __dirname,
            },
            {
                test: /\.css$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader'),
            },
            {
                test: /\.scss$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader!postcss-loader!sass-loader'),
            },
            {
                test: /\.less$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader!postcss-loader!less-loader'),
            },
            {
                test: /\.tpl\.html$/,
                loader: 'ngtemplate?relativeTo=' + (path.resolve(__dirname, './src/app')) + '/!html!html-minifier-loader'
            },
            {
                test: /\.tpl\.txt$/,
                loader: 'raw-loader'
            },
            {
                test: /\.(svg)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                loader: 'url?limit=8192'
            },
            {
                test: /\.(png|jpe?g|gif|woff|woff2|ttf|otf|eot|ico)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                loaders: [
                    'url?limit=8192',
                    'img?minimize'
                ]
            },
            {
                test: /\.json$/,
                loader: 'json-loader',
                exclude: /locale\.constant.*$/,
                include: __dirname,
            }
        ],
    },
    'html-minifier-loader': {
        caseSensitive: true,
        removeComments: true,
        collapseWhitespace: false,
        preventAttributesEscaping: true,
        removeEmptyAttributes: false
    }
};
