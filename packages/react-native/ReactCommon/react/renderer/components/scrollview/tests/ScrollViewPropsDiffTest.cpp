/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <gtest/gtest.h>

#include <react/renderer/components/scrollview/ScrollViewProps.h>
#include <react/test_utils/PropsPayloads.h>

#ifdef RN_SERIALIZABLE_STATE

namespace facebook::react {

namespace {

using folly::dynamic;

void expectPayloads(const std::vector<PropsCommit>& commits) {
  expectPayloadsMatchRawProps<ScrollViewProps>(commits);
}

} // namespace

// ReactScrollViewManager has a setter for it; with
// enableExclusivePropsUpdateAndroid there is no rawProps fallback.
TEST(ScrollViewPropsDiffTest, emitsScrollsChildToFocus) {
  expectPayloads({{dynamic::object("scrollsChildToFocus", false)}});
}

// Android ScrollViews start with snapping alignment disabled, and
// setSnapToAlignment disables it for null.
TEST(
    ScrollViewPropsDiffTest,
    keepsAnExplicitSnapToAlignmentStartApartFromUnset) {
  expectPayloads({
      {dynamic::object("snapToAlignment", "start")},
      {dynamic::object("snapToAlignment", "center")},
      {dynamic::object("snapToAlignment", nullptr)},
  });
}

// Android ScrollViews start decelerating at 0.985 (ReactScrollViewHelper).
// A removed rate restores it; Props 1.5 sends null, which the setter reads as
// 0.
TEST(ScrollViewPropsDiffTest, emitsAnExplicitDecelerationRate) {
  expectPayloads({
      {dynamic::object("decelerationRate", 0.998)},
      {dynamic::object("decelerationRate", nullptr),
       dynamic::object("decelerationRate", 0.985)},
  });
}

// The ScrollView managers turn both off for null.
TEST(ScrollViewPropsDiffTest, removingNestedScrollAndMomentumEventsEmitsFalse) {
  expectPayloads({
      {dynamic::object("nestedScrollEnabled", true)(
          "sendMomentumEvents", true)},
      {dynamic::object("nestedScrollEnabled", nullptr)(
           "sendMomentumEvents", nullptr),
       dynamic::object("nestedScrollEnabled", false)(
           "sendMomentumEvents", false)},
  });
}

// BaseViewManager stores the map as a view tag that ReactScrollView's
// accessibility delegate reads, so a change to one member sends the whole
// object.
TEST(
    ScrollViewPropsDiffTest,
    emitsTheWholeAccessibilityCollectionWhenOneMemberChanges) {
  auto collection = [](int count) {
    return dynamic::object(
        "accessibilityCollection",
        dynamic::object("rowCount", count)("columnCount", 1)(
            "hierarchical", false)("itemCount", count));
  };
  expectPayloads({{collection(20)}, {collection(40)}});
}

// Absent members are not filled in; the view manager applies its defaults.
TEST(ScrollViewPropsDiffTest, emitsOnlyThePresentAccessibilityCollectionMembers) {
  expectPayloads({
      {dynamic::object(
          "accessibilityCollection", dynamic::object("rowCount", 3))},
  });
}

// BaseViewManager clears the tag only for null.
TEST(ScrollViewPropsDiffTest, removingAccessibilityCollectionEmitsNull) {
  expectPayloads({
      {dynamic::object(
          "accessibilityCollection", dynamic::object("rowCount", 3))},
      {dynamic::object("accessibilityCollection", nullptr)},
  });
}

} // namespace facebook::react

#endif
