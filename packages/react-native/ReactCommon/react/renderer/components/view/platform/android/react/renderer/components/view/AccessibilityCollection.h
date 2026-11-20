/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#pragma once

#include <react/cxxstableapi/UmbrellaGuard.h>

#include <glog/logging.h>
#include <react/renderer/core/PropsParserContext.h>
#include <react/renderer/core/RawValue.h>
#include <react/renderer/debug/DebugStringConvertible.h>

#include <optional>
#include <string>
#include <unordered_map>

// The accessibilityCollection and accessibilityCollectionItem props, which
// Android maps to AccessibilityNodeInfo.CollectionInfo and CollectionItemInfo.
// Only Android reads them, so they live with the Android view props.

namespace facebook::react {

/*
 * Describes a scroll view as a collection, mirroring
 * `AccessibilityNodeInfo.CollectionInfo` plus the `itemCount` of the scroll
 * events it sends. Members are optional because JavaScript may pass a partial
 * object; the native side fills in defaults for the absent ones.
 */
struct AccessibilityCollection {
  std::optional<int> rowCount;
  std::optional<int> columnCount;
  std::optional<bool> hierarchical;
  std::optional<int> itemCount;
};

constexpr bool operator==(const AccessibilityCollection &lhs, const AccessibilityCollection &rhs)
{
  return lhs.rowCount == rhs.rowCount && lhs.columnCount == rhs.columnCount && lhs.hierarchical == rhs.hierarchical &&
      lhs.itemCount == rhs.itemCount;
}

/*
 * Describes a view as an item of the nearest ancestor
 * collection, mirroring `AccessibilityNodeInfo.CollectionItemInfo` plus the
 * `itemIndex` used for the collection's scroll events.
 */
struct AccessibilityCollectionItem {
  std::optional<int> rowIndex;
  std::optional<int> rowSpan;
  std::optional<int> columnIndex;
  std::optional<int> columnSpan;
  std::optional<bool> heading;
  std::optional<int> itemIndex;
};

constexpr bool operator==(const AccessibilityCollectionItem &lhs, const AccessibilityCollectionItem &rhs)
{
  return lhs.rowIndex == rhs.rowIndex && lhs.rowSpan == rhs.rowSpan && lhs.columnIndex == rhs.columnIndex &&
      lhs.columnSpan == rhs.columnSpan && lhs.heading == rhs.heading && lhs.itemIndex == rhs.itemIndex;
}

inline void fromRawValue(const PropsParserContext & /*unused*/, const RawValue &value, AccessibilityCollection &result)
{
  result = {};
  if (!value.hasType<std::unordered_map<std::string, RawValue>>()) {
    LOG(ERROR) << "Unsupported AccessibilityCollection type";
    return;
  }
  auto map = (std::unordered_map<std::string, RawValue>)value;

  auto readInt = [&](const char *key, std::optional<int> &out) {
    auto it = map.find(key);
    if (it != map.end() && it->second.hasType<int>()) {
      out = (int)it->second;
    }
  };
  readInt("rowCount", result.rowCount);
  readInt("columnCount", result.columnCount);
  readInt("itemCount", result.itemCount);

  auto hierarchical = map.find("hierarchical");
  if (hierarchical != map.end() && hierarchical->second.hasType<bool>()) {
    result.hierarchical = (bool)hierarchical->second;
  }
}

inline void
fromRawValue(const PropsParserContext & /*unused*/, const RawValue &value, AccessibilityCollectionItem &result)
{
  result = {};
  if (!value.hasType<std::unordered_map<std::string, RawValue>>()) {
    LOG(ERROR) << "Unsupported AccessibilityCollectionItem type";
    return;
  }
  auto map = (std::unordered_map<std::string, RawValue>)value;

  auto readInt = [&](const char *key, std::optional<int> &out) {
    auto it = map.find(key);
    if (it != map.end() && it->second.hasType<int>()) {
      out = (int)it->second;
    }
  };
  readInt("rowIndex", result.rowIndex);
  readInt("rowSpan", result.rowSpan);
  readInt("columnIndex", result.columnIndex);
  readInt("columnSpan", result.columnSpan);
  readInt("itemIndex", result.itemIndex);

  auto heading = map.find("heading");
  if (heading != map.end() && heading->second.hasType<bool>()) {
    result.heading = (bool)heading->second;
  }
}

#if RN_DEBUG_STRING_CONVERTIBLE
namespace accessibilityCollectionDetail {
inline void appendOptionalMember(std::string &result, const char *name, const std::optional<int> &value)
{
  if (value.has_value()) {
    result += std::string(result.size() > 1 ? "," : "") + name + ":" + std::to_string(*value);
  }
}

inline void appendOptionalMember(std::string &result, const char *name, const std::optional<bool> &value)
{
  if (value.has_value()) {
    result += std::string(result.size() > 1 ? "," : "") + name + ":" + (*value ? "true" : "false");
  }
}
} // namespace accessibilityCollectionDetail

inline std::string toString(const AccessibilityCollection &accessibilityCollection)
{
  std::string result = "{";
  accessibilityCollectionDetail::appendOptionalMember(result, "rowCount", accessibilityCollection.rowCount);
  accessibilityCollectionDetail::appendOptionalMember(result, "columnCount", accessibilityCollection.columnCount);
  accessibilityCollectionDetail::appendOptionalMember(result, "hierarchical", accessibilityCollection.hierarchical);
  accessibilityCollectionDetail::appendOptionalMember(result, "itemCount", accessibilityCollection.itemCount);
  return result + "}";
}

inline std::string toString(const AccessibilityCollectionItem &accessibilityCollectionItem)
{
  std::string result = "{";
  accessibilityCollectionDetail::appendOptionalMember(result, "rowIndex", accessibilityCollectionItem.rowIndex);
  accessibilityCollectionDetail::appendOptionalMember(result, "rowSpan", accessibilityCollectionItem.rowSpan);
  accessibilityCollectionDetail::appendOptionalMember(result, "columnIndex", accessibilityCollectionItem.columnIndex);
  accessibilityCollectionDetail::appendOptionalMember(result, "columnSpan", accessibilityCollectionItem.columnSpan);
  accessibilityCollectionDetail::appendOptionalMember(result, "heading", accessibilityCollectionItem.heading);
  accessibilityCollectionDetail::appendOptionalMember(result, "itemIndex", accessibilityCollectionItem.itemIndex);
  return result + "}";
}
#endif

} // namespace facebook::react
