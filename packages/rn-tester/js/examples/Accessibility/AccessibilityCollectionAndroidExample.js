/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow strict-local
 * @format
 */

'use strict';

import type {RNTesterModule} from '../../types/RNTesterTypes';
import type {AccessibilityCollectionItem} from 'react-native';

import RNTesterText from '../../components/RNTesterText';
import * as React from 'react';
import {useState} from 'react';
import {FlatList, SectionList, StyleSheet, View} from 'react-native';

// Each list sets `accessibilityCollection` on the list and
// `accessibilityCollectionItem` on the root view of every item, so that
// TalkBack announces the list like a native one: "in list, 40 items" on
// entering, the position of each item, and "out of list" on leaving.

function Item({
  label,
  collectionItem,
  style,
}: {
  label: string,
  collectionItem: AccessibilityCollectionItem,
  style?: React.PropOf<View, 'style'>,
}): React.Node {
  return (
    <View
      accessible={true}
      accessibilityCollectionItem={collectionItem}
      style={[styles.item, style]}>
      <RNTesterText>{label}</RNTesterText>
    </View>
  );
}

function makeLabels(count: number, offset: number = 0): Array<string> {
  return Array.from({length: count}, (_, i) => `Item ${offset + i + 1}`);
}

const VERTICAL_ITEMS = makeLabels(40);

function VerticalListExample(): React.Node {
  return (
    <FlatList
      data={VERTICAL_ITEMS}
      keyExtractor={label => label}
      accessibilityCollection={{
        rowCount: VERTICAL_ITEMS.length,
        columnCount: 1,
        itemCount: VERTICAL_ITEMS.length,
      }}
      renderItem={({item, index}) => (
        <Item
          label={item}
          collectionItem={{rowIndex: index, columnIndex: 0, itemIndex: index}}
        />
      )}
    />
  );
}

const HORIZONTAL_ITEMS = makeLabels(20);

function HorizontalListExample(): React.Node {
  return (
    <FlatList
      horizontal={true}
      style={styles.horizontalList}
      data={HORIZONTAL_ITEMS}
      keyExtractor={label => label}
      accessibilityCollection={{
        rowCount: 1,
        columnCount: HORIZONTAL_ITEMS.length,
        itemCount: HORIZONTAL_ITEMS.length,
      }}
      renderItem={({item, index}) => (
        <Item
          label={item}
          style={styles.horizontalItem}
          collectionItem={{rowIndex: 0, columnIndex: index, itemIndex: index}}
        />
      )}
    />
  );
}

type Section = {title: string, data: Array<string>};

const SECTIONS: Array<Section> = [
  {title: 'Fruits', data: ['Apple', 'Banana', 'Cherry', 'Grape', 'Mango']},
  {title: 'Vegetables', data: ['Carrot', 'Celery', 'Kale', 'Leek', 'Onion']},
  {title: 'Grains', data: ['Barley', 'Oats', 'Quinoa', 'Rice', 'Wheat']},
];

// Section headers are items of the collection too, marked as headings, so each
// section starts one position after the previous section's last item.
const SECTION_STARTS: Map<string, number> = new Map();
let sectionRowCount = 0;
for (const section of SECTIONS) {
  SECTION_STARTS.set(section.title, sectionRowCount);
  sectionRowCount += 1 + section.data.length;
}

function sectionStart(section: Section): number {
  return SECTION_STARTS.get(section.title) ?? 0;
}

function SectionListExample(): React.Node {
  return (
    <SectionList
      sections={SECTIONS}
      keyExtractor={item => item}
      accessibilityCollection={{
        rowCount: sectionRowCount,
        columnCount: 1,
        itemCount: sectionRowCount,
      }}
      renderSectionHeader={({section}) => {
        const row = sectionStart(section);
        return (
          <Item
            label={section.title}
            style={styles.header}
            collectionItem={{
              rowIndex: row,
              columnIndex: 0,
              heading: true,
              itemIndex: row,
            }}
          />
        );
      }}
      renderItem={({item, index, section}) => {
        const row = sectionStart(section) + 1 + index;
        return (
          <Item
            label={item}
            collectionItem={{rowIndex: row, columnIndex: 0, itemIndex: row}}
          />
        );
      }}
    />
  );
}

const GRID_COLUMNS = 3;
const GRID_ITEMS = makeLabels(30);

function GridExample(): React.Node {
  return (
    <FlatList
      data={GRID_ITEMS}
      numColumns={GRID_COLUMNS}
      keyExtractor={label => label}
      accessibilityCollection={{
        rowCount: Math.ceil(GRID_ITEMS.length / GRID_COLUMNS),
        columnCount: GRID_COLUMNS,
        itemCount: GRID_ITEMS.length,
      }}
      renderItem={({item, index}) => (
        <Item
          label={item}
          style={styles.gridItem}
          collectionItem={{
            rowIndex: Math.floor(index / GRID_COLUMNS),
            columnIndex: index % GRID_COLUMNS,
            itemIndex: index,
          }}
        />
      )}
    />
  );
}

const PAGE_SIZE = 20;
const MAX_ITEMS = 100;

// The collection's counts follow the data, so they grow as pages load.
function LoadMoreExample(): React.Node {
  const [items, setItems] = useState(() => makeLabels(PAGE_SIZE));
  return (
    <FlatList
      data={items}
      keyExtractor={label => label}
      accessibilityCollection={{
        rowCount: items.length,
        columnCount: 1,
        itemCount: items.length,
      }}
      onEndReached={() => {
        if (items.length < MAX_ITEMS) {
          setItems([...items, ...makeLabels(PAGE_SIZE, items.length)]);
        }
      }}
      renderItem={({item, index}) => (
        <Item
          label={item}
          collectionItem={{rowIndex: index, columnIndex: 0, itemIndex: index}}
        />
      )}
    />
  );
}

const styles = StyleSheet.create({
  item: {
    padding: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#ccc',
  },
  header: {
    backgroundColor: '#e8e8e8',
  },
  horizontalList: {
    flexGrow: 0,
  },
  horizontalItem: {
    width: 120,
    borderRightWidth: StyleSheet.hairlineWidth,
    borderRightColor: '#ccc',
  },
  gridItem: {
    flex: 1,
    borderRightWidth: StyleSheet.hairlineWidth,
    borderRightColor: '#ccc',
  },
});

export default {
  title: 'Accessibility Collections',
  category: 'Android',
  description:
    'Lists and grids that TalkBack announces like native ones, using accessibilityCollection and accessibilityCollectionItem.',
  showIndividualExamples: true,
  examples: [
    {
      name: 'vertical-list',
      title: 'Vertical list',
      description: 'A FlatList of 40 items.',
      render: VerticalListExample,
    },
    {
      name: 'horizontal-list',
      title: 'Horizontal list',
      description: 'A horizontal FlatList of 20 items.',
      render: HorizontalListExample,
    },
    {
      name: 'section-list',
      title: 'SectionList with headers',
      description:
        'Section headers are headings and count as items of the list.',
      render: SectionListExample,
    },
    {
      name: 'grid',
      title: 'Grid',
      description: 'A FlatList with numColumns of 3.',
      render: GridExample,
    },
    {
      name: 'load-more',
      title: 'Loading more items',
      description:
        'Loads 20 items at a time up to 100; the counts follow the data.',
      render: LoadMoreExample,
    },
  ],
} as RNTesterModule;
