package com.github.jsonschemadiff.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loads a {@link JsonNode} and produces a typed {@link Schema} tree.
 *
 * Handles $ref resolution, type arrays, implicit combined schemas
 * (type + enum/const), and draft-specific keyword interpretation.
 * When no {@link JsonSchemaVersion} is specified the loader auto-detects
 * from the {@code $schema} URL, falling back to {@code DRAFT_7}.
 */
public class SchemaLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final JsonNode rootNode;
  private final JsonSchemaVersion version;

  public SchemaLoader(JsonNode rootNode) {
    this(rootNode, (JsonSchemaVersion) null);
  }

  public SchemaLoader(JsonNode rootNode, JsonSchemaVersion version) {
    this.rootNode = rootNode;
    this.version = version != null ? version : detectVersion(rootNode);
  }

  public Schema load() {
    return loadSchema(rootNode);
  }

  public JsonSchemaVersion getVersion() {
    return version;
  }

  // --- Static convenience methods ---

  public static Schema load(String json) {
    return load(json, null);
  }

  public static Schema load(String json, JsonSchemaVersion version) {
    try {
      JsonNode node = MAPPER.readTree(json);
      return new SchemaLoader(node, version).load();
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON schema", e);
    }
  }

  public static Schema load(JsonNode node) {
    return new SchemaLoader(node).load();
  }

  public static Schema load(JsonNode node, JsonSchemaVersion version) {
    return new SchemaLoader(node, version).load();
  }

  private static JsonSchemaVersion detectVersion(JsonNode root) {
    if (root != null && root.isObject() && root.has("$schema")) {
      JsonNode schemaNode = root.get("$schema");
      if (schemaNode.isTextual()) {
        JsonSchemaVersion detected = JsonSchemaVersion.fromSchemaUrl(schemaNode.asText());
        if (detected != null) {
          return detected;
        }
      }
    }
    return JsonSchemaVersion.DRAFT_7;
  }

  private Schema loadSchema(JsonNode node) {
    if (node == null) {
      return null;
    }

    // Boolean schemas
    if (node.isBoolean()) {
      return node.asBoolean()
          ? EmptySchema.builder().build()
          : FalseSchema.builder().build();
    }

    if (!node.isObject()) {
      return EmptySchema.builder().build();
    }

    // Empty object = accept everything
    if (node.isEmpty()) {
      return EmptySchema.builder().build();
    }

    // Resolve $ref first
    node = resolveRef(node);

    // Check for implicit combined: type + enum/const → allOf
    if (isImplicitCombined(node)) {
      return loadImplicitCombined(node);
    }

    // Explicit combined schemas: allOf, anyOf, oneOf
    if (node.has("allOf") || node.has("anyOf") || node.has("oneOf")) {
      return loadCombinedSchema(node);
    }

    // Type arrays → anyOf
    JsonNode typeNode = node.get("type");
    if (typeNode != null && typeNode.isArray()) {
      return loadTypeArray(node);
    }

    // not schema
    if (node.has("not")) {
      return loadNotSchema(node);
    }

    // const schema (checked before enum since const is more specific)
    if (node.has("const")) {
      return loadConstSchema(node);
    }

    // enum schema
    if (node.has("enum")) {
      return loadEnumSchema(node);
    }

    String type = getTextualType(node);

    if ("string".equals(type)) {
      return loadStringSchema(node);
    }
    if ("number".equals(type) || "integer".equals(type)) {
      return loadNumberSchema(node);
    }
    if ("object".equals(type) || isInferredObject(node, type)) {
      return loadObjectSchema(node);
    }
    if ("array".equals(type) || isInferredArray(node, type)) {
      return loadArraySchema(node);
    }
    if ("boolean".equals(type) || "null".equals(type)) {
      return buildBaseSchema(EmptySchema.builder(), node).build();
    }

    return buildBaseSchema(EmptySchema.builder(), node).build();
  }

  // --- Ref resolution ---

  private JsonNode resolveRef(JsonNode node) {
    if (!node.isObject()) {
      return node;
    }
    JsonNode refNode = node.get("$ref");
    if (refNode == null || !refNode.isTextual()) {
      return node;
    }
    String ref = refNode.asText();
    if (ref.startsWith("#/")) {
      String pointer = ref.substring(1);
      JsonNode resolved = rootNode.at(pointer);
      if (resolved != null && !resolved.isMissingNode()) {
        return resolveRef(resolved);
      }
    }
    return node;
  }

  // --- Implicit combined (type + enum/const) ---

  private boolean isImplicitCombined(JsonNode node) {
    boolean hasEnumOrConst = node.has("enum") || node.has("const");
    if (!hasEnumOrConst) {
      return false;
    }
    String type = getTextualType(node);
    return type != null
        || node.has("properties") || node.has("items")
        || node.has("minLength") || node.has("maxLength") || node.has("pattern")
        || node.has("minimum") || node.has("maximum")
        || node.has("exclusiveMinimum") || node.has("exclusiveMaximum")
        || node.has("multipleOf");
  }

  private Schema loadImplicitCombined(JsonNode node) {
    com.fasterxml.jackson.databind.node.ObjectNode base =
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).deepCopy();
    com.fasterxml.jackson.databind.node.ObjectNode enumPart = base.objectNode();

    if (base.has("enum")) {
      enumPart.set("enum", base.get("enum"));
      base.remove("enum");
    }
    if (base.has("const")) {
      enumPart.set("const", base.get("const"));
      base.remove("const");
    }

    List<Schema> subs = new ArrayList<>();
    subs.add(loadSchema(base));
    if (!enumPart.isEmpty()) {
      subs.add(loadSchema(enumPart));
    }

    return CombinedSchema.builder()
        .criterion(CombinedSchema.ALL_CRITERION)
        .subschemas(subs)
        .build();
  }

  // --- Type array ---

  private Schema loadTypeArray(JsonNode node) {
    JsonNode typeNode = node.get("type");
    List<Schema> subs = new ArrayList<>();
    for (JsonNode t : typeNode) {
      com.fasterxml.jackson.databind.node.ObjectNode sub =
          ((com.fasterxml.jackson.databind.node.ObjectNode) node).deepCopy();
      sub.put("type", t.asText());
      subs.add(loadSchema(sub));
    }

    CombinedSchema.Builder builder = CombinedSchema.builder()
        .criterion(CombinedSchema.ANY_CRITERION)
        .subschemas(subs);
    return buildBaseSchema(builder, node).build();
  }

  // --- Explicit combined ---

  private Schema loadCombinedSchema(JsonNode node) {
    CombinedSchema.ValidationCriterion criterion;
    String keyword;
    if (node.has("allOf")) {
      criterion = CombinedSchema.ALL_CRITERION;
      keyword = "allOf";
    } else if (node.has("anyOf")) {
      criterion = CombinedSchema.ANY_CRITERION;
      keyword = "anyOf";
    } else {
      criterion = CombinedSchema.ONE_CRITERION;
      keyword = "oneOf";
    }

    JsonNode arr = node.get(keyword);
    List<Schema> subs = new ArrayList<>();
    if (arr != null && arr.isArray()) {
      for (JsonNode sub : arr) {
        subs.add(loadSchema(sub));
      }
    }

    CombinedSchema.Builder builder = CombinedSchema.builder()
        .criterion(criterion)
        .subschemas(subs);
    return buildBaseSchema(builder, node).build();
  }

  // --- Not ---

  private Schema loadNotSchema(JsonNode node) {
    Schema mustNotMatch = loadSchema(node.get("not"));
    NotSchema.Builder builder = NotSchema.builder()
        .mustNotMatch(mustNotMatch);
    return buildBaseSchema(builder, node).build();
  }

  // --- Enum ---

  private Schema loadEnumSchema(JsonNode node) {
    Set<Object> values = new LinkedHashSet<>();
    JsonNode enumArr = node.get("enum");
    if (enumArr != null && enumArr.isArray()) {
      for (JsonNode val : enumArr) {
        values.add(nodeToValue(val));
      }
    }
    EnumSchema.Builder builder = EnumSchema.builder()
        .possibleValues(values);
    return buildBaseSchema(builder, node).build();
  }

  // --- Const ---

  private Schema loadConstSchema(JsonNode node) {
    Object value = nodeToValue(node.get("const"));
    ConstSchema.Builder builder = ConstSchema.builder()
        .permittedValue(value);
    return buildBaseSchema(builder, node).build();
  }

  // --- String ---

  private Schema loadStringSchema(JsonNode node) {
    StringSchema.Builder builder = StringSchema.builder();
    if (node.has("maxLength") && node.get("maxLength").isIntegralNumber()) {
      builder.maxLength(node.get("maxLength").asInt());
    }
    if (node.has("minLength") && node.get("minLength").isIntegralNumber()) {
      builder.minLength(node.get("minLength").asInt());
    }
    if (node.has("pattern") && node.get("pattern").isTextual()) {
      builder.pattern(node.get("pattern").asText());
    }
    return buildBaseSchema(builder, node).build();
  }

  // --- Number ---

  private Schema loadNumberSchema(JsonNode node) {
    NumberSchema.Builder builder = NumberSchema.builder();
    if (node.has("maximum") && node.get("maximum").isNumber()) {
      builder.maximum(node.get("maximum").numberValue());
    }
    if (node.has("minimum") && node.get("minimum").isNumber()) {
      builder.minimum(node.get("minimum").numberValue());
    }

    if (version.usesNumericExclusiveBounds()) {
      // Draft-06+: exclusiveMaximum/exclusiveMinimum are numeric values
      if (node.has("exclusiveMaximum") && node.get("exclusiveMaximum").isNumber()) {
        builder.exclusiveMaximumLimit(node.get("exclusiveMaximum").numberValue());
      }
      if (node.has("exclusiveMinimum") && node.get("exclusiveMinimum").isNumber()) {
        builder.exclusiveMinimumLimit(node.get("exclusiveMinimum").numberValue());
      }
    } else {
      // Draft-04: exclusiveMaximum/exclusiveMinimum are booleans that modify maximum/minimum
      if (node.has("exclusiveMaximum") && node.get("exclusiveMaximum").isBoolean()
          && node.get("exclusiveMaximum").asBoolean()
          && node.has("maximum") && node.get("maximum").isNumber()) {
        builder.exclusiveMaximumLimit(node.get("maximum").numberValue());
      }
      if (node.has("exclusiveMinimum") && node.get("exclusiveMinimum").isBoolean()
          && node.get("exclusiveMinimum").asBoolean()
          && node.has("minimum") && node.get("minimum").isNumber()) {
        builder.exclusiveMinimumLimit(node.get("minimum").numberValue());
      }
    }

    if (node.has("multipleOf") && node.get("multipleOf").isNumber()) {
      builder.multipleOf(node.get("multipleOf").numberValue());
    }
    builder.requiresInteger("integer".equals(getTextualType(node)));
    return buildBaseSchema(builder, node).build();
  }

  // --- Object ---

  private Schema loadObjectSchema(JsonNode node) {
    ObjectSchema.Builder builder = ObjectSchema.builder();

    // properties
    JsonNode props = node.get("properties");
    if (props != null && props.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        builder.addPropertySchema(entry.getKey(), loadSchema(entry.getValue()));
      }
    }

    // required
    JsonNode req = node.get("required");
    if (req != null && req.isArray()) {
      for (JsonNode element : req) {
        if (element.isTextual()) {
          builder.addRequiredProperty(element.asText());
        }
      }
    }

    // additionalProperties
    JsonNode ap = node.get("additionalProperties");
    if (ap != null) {
      if (ap.isBoolean()) {
        builder.permitsAdditionalProperties(ap.asBoolean());
      } else if (ap.isObject()) {
        builder.permitsAdditionalProperties(true);
        builder.schemaOfAdditionalProperties(loadSchema(ap));
      }
    }

    // patternProperties
    JsonNode pp = node.get("patternProperties");
    if (pp != null && pp.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = pp.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        builder.addPatternProperty(Pattern.compile(entry.getKey()), loadSchema(entry.getValue()));
      }
    }

    // dependencies / dependentRequired / dependentSchemas
    if (version.usesDependentKeywords()) {
      loadDependentRequired(node, builder);
      loadDependentSchemas(node, builder);
      // Fall back to legacy "dependencies" if present and the new keywords are absent
      if (!node.has("dependentRequired") && !node.has("dependentSchemas")) {
        loadLegacyDependencies(node, builder);
      }
    } else {
      loadLegacyDependencies(node, builder);
    }

    // maxProperties / minProperties
    if (node.has("maxProperties") && node.get("maxProperties").isIntegralNumber()) {
      builder.maxProperties(node.get("maxProperties").asInt());
    }
    if (node.has("minProperties") && node.get("minProperties").isIntegralNumber()) {
      builder.minProperties(node.get("minProperties").asInt());
    }

    return buildBaseSchema(builder, node).build();
  }

  private void loadLegacyDependencies(JsonNode node, ObjectSchema.Builder builder) {
    JsonNode deps = node.get("dependencies");
    if (deps != null && deps.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = deps.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        if (entry.getValue().isArray()) {
          Set<String> depSet = new LinkedHashSet<>();
          for (JsonNode element : entry.getValue()) {
            if (element.isTextual()) {
              depSet.add(element.asText());
            }
          }
          builder.addPropertyDependency(entry.getKey(), depSet);
        } else if (entry.getValue().isObject()) {
          builder.addSchemaDependency(entry.getKey(), loadSchema(entry.getValue()));
        }
      }
    }
  }

  private void loadDependentRequired(JsonNode node, ObjectSchema.Builder builder) {
    JsonNode deps = node.get("dependentRequired");
    if (deps != null && deps.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = deps.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        if (entry.getValue().isArray()) {
          Set<String> depSet = new LinkedHashSet<>();
          for (JsonNode element : entry.getValue()) {
            if (element.isTextual()) {
              depSet.add(element.asText());
            }
          }
          builder.addPropertyDependency(entry.getKey(), depSet);
        }
      }
    }
  }

  private void loadDependentSchemas(JsonNode node, ObjectSchema.Builder builder) {
    JsonNode deps = node.get("dependentSchemas");
    if (deps != null && deps.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = deps.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        if (entry.getValue().isObject()) {
          builder.addSchemaDependency(entry.getKey(), loadSchema(entry.getValue()));
        }
      }
    }
  }

  // --- Array ---

  private Schema loadArraySchema(JsonNode node) {
    ArraySchema.Builder builder = ArraySchema.builder();

    if (version.usesPrefixItems()) {
      // Draft 2020-12: tuple items via "prefixItems", "items" is always a single schema
      JsonNode prefixItems = node.get("prefixItems");
      if (prefixItems != null && prefixItems.isArray()) {
        List<Schema> itemSchemaList = new ArrayList<>();
        for (JsonNode item : prefixItems) {
          itemSchemaList.add(loadSchema(item));
        }
        builder.itemSchemas(itemSchemaList);
      }
      JsonNode items = node.get("items");
      if (items != null) {
        if (items.isObject()) {
          builder.allItemSchema(loadSchema(items));
        } else if (items.isBoolean()) {
          builder.permitsAdditionalItems(items.asBoolean());
        }
      }
    } else {
      // Older drafts: "items" can be a single schema or an array (tuple)
      JsonNode items = node.get("items");
      if (items != null) {
        if (items.isObject()) {
          builder.allItemSchema(loadSchema(items));
        } else if (items.isArray()) {
          List<Schema> itemSchemaList = new ArrayList<>();
          for (JsonNode item : items) {
            itemSchemaList.add(loadSchema(item));
          }
          builder.itemSchemas(itemSchemaList);
        }
      }
    }

    // additionalItems (used by pre-2020-12 drafts, but harmless to check always)
    JsonNode ai = node.get("additionalItems");
    if (ai != null) {
      if (ai.isBoolean()) {
        builder.permitsAdditionalItems(ai.asBoolean());
      } else if (ai.isObject()) {
        builder.permitsAdditionalItems(true);
        builder.schemaOfAdditionalItems(loadSchema(ai));
      }
    }

    if (node.has("maxItems") && node.get("maxItems").isIntegralNumber()) {
      builder.maxItems(node.get("maxItems").asInt());
    }
    if (node.has("minItems") && node.get("minItems").isIntegralNumber()) {
      builder.minItems(node.get("minItems").asInt());
    }
    if (node.has("uniqueItems") && node.get("uniqueItems").isBoolean()) {
      builder.needsUniqueItems(node.get("uniqueItems").asBoolean());
    }

    return buildBaseSchema(builder, node).build();
  }

  // --- Helpers ---

  private String getTextualType(JsonNode node) {
    JsonNode typeNode = node.get("type");
    if (typeNode != null && typeNode.isTextual()) {
      return typeNode.asText();
    }
    return null;
  }

  private boolean isInferredObject(JsonNode node, String type) {
    if (type != null) return false;
    return node.has("properties") || node.has("additionalProperties")
        || node.has("patternProperties") || node.has("required")
        || node.has("minProperties") || node.has("maxProperties")
        || node.has("dependencies")
        || node.has("dependentRequired") || node.has("dependentSchemas");
  }

  private boolean isInferredArray(JsonNode node, String type) {
    if (type != null) return false;
    return node.has("items") || node.has("additionalItems")
        || node.has("minItems") || node.has("maxItems")
        || node.has("uniqueItems") || node.has("prefixItems");
  }

  /**
   * Sets common base schema fields (id, title, description, default, unprocessed properties).
   */
  @SuppressWarnings("unchecked")
  private <B extends Schema.Builder<B>> B buildBaseSchema(B builder, JsonNode node) {
    String idKeyword = version.idKeyword();
    if (node.has(idKeyword) && node.get(idKeyword).isTextual()) {
      builder.id(node.get(idKeyword).asText());
    }
    if (node.has("title") && node.get("title").isTextual()) {
      builder.title(node.get("title").asText());
    }
    if (node.has("description") && node.get("description").isTextual()) {
      builder.description(node.get("description").asText());
    }
    if (node.has("default")) {
      builder.defaultValue(nodeToValue(node.get("default")));
    }

    // Collect unprocessed properties (e.g., "connect.type")
    Map<String, JsonNode> unprocessed = new LinkedHashMap<>();
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      String key = entry.getKey();
      if (!isStandardKeyword(key)) {
        unprocessed.put(key, entry.getValue());
      }
    }
    if (!unprocessed.isEmpty()) {
      builder.unprocessedProperties(unprocessed);
    }

    return builder;
  }

  private static final Set<String> STANDARD_KEYWORDS = Set.of(
      "$id", "id", "$schema", "$ref", "title", "description", "default",
      "type", "properties", "required", "additionalProperties", "patternProperties",
      "dependencies", "dependentRequired", "dependentSchemas",
      "maxProperties", "minProperties",
      "items", "prefixItems", "additionalItems", "maxItems", "minItems", "uniqueItems",
      "maxLength", "minLength", "pattern",
      "maximum", "minimum", "exclusiveMaximum", "exclusiveMinimum", "multipleOf",
      "allOf", "anyOf", "oneOf", "not",
      "enum", "const",
      "definitions", "$defs", "format",
      "if", "then", "else", "readOnly", "writeOnly",
      "contentMediaType", "contentEncoding"
  );

  private boolean isStandardKeyword(String key) {
    return STANDARD_KEYWORDS.contains(key);
  }

  static Object nodeToValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isTextual()) {
      return node.asText();
    }
    if (node.isBoolean()) {
      return node.asBoolean();
    }
    if (node.isIntegralNumber()) {
      return node.asLong();
    }
    if (node.isFloatingPointNumber()) {
      return node.asDouble();
    }
    return node.toString();
  }
}
