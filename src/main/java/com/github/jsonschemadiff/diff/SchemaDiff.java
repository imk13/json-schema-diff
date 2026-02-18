package com.github.jsonschemadiff.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonschemadiff.diff.Difference.Type;
import com.github.jsonschemadiff.schema.ArraySchema;
import com.github.jsonschemadiff.schema.CombinedSchema;
import com.github.jsonschemadiff.schema.ConstSchema;
import com.github.jsonschemadiff.schema.EmptySchema;
import com.github.jsonschemadiff.schema.EnumSchema;
import com.github.jsonschemadiff.schema.FalseSchema;
import com.github.jsonschemadiff.schema.NotSchema;
import com.github.jsonschemadiff.schema.NumberSchema;
import com.github.jsonschemadiff.schema.ObjectSchema;
import com.github.jsonschemadiff.schema.Schema;
import com.github.jsonschemadiff.schema.JsonSchemaVersion;
import com.github.jsonschemadiff.schema.SchemaLoader;
import com.github.jsonschemadiff.schema.StringSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SchemaDiff {

  public static final Set<Difference.Type> COMPATIBLE_CHANGES_LENIENT;
  public static final Set<Difference.Type> COMPATIBLE_CHANGES_STRICT;

  private static final String CONNECT_TYPE_PROP = "connect.type";
  private static final String BYTES_VAL = "bytes";

  static {
    Set<Difference.Type> changes = new HashSet<>();

    changes.add(Type.ID_CHANGED);
    changes.add(Type.DESCRIPTION_CHANGED);
    changes.add(Type.TITLE_CHANGED);
    changes.add(Type.DEFAULT_CHANGED);
    changes.add(Type.SCHEMA_REMOVED);
    changes.add(Type.TYPE_EXTENDED);

    changes.add(Type.MAX_LENGTH_INCREASED);
    changes.add(Type.MAX_LENGTH_REMOVED);
    changes.add(Type.MIN_LENGTH_DECREASED);
    changes.add(Type.MIN_LENGTH_REMOVED);
    changes.add(Type.PATTERN_REMOVED);

    changes.add(Type.MAXIMUM_INCREASED);
    changes.add(Type.MAXIMUM_REMOVED);
    changes.add(Type.MINIMUM_DECREASED);
    changes.add(Type.MINIMUM_REMOVED);
    changes.add(Type.EXCLUSIVE_MAXIMUM_INCREASED);
    changes.add(Type.EXCLUSIVE_MAXIMUM_REMOVED);
    changes.add(Type.EXCLUSIVE_MINIMUM_DECREASED);
    changes.add(Type.EXCLUSIVE_MINIMUM_REMOVED);
    changes.add(Type.MULTIPLE_OF_REDUCED);
    changes.add(Type.MULTIPLE_OF_REMOVED);

    changes.add(Type.REQUIRED_ATTRIBUTE_WITH_DEFAULT_ADDED);
    changes.add(Type.REQUIRED_ATTRIBUTE_REMOVED);
    changes.add(Type.DEPENDENCY_ARRAY_NARROWED);
    changes.add(Type.DEPENDENCY_ARRAY_REMOVED);
    changes.add(Type.DEPENDENCY_SCHEMA_REMOVED);
    changes.add(Type.MAX_PROPERTIES_INCREASED);
    changes.add(Type.MAX_PROPERTIES_REMOVED);
    changes.add(Type.MIN_PROPERTIES_DECREASED);
    changes.add(Type.MIN_PROPERTIES_REMOVED);
    changes.add(Type.ADDITIONAL_PROPERTIES_ADDED);
    changes.add(Type.ADDITIONAL_PROPERTIES_EXTENDED);
    changes.add(Type.PROPERTY_WITH_EMPTY_SCHEMA_ADDED_TO_OPEN_CONTENT_MODEL);
    changes.add(Type.REQUIRED_PROPERTY_WITH_DEFAULT_ADDED_TO_UNOPEN_CONTENT_MODEL);
    changes.add(Type.OPTIONAL_PROPERTY_ADDED_TO_UNOPEN_CONTENT_MODEL);
    changes.add(Type.PROPERTY_WITH_FALSE_REMOVED_FROM_CLOSED_CONTENT_MODEL);
    changes.add(Type.PROPERTY_REMOVED_FROM_OPEN_CONTENT_MODEL);
    changes.add(Type.PROPERTY_ADDED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
    changes.add(Type.PROPERTY_REMOVED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);

    changes.add(Type.MAX_ITEMS_INCREASED);
    changes.add(Type.MAX_ITEMS_REMOVED);
    changes.add(Type.MIN_ITEMS_DECREASED);
    changes.add(Type.MIN_ITEMS_REMOVED);
    changes.add(Type.UNIQUE_ITEMS_REMOVED);
    changes.add(Type.ADDITIONAL_ITEMS_ADDED);
    changes.add(Type.ADDITIONAL_ITEMS_EXTENDED);
    changes.add(Type.ITEM_WITH_EMPTY_SCHEMA_ADDED_TO_OPEN_CONTENT_MODEL);
    changes.add(Type.ITEM_ADDED_TO_CLOSED_CONTENT_MODEL);
    changes.add(Type.ITEM_WITH_FALSE_REMOVED_FROM_CLOSED_CONTENT_MODEL);
    changes.add(Type.ITEM_REMOVED_FROM_OPEN_CONTENT_MODEL);
    changes.add(Type.ITEM_ADDED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
    changes.add(Type.ITEM_REMOVED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);

    changes.add(Type.ENUM_ARRAY_EXTENDED);

    changes.add(Type.COMBINED_TYPE_EXTENDED);
    changes.add(Type.PRODUCT_TYPE_NARROWED);
    changes.add(Type.SUM_TYPE_EXTENDED);
    changes.add(Type.NOT_TYPE_NARROWED);

    COMPATIBLE_CHANGES_STRICT = Collections.unmodifiableSet(new HashSet<>(changes));

    changes.add(Type.ADDITIONAL_PROPERTIES_NARROWED);
    changes.add(Type.ADDITIONAL_PROPERTIES_REMOVED);
    changes.add(Type.PROPERTY_ADDED_TO_OPEN_CONTENT_MODEL);
    changes.add(Type.PROPERTY_REMOVED_FROM_OPEN_CONTENT_MODEL);
    changes.add(Type.PROPERTY_ADDED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
    changes.add(Type.PROPERTY_REMOVED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);

    COMPATIBLE_CHANGES_LENIENT = Collections.unmodifiableSet(new HashSet<>(changes));
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static List<Difference> compare(
      Set<Difference.Type> compatibleChanges, final Schema original, final Schema update) {
    final Context ctx = new Context(compatibleChanges);
    compare(ctx, original, update);
    return ctx.getDifferences();
  }

  public static List<Difference> compare(final Schema original, final Schema update) {
    return compare(COMPATIBLE_CHANGES_STRICT, original, update);
  }

  // Convenience: accept JsonNode and parse via SchemaLoader
  public static List<Difference> compare(
      Set<Difference.Type> compatibleChanges, final JsonNode original, final JsonNode update) {
    Schema origSchema = SchemaLoader.load(original);
    Schema updSchema = SchemaLoader.load(update);
    return compare(compatibleChanges, origSchema, updSchema);
  }

  public static List<Difference> compare(final JsonNode original, final JsonNode update) {
    return compare(COMPATIBLE_CHANGES_STRICT, original, update);
  }

  public static List<Difference> compare(final String originalJson, final String updateJson) {
    try {
      Schema original = SchemaLoader.load(originalJson);
      Schema update = SchemaLoader.load(updateJson);
      return compare(original, update);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON schema", e);
    }
  }

  // --- Version-aware overloads ---

  public static List<Difference> compare(
      final JsonNode original, final JsonNode update, JsonSchemaVersion version) {
    return compare(COMPATIBLE_CHANGES_STRICT,
        SchemaLoader.load(original, version),
        SchemaLoader.load(update, version));
  }

  public static List<Difference> compare(
      final String originalJson, final String updateJson, JsonSchemaVersion version) {
    try {
      return compare(COMPATIBLE_CHANGES_STRICT,
          SchemaLoader.load(originalJson, version),
          SchemaLoader.load(updateJson, version));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON schema", e);
    }
  }

  public static List<Difference> compare(
      Set<Difference.Type> compatibleChanges,
      final JsonNode original, final JsonNode update,
      JsonSchemaVersion origVersion, JsonSchemaVersion updVersion) {
    return compare(compatibleChanges,
        SchemaLoader.load(original, origVersion),
        SchemaLoader.load(update, updVersion));
  }

  public static List<Difference> compare(
      Set<Difference.Type> compatibleChanges,
      final String originalJson, final String updateJson,
      JsonSchemaVersion origVersion, JsonSchemaVersion updVersion) {
    try {
      return compare(compatibleChanges,
          SchemaLoader.load(originalJson, origVersion),
          SchemaLoader.load(updateJson, updVersion));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON schema", e);
    }
  }

  @SuppressWarnings("ConstantConditions")
  static void compare(final Context ctx, Schema original, Schema update) {
    if (original == null && update == null) {
      return;
    } else if (original == null) {
      ctx.addDifference(Type.SCHEMA_ADDED);
      return;
    } else if (update == null) {
      ctx.addDifference(Type.SCHEMA_REMOVED);
      return;
    }

    // Handle combined vs non-combined asymmetry
    boolean origIsCombined = original instanceof CombinedSchema;
    boolean updIsCombined = update instanceof CombinedSchema;

    if (!origIsCombined && updIsCombined) {
      CombinedSchema updCombined = (CombinedSchema) update;
      List<Schema> updateSubs = new ArrayList<>(updCombined.getSubschemas());
      if (updateSubs.size() == 1) {
        final Context subctx = ctx.getSubcontext();
        compare(subctx, original, updateSubs.get(0));
        if (subctx.isCompatible()) {
          ctx.addDifferences(subctx.getDifferences());
          return;
        }
      } else {
        CombinedSchema.ValidationCriterion criterion = updCombined.getCriterion();
        if (criterion == CombinedSchema.ANY_CRITERION
            || criterion == CombinedSchema.ONE_CRITERION) {
          for (Schema subschema : updateSubs) {
            final Context subctx = ctx.getSubcontext();
            compare(subctx, original, subschema);
            if (subctx.isCompatible()) {
              ctx.addDifferences(subctx.getDifferences());
              ctx.addDifference(Type.SUM_TYPE_EXTENDED);
              return;
            }
          }
        }
      }
    } else if (origIsCombined && !updIsCombined) {
      CombinedSchema origCombined = (CombinedSchema) original;
      List<Schema> originalSubs = new ArrayList<>(origCombined.getSubschemas());
      if (originalSubs.size() == 1) {
        final Context subctx = ctx.getSubcontext();
        compare(subctx, originalSubs.get(0), update);
        if (subctx.isCompatible()) {
          ctx.addDifferences(subctx.getDifferences());
          return;
        }
      }
      if (origCombined.getCriterion() == CombinedSchema.ALL_CRITERION) {
        for (Schema subschema : originalSubs) {
          final Context subctx = ctx.getSubcontext();
          compare(subctx, subschema, update);
          if (subctx.isCompatible()) {
            ctx.addDifferences(subctx.getDifferences());
            ctx.addDifference(Type.PRODUCT_TYPE_NARROWED);
            return;
          }
        }
      }
    }

    if (!schemaTypesEqual(original, update)) {
      if (original instanceof FalseSchema || update instanceof EmptySchema) {
        return;
      }

      String originalConnectType = getUnprocessedStringProp(original, CONNECT_TYPE_PROP);
      String updateConnectType = getUnprocessedStringProp(update, CONNECT_TYPE_PROP);
      if (BYTES_VAL.equals(originalConnectType) && BYTES_VAL.equals(updateConnectType)) {
        return;
      }

      ctx.addDifference(Type.TYPE_CHANGED);
      return;
    }

    try (Context.SchemaScope schemaScope = ctx.enterSchema(original)) {
      if (schemaScope != null) {
        if (!Objects.equals(original.getId(), update.getId())) {
          ctx.addDifference(Type.ID_CHANGED);
        }
        if (!Objects.equals(original.getTitle(), update.getTitle())) {
          ctx.addDifference(Type.TITLE_CHANGED);
        }
        if (!Objects.equals(original.getDescription(), update.getDescription())) {
          ctx.addDifference(Type.DESCRIPTION_CHANGED);
        }
        if (!Objects.equals(original.getDefaultValue(), update.getDefaultValue())) {
          ctx.addDifference(Type.DEFAULT_CHANGED);
        }

        if (original instanceof StringSchema) {
          StringSchemaDiff.compare(ctx, (StringSchema) original, (StringSchema) update);
        } else if (original instanceof NumberSchema) {
          NumberSchemaDiff.compare(ctx, (NumberSchema) original, (NumberSchema) update);
        } else if (original instanceof ConstSchema) {
          ConstSchemaDiff.compare(ctx, (ConstSchema) original, (ConstSchema) update);
        } else if (original instanceof EnumSchema) {
          EnumSchemaDiff.compare(ctx, (EnumSchema) original, (EnumSchema) update);
        } else if (original instanceof CombinedSchema) {
          CombinedSchemaDiff.compare(ctx, (CombinedSchema) original, (CombinedSchema) update);
        } else if (original instanceof NotSchema) {
          NotSchemaDiff.compare(ctx, (NotSchema) original, (NotSchema) update);
        } else if (original instanceof ObjectSchema) {
          ObjectSchemaDiff.compare(ctx, (ObjectSchema) original, (ObjectSchema) update);
        } else if (original instanceof ArraySchema) {
          ArraySchemaDiff.compare(ctx, (ArraySchema) original, (ArraySchema) update);
        }
      }
    }
  }

  private static boolean schemaTypesEqual(Schema s1, Schema s2) {
    if (s1 instanceof CombinedSchema && s2 instanceof CombinedSchema) {
      return true;
    }
    if (s1 instanceof NumberSchema && s2 instanceof NumberSchema) {
      return true;
    }
    if (s1 instanceof EmptySchema && s2 instanceof EmptySchema) {
      return true;
    }
    return s1.getClass().equals(s2.getClass());
  }

  private static String getUnprocessedStringProp(Schema schema, String prop) {
    JsonNode node = schema.getUnprocessedProperties().get(prop);
    if (node != null && node.isTextual()) {
      return node.asText();
    }
    return null;
  }
}
