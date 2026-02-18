# Difference Types Reference

Every change detected by `SchemaDiff` is represented as a `Difference` with a `Type` enum value and a JSON path indicating where the change occurred. This document lists all 77 difference types grouped by category.

## General

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `TYPE_CHANGED` | Schema type changed (e.g., string to integer) | - | - |
| `TYPE_EXTENDED` | Type became less restrictive (e.g., integer to number) | C | C |
| `TYPE_NARROWED` | Type became more restrictive (e.g., number to integer) | - | - |
| `SCHEMA_ADDED` | A new schema appeared where none existed | - | - |
| `SCHEMA_REMOVED` | A schema was removed | C | C |
| `ID_CHANGED` | `$id` value changed | C | C |
| `TITLE_CHANGED` | `title` value changed | C | C |
| `DESCRIPTION_CHANGED` | `description` value changed | C | C |
| `DEFAULT_CHANGED` | `default` value changed | C | C |

**C** = Compatible, **-** = Incompatible

## String Constraints

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `MAX_LENGTH_ADDED` | `maxLength` added (new constraint) | - | - |
| `MAX_LENGTH_REMOVED` | `maxLength` removed (relaxed) | C | C |
| `MAX_LENGTH_INCREASED` | `maxLength` increased (relaxed) | C | C |
| `MAX_LENGTH_DECREASED` | `maxLength` decreased (tightened) | - | - |
| `MIN_LENGTH_ADDED` | `minLength` added (new constraint) | - | - |
| `MIN_LENGTH_REMOVED` | `minLength` removed (relaxed) | C | C |
| `MIN_LENGTH_INCREASED` | `minLength` increased (tightened) | - | - |
| `MIN_LENGTH_DECREASED` | `minLength` decreased (relaxed) | C | C |
| `PATTERN_ADDED` | `pattern` added (new constraint) | - | - |
| `PATTERN_REMOVED` | `pattern` removed (relaxed) | C | C |
| `PATTERN_CHANGED` | `pattern` value changed | - | - |

## Number Constraints

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `MAXIMUM_ADDED` | `maximum` added | - | - |
| `MAXIMUM_REMOVED` | `maximum` removed (relaxed) | C | C |
| `MAXIMUM_INCREASED` | `maximum` increased (relaxed) | C | C |
| `MAXIMUM_DECREASED` | `maximum` decreased (tightened) | - | - |
| `MINIMUM_ADDED` | `minimum` added | - | - |
| `MINIMUM_REMOVED` | `minimum` removed (relaxed) | C | C |
| `MINIMUM_INCREASED` | `minimum` increased (tightened) | - | - |
| `MINIMUM_DECREASED` | `minimum` decreased (relaxed) | C | C |
| `EXCLUSIVE_MAXIMUM_ADDED` | `exclusiveMaximum` added | - | - |
| `EXCLUSIVE_MAXIMUM_REMOVED` | `exclusiveMaximum` removed (relaxed) | C | C |
| `EXCLUSIVE_MAXIMUM_INCREASED` | `exclusiveMaximum` increased (relaxed) | C | C |
| `EXCLUSIVE_MAXIMUM_DECREASED` | `exclusiveMaximum` decreased (tightened) | - | - |
| `EXCLUSIVE_MINIMUM_ADDED` | `exclusiveMinimum` added | - | - |
| `EXCLUSIVE_MINIMUM_REMOVED` | `exclusiveMinimum` removed (relaxed) | C | C |
| `EXCLUSIVE_MINIMUM_INCREASED` | `exclusiveMinimum` increased (tightened) | - | - |
| `EXCLUSIVE_MINIMUM_DECREASED` | `exclusiveMinimum` decreased (relaxed) | C | C |
| `MULTIPLE_OF_ADDED` | `multipleOf` added | - | - |
| `MULTIPLE_OF_REMOVED` | `multipleOf` removed (relaxed) | C | C |
| `MULTIPLE_OF_EXPANDED` | `multipleOf` became a multiple of the old value | - | - |
| `MULTIPLE_OF_REDUCED` | `multipleOf` old value is a multiple of the new value (relaxed) | C | C |
| `MULTIPLE_OF_CHANGED` | `multipleOf` changed without divisibility relation | - | - |

## Object Properties

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `REQUIRED_ATTRIBUTE_ADDED` | A property became required | - | - |
| `REQUIRED_ATTRIBUTE_WITH_DEFAULT_ADDED` | A property became required but has a default | C | C |
| `REQUIRED_ATTRIBUTE_REMOVED` | A property is no longer required (relaxed) | C | C |
| `MAX_PROPERTIES_ADDED` | `maxProperties` added | - | - |
| `MAX_PROPERTIES_REMOVED` | `maxProperties` removed (relaxed) | C | C |
| `MAX_PROPERTIES_INCREASED` | `maxProperties` increased (relaxed) | C | C |
| `MAX_PROPERTIES_DECREASED` | `maxProperties` decreased (tightened) | - | - |
| `MIN_PROPERTIES_ADDED` | `minProperties` added | - | - |
| `MIN_PROPERTIES_REMOVED` | `minProperties` removed (relaxed) | C | C |
| `MIN_PROPERTIES_INCREASED` | `minProperties` increased (tightened) | - | - |
| `MIN_PROPERTIES_DECREASED` | `minProperties` decreased (relaxed) | C | C |
| `ADDITIONAL_PROPERTIES_ADDED` | `additionalProperties` changed from false to true (relaxed) | C | C |
| `ADDITIONAL_PROPERTIES_REMOVED` | `additionalProperties` changed from true to false (tightened) | - | C |
| `ADDITIONAL_PROPERTIES_EXTENDED` | `additionalProperties` schema constraint removed (relaxed) | C | C |
| `ADDITIONAL_PROPERTIES_NARROWED` | `additionalProperties` schema constraint added (tightened) | - | C |

## Object Dependencies

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `DEPENDENCY_ARRAY_ADDED` | A property dependency array was added | - | - |
| `DEPENDENCY_ARRAY_REMOVED` | A property dependency array was removed (relaxed) | C | C |
| `DEPENDENCY_ARRAY_EXTENDED` | A dependency array gained new entries (tightened) | - | - |
| `DEPENDENCY_ARRAY_NARROWED` | A dependency array lost entries (relaxed) | C | C |
| `DEPENDENCY_ARRAY_CHANGED` | A dependency array changed without subset relation | - | - |
| `DEPENDENCY_SCHEMA_ADDED` | A schema dependency was added | - | - |
| `DEPENDENCY_SCHEMA_REMOVED` | A schema dependency was removed (relaxed) | C | C |

## Object Content Model (Property Add/Remove)

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `PROPERTY_ADDED_TO_OPEN_CONTENT_MODEL` | Property added; original allows additional properties | - | C |
| `PROPERTY_WITH_EMPTY_SCHEMA_ADDED_TO_OPEN_CONTENT_MODEL` | Property with `{}` schema added to open model | C | C |
| `REQUIRED_PROPERTY_ADDED_TO_UNOPEN_CONTENT_MODEL` | Required property added to non-open model | - | - |
| `REQUIRED_PROPERTY_WITH_DEFAULT_ADDED_TO_UNOPEN_CONTENT_MODEL` | Required property with default added to non-open model | C | C |
| `OPTIONAL_PROPERTY_ADDED_TO_UNOPEN_CONTENT_MODEL` | Optional property added to non-open model | C | C |
| `PROPERTY_REMOVED_FROM_OPEN_CONTENT_MODEL` | Property removed; update allows additional properties | C | C |
| `PROPERTY_WITH_FALSE_REMOVED_FROM_CLOSED_CONTENT_MODEL` | Property with `false` schema removed from closed model | C | C |
| `PROPERTY_REMOVED_FROM_CLOSED_CONTENT_MODEL` | Property removed from closed model | - | - |
| `PROPERTY_ADDED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL` | Property added and covered by `patternProperties`/`additionalProperties` schema | C | C |
| `PROPERTY_ADDED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL` | Property added but NOT covered by partial model | - | C |
| `PROPERTY_REMOVED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL` | Property removed and covered by partial model | C | C |
| `PROPERTY_REMOVED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL` | Property removed but NOT covered by partial model | - | C |

## Array Constraints

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `MAX_ITEMS_ADDED` | `maxItems` added | - | - |
| `MAX_ITEMS_REMOVED` | `maxItems` removed (relaxed) | C | C |
| `MAX_ITEMS_INCREASED` | `maxItems` increased (relaxed) | C | C |
| `MAX_ITEMS_DECREASED` | `maxItems` decreased (tightened) | - | - |
| `MIN_ITEMS_ADDED` | `minItems` added | - | - |
| `MIN_ITEMS_REMOVED` | `minItems` removed (relaxed) | C | C |
| `MIN_ITEMS_INCREASED` | `minItems` increased (tightened) | - | - |
| `MIN_ITEMS_DECREASED` | `minItems` decreased (relaxed) | C | C |
| `UNIQUE_ITEMS_ADDED` | `uniqueItems` changed to true (tightened) | - | - |
| `UNIQUE_ITEMS_REMOVED` | `uniqueItems` changed to false (relaxed) | C | C |
| `ADDITIONAL_ITEMS_ADDED` | `additionalItems` changed from false to true (relaxed) | C | C |
| `ADDITIONAL_ITEMS_REMOVED` | `additionalItems` changed from true to false (tightened) | - | - |
| `ADDITIONAL_ITEMS_EXTENDED` | `additionalItems` schema constraint removed (relaxed) | C | C |
| `ADDITIONAL_ITEMS_NARROWED` | `additionalItems` schema constraint added (tightened) | - | - |

## Array Item Content Model

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `ITEM_ADDED_TO_OPEN_CONTENT_MODEL` | Item added; original has open item model | - | - |
| `ITEM_WITH_EMPTY_SCHEMA_ADDED_TO_OPEN_CONTENT_MODEL` | Item with `{}` schema added to open model | C | C |
| `ITEM_ADDED_TO_CLOSED_CONTENT_MODEL` | Item added to closed item model | C | C |
| `ITEM_REMOVED_FROM_OPEN_CONTENT_MODEL` | Item removed; update has open item model | C | C |
| `ITEM_WITH_FALSE_REMOVED_FROM_CLOSED_CONTENT_MODEL` | Item with `false` schema removed from closed model | C | C |
| `ITEM_REMOVED_FROM_CLOSED_CONTENT_MODEL` | Item removed from closed item model | - | - |
| `ITEM_ADDED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL` | Item added and covered by `additionalItems` schema | C | C |
| `ITEM_ADDED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL` | Item added but NOT covered | - | - |
| `ITEM_REMOVED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL` | Item removed and covered by partial model | C | C |
| `ITEM_REMOVED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL` | Item removed but NOT covered | - | - |

## Enum / Const

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `ENUM_ARRAY_EXTENDED` | New values added to enum (relaxed) | C | C |
| `ENUM_ARRAY_NARROWED` | Values removed from enum (tightened) | - | - |
| `ENUM_ARRAY_CHANGED` | Enum values changed (both added and removed), or const value changed | - | - |

## Combined Schemas (allOf / anyOf / oneOf)

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `COMBINED_TYPE_EXTENDED` | Criterion changed in a compatible direction (e.g., allOf to anyOf) | C | C |
| `COMBINED_TYPE_CHANGED` | Criterion changed incompatibly | - | - |
| `PRODUCT_TYPE_EXTENDED` | `allOf` gained new subschemas (tightened) | - | - |
| `PRODUCT_TYPE_NARROWED` | `allOf` lost subschemas (relaxed) | C | C |
| `SUM_TYPE_EXTENDED` | `anyOf`/`oneOf` gained new alternatives (relaxed) | C | C |
| `SUM_TYPE_NARROWED` | `anyOf`/`oneOf` lost alternatives (tightened) | - | - |
| `COMBINED_TYPE_SUBSCHEMAS_CHANGED` | Subschemas could not all be matched between original and update | - | - |

## Not Schema

| Type | Description | Strict | Lenient |
|---|---|:---:|:---:|
| `NOT_TYPE_EXTENDED` | The negated schema became more restrictive (allowing more) | - | - |
| `NOT_TYPE_NARROWED` | The negated schema became less restrictive (compatible) | C | C |
