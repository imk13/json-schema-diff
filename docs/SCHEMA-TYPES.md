# Schema Types Reference

This document describes each typed schema class in the `com.github.jsonschemadiff.schema` package and how JSON Schema constructs map to them.

## Schema (Abstract Base)

All schema classes extend `Schema`, which provides:

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Value of `$id` keyword (or `id` in Draft-04) |
| `title` | `String` | Value of `title` keyword |
| `description` | `String` | Value of `description` keyword |
| `defaultValue` | `Object` | Value of `default` keyword |
| `unprocessedProperties` | `Map<String, JsonNode>` | Non-standard keywords (e.g., `connect.type`) |

## StringSchema

Represents `{"type": "string", ...}`.

| Field | Type | JSON Keyword |
|---|---|---|
| `maxLength` | `Integer` | `maxLength` |
| `minLength` | `Integer` | `minLength` |
| `pattern` | `Pattern` | `pattern` |

**Example:**

```json
{"type": "string", "minLength": 1, "maxLength": 255, "pattern": "^[a-zA-Z]+$"}
```

## NumberSchema

Represents `{"type": "number"}` or `{"type": "integer"}`.

| Field | Type | JSON Keyword |
|---|---|---|
| `maximum` | `Number` | `maximum` |
| `minimum` | `Number` | `minimum` |
| `exclusiveMaximumLimit` | `Number` | `exclusiveMaximum` (Draft-06+: numeric value; Draft-04: derived from boolean `exclusiveMaximum` + `maximum`) |
| `exclusiveMinimumLimit` | `Number` | `exclusiveMinimum` (Draft-06+: numeric value; Draft-04: derived from boolean `exclusiveMinimum` + `minimum`) |
| `multipleOf` | `Number` | `multipleOf` |
| `requiresInteger` | `boolean` | `true` when `type` is `"integer"` |

**Example (Draft-07+):**

```json
{"type": "integer", "minimum": 0, "exclusiveMaximum": 100, "multipleOf": 5}
```

**Example (Draft-04):**

```json
{"type": "integer", "minimum": 0, "maximum": 100, "exclusiveMaximum": true, "multipleOf": 5}
```

Both load identically: `NumberSchema(minimum=0, exclusiveMaximumLimit=100, multipleOf=5, requiresInteger=true)`.

## ObjectSchema

Represents object schemas (explicit `"type": "object"` or inferred from structural keywords).

| Field | Type | JSON Keyword |
|---|---|---|
| `propertySchemas` | `Map<String, Schema>` | `properties` |
| `requiredProperties` | `Set<String>` | `required` |
| `permitsAdditionalProperties` | `boolean` | `additionalProperties` (boolean) |
| `schemaOfAdditionalProperties` | `Schema` | `additionalProperties` (object) |
| `patternProperties` | `Map<Pattern, Schema>` | `patternProperties` |
| `propertyDependencies` | `Map<String, Set<String>>` | `dependencies` (array values) or `dependentRequired` (Draft 2019-09+) |
| `schemaDependencies` | `Map<String, Schema>` | `dependencies` (object values) or `dependentSchemas` (Draft 2019-09+) |
| `maxProperties` | `Integer` | `maxProperties` |
| `minProperties` | `Integer` | `minProperties` |

**Example:**

```json
{
  "type": "object",
  "properties": {
    "name": {"type": "string"},
    "age": {"type": "integer", "minimum": 0}
  },
  "required": ["name"],
  "additionalProperties": false
}
```

**Type inference:** A schema without `"type": "object"` is still loaded as `ObjectSchema` if it contains any of: `properties`, `additionalProperties`, `patternProperties`, `required`, `dependencies`, `dependentRequired`, `dependentSchemas`, `minProperties`, `maxProperties`.

## ArraySchema

Represents array schemas.

| Field | Type | JSON Keyword |
|---|---|---|
| `allItemSchema` | `Schema` | `items` (when a single schema; in Draft 2020-12 this is always a single schema) |
| `itemSchemas` | `List<Schema>` | `items` (when a tuple/array, pre-2020-12) or `prefixItems` (Draft 2020-12) |
| `permitsAdditionalItems` | `boolean` | `additionalItems` (boolean) |
| `schemaOfAdditionalItems` | `Schema` | `additionalItems` (object) |
| `maxItems` | `Integer` | `maxItems` |
| `minItems` | `Integer` | `minItems` |
| `needsUniqueItems` | `boolean` | `uniqueItems` |

**Single item schema (all items must match):**

```json
{"type": "array", "items": {"type": "string"}, "minItems": 1}
```

**Tuple validation (pre-2020-12):**

```json
{"type": "array", "items": [{"type": "string"}, {"type": "integer"}], "additionalItems": false}
```

**Tuple validation (Draft 2020-12):**

```json
{"type": "array", "prefixItems": [{"type": "string"}, {"type": "integer"}], "items": false}
```

Both produce the same `ArraySchema(itemSchemas=[StringSchema, NumberSchema], permitsAdditionalItems=false)`.

## CombinedSchema

Represents `allOf`, `anyOf`, or `oneOf` compositions. Also used internally for:

- **Implicit allOf:** `{"type": "string", "enum": ["red"]}` becomes `CombinedSchema(ALL, [StringSchema, EnumSchema])`
- **Type arrays:** `{"type": ["string", "integer"]}` becomes `CombinedSchema(ANY, [StringSchema, NumberSchema])`

| Field | Type | Description |
|---|---|---|
| `criterion` | `ValidationCriterion` | `ALL` (allOf), `ANY` (anyOf), or `ONE` (oneOf) |
| `subschemas` | `Collection<Schema>` | The child schemas |

**Example:**

```json
{"oneOf": [{"type": "string"}, {"type": "integer"}]}
```

## NotSchema

Represents `{"not": ...}`.

| Field | Type | Description |
|---|---|---|
| `mustNotMatch` | `Schema` | The schema that must NOT match |

**Example:**

```json
{"not": {"type": "string"}}
```

## EnumSchema

Represents `{"enum": [...]}` (without a co-occurring type keyword).

| Field | Type | Description |
|---|---|---|
| `possibleValues` | `Set<Object>` | The allowed values |

**Example:**

```json
{"enum": ["red", "green", "blue"]}
```

Note: `{"type": "string", "enum": ["red"]}` is **not** loaded as `EnumSchema` -- it becomes a `CombinedSchema(ALL, [StringSchema, EnumSchema])`.

## ConstSchema

Represents `{"const": ...}` (without a co-occurring type keyword).

| Field | Type | Description |
|---|---|---|
| `permittedValue` | `Object` | The single allowed value |

**Example:**

```json
{"const": 42}
```

## EmptySchema

Represents the "accept everything" schema. Produced from:

- Boolean `true`
- Empty object `{}`
- `"type": "boolean"` or `"type": "null"` (these have no diff-relevant constraints)

## FalseSchema

Represents the "reject everything" schema. Produced from boolean `false`.

## SchemaLoader

`SchemaLoader` is the entry point for parsing. It is draft-aware and normalizes all draft-specific keywords into the same typed model.

```java
// From JSON string (auto-detect draft from $schema, default Draft-07)
Schema s1 = SchemaLoader.load("{\"type\": \"string\"}");

// From JSON string with explicit draft
Schema s2 = SchemaLoader.load("{\"type\": \"number\", \"maximum\": 100, \"exclusiveMaximum\": true}",
    JsonSchemaVersion.DRAFT_4);

// From Jackson JsonNode with explicit draft
Schema s3 = SchemaLoader.load(jsonNode, JsonSchemaVersion.DRAFT_2020_12);

// Instance API (gives access to the root for $ref resolution and detected version)
SchemaLoader loader = new SchemaLoader(rootJsonNode, JsonSchemaVersion.DRAFT_2019_09);
Schema s4 = loader.load();
JsonSchemaVersion detectedVersion = loader.getVersion();
```

### Resolution Order

The loader processes a `JsonNode` in this priority order:

1. Boolean schema (`true` / `false`)
2. Empty object (`{}`)
3. `$ref` resolution
4. Implicit combined (type + enum/const)
5. Explicit combined (`allOf` / `anyOf` / `oneOf`)
6. Type array (`"type": ["string", "integer"]`)
7. `not`
8. `const`
9. `enum`
10. Typed schemas (`string`, `number`, `integer`, `object`, `array`)
11. Type inference from structural keywords
12. Fallback to `EmptySchema`
