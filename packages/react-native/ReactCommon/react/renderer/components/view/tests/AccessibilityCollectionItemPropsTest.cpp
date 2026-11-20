/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <gtest/gtest.h>

#include <react/renderer/components/view/ViewProps.h>
#include <react/renderer/core/RawProps.h>
#include <react/renderer/core/RawPropsParser.h>

// accessibilityCollectionItem is an Android-only prop of HostPlatformViewProps.
#ifdef ANDROID

#include <react/renderer/components/view/HostPlatformViewTraitsInitializer.h>

namespace facebook::react {

namespace {

ViewProps parse(
    const folly::dynamic& rawPropsValue,
    const ViewProps& sourceProps = ViewProps()) {
  ContextContainer contextContainer{};
  PropsParserContext parserContext{-1, contextContainer};

  auto raw = RawProps(rawPropsValue);
  auto parser = RawPropsParser();
  parser.prepare<ViewProps>();
  raw.parse(parser);

  return {parserContext, sourceProps, raw};
}

} // namespace

TEST(AccessibilityCollectionItemPropsTest, parsesAllCollectionItemMembers) {
  auto props = parse(
      folly::dynamic::object(
          "accessibilityCollectionItem",
          folly::dynamic::object("rowIndex", 3)("rowSpan", 1)("columnIndex", 0)(
              "columnSpan", 1)("heading", true)("itemIndex", 3)));

  ASSERT_TRUE(props.accessibilityCollectionItem.has_value());
  EXPECT_EQ(
      *props.accessibilityCollectionItem,
      (AccessibilityCollectionItem{
          .rowIndex = 3,
          .rowSpan = 1,
          .columnIndex = 0,
          .columnSpan = 1,
          .heading = true,
          .itemIndex = 3}));
}

// Absent members stay unset so that only what JavaScript sent reaches the
// view manager, which applies its own defaults.
TEST(AccessibilityCollectionItemPropsTest, leavesAbsentMembersUnset) {
  auto props = parse(
      folly::dynamic::object(
          "accessibilityCollectionItem",
          folly::dynamic::object("rowIndex", 2)));

  ASSERT_TRUE(props.accessibilityCollectionItem.has_value());
  EXPECT_EQ(props.accessibilityCollectionItem->rowIndex, 2);
  EXPECT_FALSE(props.accessibilityCollectionItem->rowSpan.has_value());
  EXPECT_FALSE(props.accessibilityCollectionItem->heading.has_value());
  EXPECT_FALSE(props.accessibilityCollectionItem->itemIndex.has_value());
}

TEST(AccessibilityCollectionItemPropsTest, unrelatedUpdateKeepsTheItem) {
  auto withItem = parse(
      folly::dynamic::object(
          "accessibilityCollectionItem",
          folly::dynamic::object("rowIndex", 5)));

  auto updated = parse(folly::dynamic::object("opacity", 0.5), withItem);

  ASSERT_TRUE(updated.accessibilityCollectionItem.has_value());
  EXPECT_EQ(updated.accessibilityCollectionItem->rowIndex, 5);
}

TEST(AccessibilityCollectionItemPropsTest, ignoresMembersOfTheWrongType) {
  auto props = parse(
      folly::dynamic::object(
          "accessibilityCollectionItem",
          folly::dynamic::object("rowIndex", "3")("heading", 1)));

  ASSERT_TRUE(props.accessibilityCollectionItem.has_value());
  EXPECT_EQ(*props.accessibilityCollectionItem, AccessibilityCollectionItem{});
}

TEST(AccessibilityCollectionItemPropsTest, removingTheItemUnsetsIt) {
  auto withItem = parse(
      folly::dynamic::object(
          "accessibilityCollectionItem",
          folly::dynamic::object("rowIndex", 2)));
  ASSERT_TRUE(withItem.accessibilityCollectionItem.has_value());

  auto removed = parse(
      folly::dynamic::object("accessibilityCollectionItem", nullptr),
      withItem);

  EXPECT_FALSE(removed.accessibilityCollectionItem.has_value());
}

// TalkBack looks for the collection item among the ancestors of the focused
// node, so the item view must stay the native parent of its children.
TEST(AccessibilityCollectionItemPropsTest, collectionItemFormsAStackingContext) {
  EXPECT_FALSE(
      HostPlatformViewTraitsInitializer::formsStackingContext(
          parse(folly::dynamic::object())));
  EXPECT_TRUE(
      HostPlatformViewTraitsInitializer::formsStackingContext(parse(
          folly::dynamic::object(
              "accessibilityCollectionItem",
              folly::dynamic::object("rowIndex", 0)))));
}

} // namespace facebook::react

#endif
