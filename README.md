# json-schema-diff

A standalone Java library for detecting differences between two JSON Schemas and determining backward compatibility. Inspired by Confluent's Schema Registry `SchemaDiff`, reimplemented with a clean typed schema model and Jackson.

## Features

- **Typed schema model** -- JSON Schema documents are parsed into a rich class hierarchy (`StringSchema`, `ObjectSchema`, `CombinedSchema`, etc.) rather than operating on raw JSON trees.
- **Multi-draft support** -- Supports Draft-04, Draft-06, Draft-07, Draft 2019-09, and Draft 2020-12 with automatic version detection from the `$schema` keyword.
- **Backward compatibility checking** -- Classifies every detected change as compatible or incompatible with two built-in modes (strict and lenient).
- **77 difference types** -- Tracks granular changes across all JSON Schema constructs: strings, numbers, objects, arrays, enums, const, combined schemas (`allOf`/`anyOf`/`oneOf`), `not`, and `$ref`.
- **Zero external schema registry dependencies** -- Standalone Maven module targeting Java 17.

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.github.jsonschemadiff</groupId>
    <artifactId>json-schema-diff</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Compare Two Schemas (JSON Strings)

```java
import com.github.jsonschemadiff.diff.SchemaDiff;
import com.github.jsonschemadiff.diff.Difference;

List<Difference> diffs = SchemaDiff.compare(
    "{\"type\":\"string\",\"maxLength\":10}",
    "{\"type\":\"string\",\"maxLength\":20}"
);
// diffs = [MAX_LENGTH_INCREASED #/maxLength]
```

### Compare Using Typed Schema Objects

```java
import com.github.jsonschemadiff.schema.Schema;
import com.github.jsonschemadiff.schema.SchemaLoader;
import com.github.jsonschemadiff.diff.SchemaDiff;
import com.github.jsonschemadiff.diff.Difference;

Schema original = SchemaLoader.load("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
Schema update   = SchemaLoader.load("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"integer\"}}}");

List<Difference> diffs = SchemaDiff.compare(original, update);
// diffs = [TYPE_CHANGED #/properties/name]
```

### Compare Using Jackson JsonNode

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
JsonNode orig = mapper.readTree("{\"type\":\"string\"}");
JsonNode upd  = mapper.readTree("{\"type\":\"integer\"}");

List<Difference> diffs = SchemaDiff.compare(orig, upd);
// diffs = [TYPE_CHANGED #/]
```

### Check Backward Compatibility

```java
List<Difference> diffs = SchemaDiff.compare(original, update);

// Strict mode (default) -- fewer changes tolerated
boolean compatible = diffs.stream()
    .noneMatch(d -> !SchemaDiff.COMPATIBLE_CHANGES_STRICT.contains(d.getType()));

// Lenient mode -- additionally allows some additionalProperties changes
boolean compatibleLenient = diffs.stream()
    .noneMatch(d -> !SchemaDiff.COMPATIBLE_CHANGES_LENIENT.contains(d.getType()));
```

### Compare with a Specific JSON Schema Draft

```java
import com.github.jsonschemadiff.schema.JsonSchemaVersion;

// Draft-04 schemas (boolean exclusiveMaximum, "id" instead of "$id")
List<Difference> diffs = SchemaDiff.compare(
    "{\"type\":\"number\",\"maximum\":100,\"exclusiveMaximum\":true}",
    "{\"type\":\"number\",\"maximum\":200,\"exclusiveMaximum\":true}",
    JsonSchemaVersion.DRAFT_4
);

// Draft 2020-12 schemas (prefixItems for tuple validation)
List<Difference> diffs2020 = SchemaDiff.compare(
    "{\"type\":\"array\",\"prefixItems\":[{\"type\":\"string\"}]}",
    "{\"type\":\"array\",\"prefixItems\":[{\"type\":\"integer\"}]}",
    JsonSchemaVersion.DRAFT_2020_12
);
```

### Cross-Draft Comparison

```java
// Each schema is loaded with its own draft version
Schema orig = SchemaLoader.load(draft07Json, JsonSchemaVersion.DRAFT_7);
Schema upd  = SchemaLoader.load(draft2020Json, JsonSchemaVersion.DRAFT_2020_12);
List<Difference> diffs = SchemaDiff.compare(orig, upd);
```

### Auto-Detection from `$schema`

When no version is specified, the loader auto-detects from the `$schema` URL:

```java
// Auto-detects Draft-04 from $schema URL
String schema = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\","
    + "\"type\":\"number\",\"maximum\":100,\"exclusiveMaximum\":true}";
Schema loaded = SchemaLoader.load(schema);  // Draft-04 semantics applied
```

If `$schema` is absent or unrecognized, the loader defaults to Draft-07.

### Using a Custom Compatible Changes Set

```java
Set<Difference.Type> myChanges = new HashSet<>(SchemaDiff.COMPATIBLE_CHANGES_STRICT);
myChanges.add(Difference.Type.PROPERTY_ADDED_TO_OPEN_CONTENT_MODEL);

List<Difference> diffs = SchemaDiff.compare(myChanges, original, update);
```

## Supported JSON Schema Drafts

| Draft | `$schema` URL | Key Differences |
|---|---|---|
| **Draft-04** | `http://json-schema.org/draft-04/schema#` | `id` (not `$id`), boolean `exclusiveMaximum`/`exclusiveMinimum`, `definitions`, no `const` |
| **Draft-06** | `http://json-schema.org/draft-06/schema#` | `$id`, numeric `exclusiveMaximum`/`exclusiveMinimum`, `const` added, `definitions` |
| **Draft-07** (default) | `http://json-schema.org/draft-07/schema#` | Same as Draft-06 plus `if`/`then`/`else`, `readOnly`, `writeOnly` |
| **Draft 2019-09** | `https://json-schema.org/draft/2019-09/schema` | `$defs` replaces `definitions`, `dependentRequired`/`dependentSchemas` replace `dependencies` |
| **Draft 2020-12** | `https://json-schema.org/draft/2020-12/schema` | `prefixItems` for tuple validation, `items` is always a single schema |

## Compatibility Modes

| Mode | Description |
|------|-------------|
| **Strict** (`COMPATIBLE_CHANGES_STRICT`) | Default. Changes that relax constraints are compatible (e.g., increasing `maxLength`, removing `required`). Changes that tighten constraints or alter types are incompatible. |
| **Lenient** (`COMPATIBLE_CHANGES_LENIENT`) | Strict plus: tolerates `additionalProperties` narrowing/removal and property additions/removals in open content models. |

## Building

```bash
cd json-schema-diff
mvn clean package
```

## Running Tests

```bash
mvn clean test
```

Tests use the same comprehensive test suites from Confluent's Schema Registry, covering 60+ schema comparison scenarios across both basic and combined schema examples.

## Requirements

- Java 17+
- Maven 3.8+

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `com.networknt:json-schema-validator` | 1.5.6 | Transitive JSON Schema support |
| `com.fasterxml.jackson.core:jackson-databind` | 2.17.2 | JSON parsing and `JsonNode` API |
| `junit:junit` | 4.13.2 | Testing (test scope) |

## License

This module is a standalone reimplementation inspired by Confluent's Schema Registry JSON Schema diff logic.
