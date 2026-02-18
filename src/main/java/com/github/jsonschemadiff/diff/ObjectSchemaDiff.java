package com.github.jsonschemadiff.diff;

import com.github.jsonschemadiff.schema.EmptySchema;
import com.github.jsonschemadiff.schema.FalseSchema;
import com.github.jsonschemadiff.schema.ObjectSchema;
import com.github.jsonschemadiff.schema.Schema;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static com.github.jsonschemadiff.diff.Difference.Type.ADDITIONAL_PROPERTIES_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.ADDITIONAL_PROPERTIES_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.ADDITIONAL_PROPERTIES_NARROWED;
import static com.github.jsonschemadiff.diff.Difference.Type.ADDITIONAL_PROPERTIES_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.DEPENDENCY_ARRAY_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.DEPENDENCY_ARRAY_CHANGED;
import static com.github.jsonschemadiff.diff.Difference.Type.DEPENDENCY_ARRAY_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.DEPENDENCY_ARRAY_NARROWED;
import static com.github.jsonschemadiff.diff.Difference.Type.DEPENDENCY_ARRAY_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.DEPENDENCY_SCHEMA_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.DEPENDENCY_SCHEMA_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_PROPERTIES_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_PROPERTIES_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_PROPERTIES_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_PROPERTIES_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_PROPERTIES_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_PROPERTIES_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_PROPERTIES_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_PROPERTIES_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.OPTIONAL_PROPERTY_ADDED_TO_UNOPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_ADDED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_ADDED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_ADDED_TO_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_REMOVED_FROM_CLOSED_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_REMOVED_FROM_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_REMOVED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_REMOVED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_WITH_EMPTY_SCHEMA_ADDED_TO_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.PROPERTY_WITH_FALSE_REMOVED_FROM_CLOSED_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.REQUIRED_ATTRIBUTE_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.REQUIRED_ATTRIBUTE_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.REQUIRED_ATTRIBUTE_WITH_DEFAULT_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.REQUIRED_PROPERTY_ADDED_TO_UNOPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.REQUIRED_PROPERTY_WITH_DEFAULT_ADDED_TO_UNOPEN_CONTENT_MODEL;

class ObjectSchemaDiff {

  static void compare(final Context ctx, final ObjectSchema original, final ObjectSchema update) {
    compareRequired(ctx, original, update);
    compareProperties(ctx, original, update);
    compareDependencies(ctx, original, update);
    compareAdditionalProperties(ctx, original, update);
    compareAttributes(ctx, original, update);
  }

  private static void compareAttributes(
      final Context ctx, final ObjectSchema original, final ObjectSchema update) {
    Integer origMaxProps = original.getMaxProperties();
    Integer updMaxProps = update.getMaxProperties();
    if (!Objects.equals(origMaxProps, updMaxProps)) {
      if (origMaxProps == null) {
        ctx.addDifference("maxProperties", MAX_PROPERTIES_ADDED);
      } else if (updMaxProps == null) {
        ctx.addDifference("maxProperties", MAX_PROPERTIES_REMOVED);
      } else if (origMaxProps < updMaxProps) {
        ctx.addDifference("maxProperties", MAX_PROPERTIES_INCREASED);
      } else {
        ctx.addDifference("maxProperties", MAX_PROPERTIES_DECREASED);
      }
    }

    Integer origMinProps = original.getMinProperties();
    Integer updMinProps = update.getMinProperties();
    if (!Objects.equals(origMinProps, updMinProps)) {
      if (origMinProps == null) {
        ctx.addDifference("minProperties", MIN_PROPERTIES_ADDED);
      } else if (updMinProps == null) {
        ctx.addDifference("minProperties", MIN_PROPERTIES_REMOVED);
      } else if (origMinProps < updMinProps) {
        ctx.addDifference("minProperties", MIN_PROPERTIES_INCREASED);
      } else {
        ctx.addDifference("minProperties", MIN_PROPERTIES_DECREASED);
      }
    }
  }

  private static void compareAdditionalProperties(
      final Context ctx, final ObjectSchema original, final ObjectSchema update) {
    try (Context.PathScope pathScope = ctx.enterPath("additionalProperties")) {
      boolean origPermits = original.permitsAdditionalProperties();
      boolean updPermits = update.permitsAdditionalProperties();

      if (origPermits != updPermits) {
        if (updPermits) {
          ctx.addDifference(ADDITIONAL_PROPERTIES_ADDED);
        } else {
          ctx.addDifference(ADDITIONAL_PROPERTIES_REMOVED);
        }
      } else {
        Schema origApSchema = original.getSchemaOfAdditionalProperties();
        Schema updApSchema = update.getSchemaOfAdditionalProperties();
        if (origApSchema == null && updApSchema != null) {
          ctx.addDifference(ADDITIONAL_PROPERTIES_NARROWED);
        } else if (updApSchema == null && origApSchema != null) {
          ctx.addDifference(ADDITIONAL_PROPERTIES_EXTENDED);
        } else {
          SchemaDiff.compare(ctx, origApSchema, updApSchema);
        }
      }
    }
  }

  private static void compareDependencies(
      final Context ctx, final ObjectSchema original, final ObjectSchema update) {
    try (Context.PathScope pathScope = ctx.enterPath("dependencies")) {
      Map<String, Set<String>> origPropDeps = original.getPropertyDependencies();
      Map<String, Set<String>> updPropDeps = update.getPropertyDependencies();

      Set<String> propertyKeys = new HashSet<>(origPropDeps.keySet());
      propertyKeys.addAll(updPropDeps.keySet());

      for (String propertyKey : propertyKeys) {
        try (Context.PathScope pathScope2 = ctx.enterPath(propertyKey)) {
          Set<String> originalDeps = origPropDeps.get(propertyKey);
          Set<String> updateDeps = updPropDeps.get(propertyKey);
          if (updateDeps == null) {
            ctx.addDifference(DEPENDENCY_ARRAY_REMOVED);
          } else if (originalDeps == null) {
            ctx.addDifference(DEPENDENCY_ARRAY_ADDED);
          } else if (!originalDeps.equals(updateDeps)) {
            if (updateDeps.containsAll(originalDeps)) {
              ctx.addDifference(DEPENDENCY_ARRAY_EXTENDED);
            } else if (originalDeps.containsAll(updateDeps)) {
              ctx.addDifference(DEPENDENCY_ARRAY_NARROWED);
            } else {
              ctx.addDifference(DEPENDENCY_ARRAY_CHANGED);
            }
          }
        }
      }

      Map<String, Schema> origSchemaDeps = original.getSchemaDependencies();
      Map<String, Schema> updSchemaDeps = update.getSchemaDependencies();

      propertyKeys = new HashSet<>(origSchemaDeps.keySet());
      propertyKeys.addAll(updSchemaDeps.keySet());

      for (String propertyKey : propertyKeys) {
        try (Context.PathScope pathScope2 = ctx.enterPath(propertyKey)) {
          Schema originalSchema = origSchemaDeps.get(propertyKey);
          Schema updateSchema = updSchemaDeps.get(propertyKey);
          if (updateSchema == null) {
            ctx.addDifference(DEPENDENCY_SCHEMA_REMOVED);
          } else if (originalSchema == null) {
            ctx.addDifference(DEPENDENCY_SCHEMA_ADDED);
          } else {
            SchemaDiff.compare(ctx, originalSchema, updateSchema);
          }
        }
      }
    }
  }

  private static void compareProperties(
      final Context ctx, final ObjectSchema original, final ObjectSchema update) {
    try (Context.PathScope pathScope = ctx.enterPath("properties")) {
      Map<String, Schema> origProps = original.getPropertySchemas();
      Map<String, Schema> updProps = update.getPropertySchemas();
      Set<String> updRequired = update.getRequiredProperties();

      Set<String> propertyKeys = new HashSet<>(origProps.keySet());
      propertyKeys.addAll(updProps.keySet());

      for (String propertyKey : propertyKeys) {
        try (Context.PathScope pathScope2 = ctx.enterPath(propertyKey)) {
          Schema originalSchema = origProps.get(propertyKey);
          Schema updateSchema = updProps.get(propertyKey);
          if (updateSchema == null) {
            if (isOpenContentModel(update)) {
              ctx.addDifference(PROPERTY_REMOVED_FROM_OPEN_CONTENT_MODEL);
            } else {
              Schema schemaFromPartial = schemaFromPartiallyOpenContentModel(
                  update, propertyKey);
              if (schemaFromPartial != null) {
                final Context subctx = ctx.getSubcontext();
                SchemaDiff.compare(subctx, originalSchema, schemaFromPartial);
                ctx.addDifferences(subctx.getDifferences());
                if (subctx.isCompatible()) {
                  ctx.addDifference(
                      PROPERTY_REMOVED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
                } else {
                  ctx.addDifference(
                      PROPERTY_REMOVED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
                }
              } else {
                if (originalSchema instanceof FalseSchema) {
                  ctx.addDifference(PROPERTY_WITH_FALSE_REMOVED_FROM_CLOSED_CONTENT_MODEL);
                } else {
                  ctx.addDifference(PROPERTY_REMOVED_FROM_CLOSED_CONTENT_MODEL);
                }
              }
            }
          } else if (originalSchema == null) {
            if (isOpenContentModel(original)) {
              if (updateSchema instanceof EmptySchema) {
                ctx.addDifference(PROPERTY_WITH_EMPTY_SCHEMA_ADDED_TO_OPEN_CONTENT_MODEL);
              } else {
                ctx.addDifference(PROPERTY_ADDED_TO_OPEN_CONTENT_MODEL);
              }
            } else {
              Schema schemaFromPartial = schemaFromPartiallyOpenContentModel(
                  original, propertyKey);
              if (schemaFromPartial != null) {
                final Context subctx = ctx.getSubcontext();
                SchemaDiff.compare(subctx, schemaFromPartial, updateSchema);
                ctx.addDifferences(subctx.getDifferences());
                if (subctx.isCompatible()) {
                  ctx.addDifference(
                      PROPERTY_ADDED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
                } else {
                  ctx.addDifference(
                      PROPERTY_ADDED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
                }
              }
              if (updRequired.contains(propertyKey)) {
                if (updateSchema.hasDefaultValue()) {
                  ctx.addDifference(
                      REQUIRED_PROPERTY_WITH_DEFAULT_ADDED_TO_UNOPEN_CONTENT_MODEL);
                } else {
                  ctx.addDifference(REQUIRED_PROPERTY_ADDED_TO_UNOPEN_CONTENT_MODEL);
                }
              } else {
                ctx.addDifference(OPTIONAL_PROPERTY_ADDED_TO_UNOPEN_CONTENT_MODEL);
              }
            }
          } else {
            SchemaDiff.compare(ctx, originalSchema, updateSchema);
          }
        }
      }
    }
  }

  private static void compareRequired(
      final Context ctx, final ObjectSchema original, final ObjectSchema update) {
    try (Context.PathScope pathScope = ctx.enterPath("required")) {
      Map<String, Schema> origProps = original.getPropertySchemas();
      Map<String, Schema> updProps = update.getPropertySchemas();
      Set<String> origRequired = original.getRequiredProperties();
      Set<String> updRequired = update.getRequiredProperties();

      for (String propertyKey : origProps.keySet()) {
        if (updProps.containsKey(propertyKey)) {
          try (Context.PathScope pathScope2 = ctx.enterPath(propertyKey)) {
            boolean originalRequired = origRequired.contains(propertyKey);
            boolean updateRequired = updRequired.contains(propertyKey);
            if (originalRequired && !updateRequired) {
              ctx.addDifference(REQUIRED_ATTRIBUTE_REMOVED);
            } else if (!originalRequired && updateRequired) {
              Schema updPropSchema = updProps.get(propertyKey);
              if (updPropSchema != null && updPropSchema.hasDefaultValue()) {
                ctx.addDifference(REQUIRED_ATTRIBUTE_WITH_DEFAULT_ADDED);
              } else {
                ctx.addDifference(REQUIRED_ATTRIBUTE_ADDED);
              }
            }
          }
        }
      }
    }
  }

  private static boolean isOpenContentModel(ObjectSchema schema) {
    return schema.getPatternProperties().isEmpty()
        && schema.getSchemaOfAdditionalProperties() == null
        && schema.permitsAdditionalProperties();
  }

  private static Schema schemaFromPartiallyOpenContentModel(
      final ObjectSchema schema, final String propertyKey) {
    Map<Pattern, Schema> patternProps = schema.getPatternProperties();
    for (Map.Entry<Pattern, Schema> entry : patternProps.entrySet()) {
      if (entry.getKey().matcher(propertyKey).find()) {
        return entry.getValue();
      }
    }
    return schema.getSchemaOfAdditionalProperties();
  }
}
