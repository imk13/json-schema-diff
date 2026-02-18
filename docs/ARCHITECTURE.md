# Architecture

This document explains the internal design of `json-schema-diff`.

## High-Level Flow

```
JSON String / JsonNode + optional JsonSchemaVersion
        │
        ▼
  ┌─────────────┐
  │ SchemaLoader │  Parses JSON into typed Schema tree (draft-aware)
  └──────┬──────┘
         │
         ▼
  Typed Schema tree (StringSchema, ObjectSchema, CombinedSchema, ...)
         │
         ▼
  ┌────────────┐
  │ SchemaDiff  │  Recursively walks two Schema trees
  └──────┬─────┘
         │
         ├── StringSchemaDiff
         ├── NumberSchemaDiff
         ├── ObjectSchemaDiff
         ├── ArraySchemaDiff
         ├── CombinedSchemaDiff
         ├── NotSchemaDiff
         ├── EnumSchemaDiff
         └── ConstSchemaDiff
         │
         ▼
  List<Difference>  (type + JSON path for each change)
```

## Package Structure

```
com.github.jsonschemadiff
├── schema/            Typed schema model + loader
│   ├── Schema.java              Abstract base class
│   ├── StringSchema.java        maxLength, minLength, pattern
│   ├── NumberSchema.java        maximum, minimum, exclusiveMax/Min, multipleOf
│   ├── ObjectSchema.java        properties, required, additionalProperties, dependencies, ...
│   ├── ArraySchema.java         items (single/tuple), additionalItems, maxItems, minItems
│   ├── CombinedSchema.java      allOf / anyOf / oneOf with subschemas
│   ├── NotSchema.java           not (mustNotMatch)
│   ├── EnumSchema.java          enum (possibleValues)
│   ├── ConstSchema.java         const (permittedValue)
│   ├── EmptySchema.java         true-schema / {} (accept everything)
│   ├── FalseSchema.java         false-schema (reject everything)
│   ├── JsonSchemaVersion.java   Draft version enum (DRAFT_4 through DRAFT_2020_12)
│   └── SchemaLoader.java        JsonNode → Schema tree parser (draft-aware)
│
├── diff/              Comparison engine
│   ├── SchemaDiff.java          Entry point, combined/non-combined routing
│   ├── StringSchemaDiff.java    Compare maxLength, minLength, pattern
│   ├── NumberSchemaDiff.java    Compare max, min, exclusiveMax/Min, multipleOf, integer/number
│   ├── ObjectSchemaDiff.java    Compare properties, required, additionalProperties, dependencies
│   ├── ArraySchemaDiff.java     Compare items, additionalItems, maxItems, minItems, uniqueItems
│   ├── CombinedSchemaDiff.java  Compare allOf/anyOf/oneOf subschemas (uses bipartite matching)
│   ├── NotSchemaDiff.java       Compare not-schemas (reversed comparison)
│   ├── EnumSchemaDiff.java      Compare enum value sets
│   ├── ConstSchemaDiff.java     Compare const values
│   ├── Context.java             Tracks JSON path, visited schemas, accumulated differences
│   └── Difference.java          Difference type enum (77 values) + error messages
│
└── utils/             Algorithms
    ├── MaximumCardinalityMatch.java   Hopcroft-Karp bipartite matching
    └── Edge.java                      Edge for graph matching
```

## Schema Loading (SchemaLoader)

`SchemaLoader` converts a raw `JsonNode` into a typed `Schema` tree in a single pass. It is **draft-aware**: when constructed with a `JsonSchemaVersion` (or when auto-detecting from the `$schema` URL), it interprets keywords according to the rules of that specific draft. All draft differences are normalized at load time so that the diff engine operates on a uniform typed model regardless of which draft produced the schema. It handles several non-trivial transformations:

### 1. `$ref` Resolution

Local `$ref` pointers (e.g., `{"$ref": "#/definitions/Address"}`) are resolved against the root document before the schema node is interpreted. Resolution is recursive to handle chains of references.

### 2. Implicit Combined Schemas

Some schemas that appear simple are logically combined:

**Type + enum/const becomes `allOf`:**

```json
{"type": "string", "enum": ["red", "blue"]}
```

is loaded as:

```
CombinedSchema(ALL, [StringSchema, EnumSchema(["red", "blue"])])
```

This matches the behavior of the `everit-org/json-schema` library which the original Confluent diff was built on.

**Type arrays become `anyOf`:**

```json
{"type": ["string", "integer"]}
```

is loaded as:

```
CombinedSchema(ANY, [StringSchema, NumberSchema(requiresInteger=true)])
```

### 3. Draft-Specific Keyword Handling

The loader normalizes draft-specific keywords into the same typed model:

| Keyword Area | Draft-04 | Draft-06/07 | Draft 2019-09+ | Draft 2020-12 |
|---|---|---|---|---|
| **Schema identity** | `id` | `$id` | `$id` | `$id` |
| **Exclusive bounds** | Boolean modifier on `maximum`/`minimum` | Numeric value | Numeric value | Numeric value |
| **Dependencies** | `dependencies` | `dependencies` | `dependentRequired` + `dependentSchemas` | `dependentRequired` + `dependentSchemas` |
| **Tuple items** | `items` (array) | `items` (array) | `items` (array) | `prefixItems` |
| **Definitions** | `definitions` | `definitions` | `$defs` | `$defs` |

For example, Draft-04's `{"maximum": 100, "exclusiveMaximum": true}` is loaded identically to Draft-07's `{"exclusiveMaximum": 100}` -- both produce a `NumberSchema` with `exclusiveMaximumLimit = 100`.

### 4. Type Inference

When the `type` keyword is absent, the loader infers the schema type from structural keywords:

| Keywords present | Inferred type |
|---|---|
| `properties`, `additionalProperties`, `patternProperties`, `required`, `dependencies`, `dependentRequired`, `dependentSchemas`, `minProperties`, `maxProperties` | `ObjectSchema` |
| `items`, `prefixItems`, `additionalItems`, `minItems`, `maxItems`, `uniqueItems` | `ArraySchema` |
| `minLength`, `maxLength`, `pattern` | `StringSchema` |
| `minimum`, `maximum`, `exclusiveMinimum`, `exclusiveMaximum`, `multipleOf` | `NumberSchema` |

### 5. Unprocessed Properties

Non-standard keywords (e.g., `connect.type`, `connect.parameters`) are collected into `Schema.getUnprocessedProperties()` so the diff engine can inspect them. This enables special-case handling like `connect.type: bytes`.

## Diff Engine (SchemaDiff)

### Entry Point

`SchemaDiff.compare()` accepts two `Schema` objects (or `JsonNode`/`String` for convenience) and returns a `List<Difference>`, where each difference has a `Type` and a JSON path.

### Combined/Non-Combined Routing

Before type-specific comparison, `SchemaDiff` handles asymmetric cases where one schema is combined and the other is not:

| Original | Update | Behavior |
|---|---|---|
| Non-combined | `CombinedSchema` (1 sub) | Try matching original against the single subschema |
| Non-combined | `anyOf`/`oneOf` (N subs) | Try each subschema; if any match, report `SUM_TYPE_EXTENDED` |
| `allOf` (N subs) | Non-combined | Try each subschema; if any match, report `PRODUCT_TYPE_NARROWED` |
| `CombinedSchema` (1 sub) | Non-combined | Try matching the single subschema against update |

### Type Dispatch

Once both schemas are confirmed to be the same "kind", the engine dispatches to the appropriate diff class using `instanceof`:

```java
if (original instanceof StringSchema) {
    StringSchemaDiff.compare(ctx, (StringSchema) original, (StringSchema) update);
} else if (original instanceof NumberSchema) {
    NumberSchemaDiff.compare(ctx, (NumberSchema) original, (NumberSchema) update);
}
// ... etc.
```

### Context

`Context` manages:

- **JSON path stack** -- Builds paths like `#/properties/name/maxLength` as the engine recurses.
- **Visited schemas set** -- Uses `IdentityHashMap`-backed `Set<Schema>` to detect and break cycles in recursive schemas.
- **Difference accumulation** -- Collects all `Difference` objects.
- **Compatibility check** -- `isCompatible()` tests whether all accumulated differences are in the allowed set.

### CombinedSchema Matching

`CombinedSchemaDiff` uses the Hopcroft-Karp algorithm (`MaximumCardinalityMatch`) to find the best matching between original and update subschemas. For each pair, it runs a full `SchemaDiff.compare()` in a sub-context and only pairs that are compatible form edges in the bipartite graph. The maximum cardinality matching determines which subschemas correspond to each other.

## Difference Types

The 77 `Difference.Type` values are organized by schema construct:

| Category | Examples |
|---|---|
| **General** | `TYPE_CHANGED`, `SCHEMA_ADDED`, `SCHEMA_REMOVED`, `ID_CHANGED`, `TITLE_CHANGED`, `DESCRIPTION_CHANGED`, `DEFAULT_CHANGED` |
| **String** | `MAX_LENGTH_ADDED/REMOVED/INCREASED/DECREASED`, `MIN_LENGTH_*`, `PATTERN_ADDED/REMOVED/CHANGED` |
| **Number** | `MAXIMUM_*`, `MINIMUM_*`, `EXCLUSIVE_MAXIMUM_*`, `EXCLUSIVE_MINIMUM_*`, `MULTIPLE_OF_*`, `TYPE_EXTENDED/NARROWED` |
| **Object** | `REQUIRED_ATTRIBUTE_*`, `MAX/MIN_PROPERTIES_*`, `ADDITIONAL_PROPERTIES_*`, `DEPENDENCY_*`, `PROPERTY_ADDED/REMOVED_*` |
| **Array** | `MAX/MIN_ITEMS_*`, `UNIQUE_ITEMS_*`, `ADDITIONAL_ITEMS_*`, `ITEM_ADDED/REMOVED_*` |
| **Enum/Const** | `ENUM_ARRAY_EXTENDED/NARROWED/CHANGED` |
| **Combined** | `COMBINED_TYPE_EXTENDED/CHANGED`, `PRODUCT_TYPE_EXTENDED/NARROWED`, `SUM_TYPE_EXTENDED/NARROWED`, `COMBINED_TYPE_SUBSCHEMAS_CHANGED` |
| **Not** | `NOT_TYPE_EXTENDED/NARROWED` |

## Backward Compatibility Rules

A schema change is **backward compatible** if data valid under the old schema remains valid under the new schema. The key principle:

- **Relaxing constraints is compatible** (e.g., increasing `maxLength`, removing `required`, adding `anyOf` alternatives)
- **Tightening constraints is incompatible** (e.g., decreasing `maxLength`, adding `required`, removing enum values)
- **Changing types is incompatible** (e.g., `string` to `integer`)

### Strict vs. Lenient

**Strict** (`COMPATIBLE_CHANGES_STRICT`) is the default and treats most constraint relaxations as compatible but considers `additionalProperties` changes as incompatible.

**Lenient** (`COMPATIBLE_CHANGES_LENIENT`) additionally allows:

- `ADDITIONAL_PROPERTIES_NARROWED` / `ADDITIONAL_PROPERTIES_REMOVED`
- `PROPERTY_ADDED_TO_OPEN_CONTENT_MODEL`
- `PROPERTY_REMOVED_FROM_OPEN_CONTENT_MODEL`
- `PROPERTY_ADDED/REMOVED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL`

## Design Decisions

### Why typed Schema classes instead of raw JsonNode?

1. **Type safety** -- Diff classes accept their specific schema type (e.g., `StringSchemaDiff.compare(ctx, StringSchema, StringSchema)`), eliminating runtime type errors.
2. **Clean code** -- Property access through typed getters (e.g., `original.getMaxLength()`) instead of `node.get("maxLength").asInt()`.
3. **Separation of concerns** -- All JSON parsing, `$ref` resolution, and implicit schema decomposition happen once in `SchemaLoader`, not scattered across diff logic.
4. **Extensibility** -- New schema types can be added by creating a new `Schema` subclass and a corresponding diff class.

### Why the builder pattern?

Schema objects are immutable after construction. Builders provide a fluent API and allow optional fields without requiring dozens of constructor parameters.

### Why are draft differences handled in SchemaLoader, not in diff classes?

All draft-specific differences are keyword-level: same concept, different syntax. By normalizing at load time (e.g., Draft-04 boolean `exclusiveMaximum: true` becomes a numeric `exclusiveMaximumLimit` on `NumberSchema`), the diff engine remains draft-agnostic. This is the same strategy used by Confluent's Schema Registry.

### Why IdentityHashMap for cycle detection?

Schema trees can contain cycles (via `$ref`). The `Context` uses object identity (`==`) rather than structural equality (`.equals()`) to track visited schemas, avoiding infinite recursion from re-entrant `equals()` calls on cyclic structures.
