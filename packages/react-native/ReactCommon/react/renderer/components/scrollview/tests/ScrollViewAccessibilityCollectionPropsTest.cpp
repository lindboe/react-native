/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <gtest/gtest.h>

#include <react/renderer/components/scrollview/ScrollViewProps.h>
#include <react/renderer/core/RawProps.h>
#include <react/renderer/core/RawPropsParser.h>

// accessibilityCollection is an Android-only prop of
// HostPlatformScrollViewProps.
#ifdef ANDROID

namespace facebook::react {

namespace {

ScrollViewProps parse(
    const folly::dynamic& rawPropsValue,
    const ScrollViewProps& sourceProps = ScrollViewProps()) {
  ContextContainer contextContainer{};
  PropsParserContext parserContext{-1, contextContainer};

  auto raw = RawProps(rawPropsValue);
  auto parser = RawPropsParser();
  parser.prepare<ScrollViewProps>();
  raw.parse(parser);

  return {parserContext, sourceProps, raw};
}

} // namespace

TEST(ScrollViewAccessibilityCollectionPropsTest, parsesAllCollectionMembers) {
  auto props = parse(
      folly::dynamic::object(
          "accessibilityCollection",
          folly::dynamic::object("rowCount", 40)("columnCount", 1)(
              "hierarchical", false)("itemCount", 40)));

  ASSERT_TRUE(props.accessibilityCollection.has_value());
  EXPECT_EQ(
      *props.accessibilityCollection,
      (AccessibilityCollection{
          .rowCount = 40,
          .columnCount = 1,
          .hierarchical = false,
          .itemCount = 40}));
}

TEST(ScrollViewAccessibilityCollectionPropsTest, ignoresMembersOfTheWrongType) {
  auto props = parse(
      folly::dynamic::object(
          "accessibilityCollection",
          folly::dynamic::object("rowCount", "40")("hierarchical", 1)));

  ASSERT_TRUE(props.accessibilityCollection.has_value());
  EXPECT_EQ(*props.accessibilityCollection, AccessibilityCollection{});
}

TEST(ScrollViewAccessibilityCollectionPropsTest, removingTheCollectionUnsetsIt) {
  auto withCollection = parse(
      folly::dynamic::object(
          "accessibilityCollection", folly::dynamic::object("rowCount", 2)));
  ASSERT_TRUE(withCollection.accessibilityCollection.has_value());

  auto removed = parse(
      folly::dynamic::object("accessibilityCollection", nullptr),
      withCollection);

  EXPECT_FALSE(removed.accessibilityCollection.has_value());
}

TEST(ScrollViewAccessibilityCollectionPropsTest, unrelatedUpdateKeepsTheCollection) {
  auto withCollection = parse(
      folly::dynamic::object(
          "accessibilityCollection", folly::dynamic::object("itemCount", 7)));

  auto updated =
      parse(folly::dynamic::object("scrollEnabled", false), withCollection);

  ASSERT_TRUE(updated.accessibilityCollection.has_value());
  EXPECT_EQ(updated.accessibilityCollection->itemCount, 7);
}

} // namespace facebook::react

#endif
