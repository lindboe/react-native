/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow strict-local
 * @format
 */

import type {CodegenTypes, HostComponent, ViewProps} from 'react-native';

import {codegenNativeComponent} from 'react-native';

type NativeProps = Readonly<{
  ...ViewProps,
  text: string,
  textSizePx: CodegenTypes.Float,
  lineHeightPx: CodegenTypes.Int32,
}>;

export type NativeTextReferenceType = HostComponent<NativeProps>;

export default codegenNativeComponent<NativeProps>(
  'RNTNativeTextReference',
) as NativeTextReferenceType;
