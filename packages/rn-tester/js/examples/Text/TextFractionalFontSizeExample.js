/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow strict-local
 * @format
 */

import type {LayoutChangeEvent, TextLayoutEvent} from 'react-native';

import RNTesterText from '../../components/RNTesterText';
import * as React from 'react';
import {useState} from 'react';
import {PixelRatio, StyleSheet, Text, View} from 'react-native';

type TextLayoutLine = TextLayoutEvent['nativeEvent']['lines'][number];

export const FRACTIONAL_FONT_SIZE_LINE =
  'The quick brown fox jumps over the lazy dog';
export const FRACTIONAL_FONT_SIZE_LINE_COUNT = 10;
export const FRACTIONAL_FONT_SIZE = 13;
export const FRACTIONAL_LINE_HEIGHT = 18;

/**
 * Renders a known string at `fontSize: 13, lineHeight: 18` and reports the
 * pixel width of each line and the pixel height of the block, so the effect of
 * the `enableFractionalFontSizeAndroid` feature flag can be measured.
 *
 * On a density 2.625 device (420 dpi, e.g. Pixel 4a-9) the font size is
 * 34.125 px and the line height is 47.25 px. With the flag off Android rounds
 * both up (35 px and 48 px), so every line is ~2.6% wider than the same string
 * drawn by a native TextView at 34.125 px, and the 10-line block is 480 px
 * tall instead of 470 px.
 */
function TextFractionalFontSizeExample(): React.Node {
  const [lines, setLines] = useState<?(TextLayoutLine[])>();
  const [blockHeightPx, setBlockHeightPx] = useState<?number>();
  const density = PixelRatio.get();
  const fontScale = PixelRatio.getFontScale();
  const px = (dp: number) => Math.round(dp * density * 1000) / 1000;

  const text = Array(FRACTIONAL_FONT_SIZE_LINE_COUNT)
    .fill(FRACTIONAL_FONT_SIZE_LINE)
    .join('\n');

  return (
    <View testID="text-fractional-font-size">
      <RNTesterText variant="label">
        density {density}, fontScale {fontScale}: fontSize{' '}
        {FRACTIONAL_FONT_SIZE} = {px(FRACTIONAL_FONT_SIZE * fontScale)} px,
        lineHeight {FRACTIONAL_LINE_HEIGHT} ={' '}
        {px(FRACTIONAL_LINE_HEIGHT * fontScale)} px
      </RNTesterText>
      <View
        style={styles.block}
        onLayout={(ev: LayoutChangeEvent) =>
          setBlockHeightPx(px(ev.nativeEvent.layout.height))
        }>
        <Text
          testID="text-fractional-font-size-block"
          style={{
            fontSize: FRACTIONAL_FONT_SIZE,
            lineHeight: FRACTIONAL_LINE_HEIGHT,
          }}
          onTextLayout={ev => setLines(ev.nativeEvent.lines)}>
          {text}
        </Text>
      </View>
      <RNTesterText style={styles.metrics}>
        {`block height: ${blockHeightPx ?? '?'} px\n` +
          (lines ?? [])
            .map(
              (line, i) =>
                `line ${i}: width ${px(line.width)} px, height ${px(line.height)} px`,
            )
            .join('\n')}
      </RNTesterText>
    </View>
  );
}

const styles = StyleSheet.create({
  block: {
    alignSelf: 'flex-start',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'red',
  },
  metrics: {
    marginTop: 8,
    fontSize: 10,
    fontFamily: 'monospace',
  },
});

export default TextFractionalFontSizeExample;
