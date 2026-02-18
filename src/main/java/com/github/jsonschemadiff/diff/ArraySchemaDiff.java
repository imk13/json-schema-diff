package com.github.jsonschemadiff.diff;

import com.github.jsonschemadiff.schema.ArraySchema;
import com.github.jsonschemadiff.schema.EmptySchema;
import com.github.jsonschemadiff.schema.FalseSchema;
import com.github.jsonschemadiff.schema.Schema;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.github.jsonschemadiff.diff.Difference.Type.ADDITIONAL_ITEMS_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.ADDITIONAL_ITEMS_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.ADDITIONAL_ITEMS_NARROWED;
import static com.github.jsonschemadiff.diff.Difference.Type.ADDITIONAL_ITEMS_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_ADDED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_ADDED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_ADDED_TO_CLOSED_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_ADDED_TO_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_REMOVED_FROM_CLOSED_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_REMOVED_FROM_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_REMOVED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_REMOVED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_WITH_EMPTY_SCHEMA_ADDED_TO_OPEN_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.ITEM_WITH_FALSE_REMOVED_FROM_CLOSED_CONTENT_MODEL;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_ITEMS_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_ITEMS_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_ITEMS_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_ITEMS_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_ITEMS_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_ITEMS_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_ITEMS_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_ITEMS_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.UNIQUE_ITEMS_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.UNIQUE_ITEMS_REMOVED;

class ArraySchemaDiff {

  static void compare(final Context ctx, final ArraySchema original, final ArraySchema update) {
    compareItemSchemaObject(ctx, original, update);
    compareItemSchemaArray(ctx, original, update);
    compareAdditionalItems(ctx, original, update);
    compareAttributes(ctx, original, update);
  }

  private static void compareAttributes(
      final Context ctx, final ArraySchema original, final ArraySchema update) {
    Integer origMaxItems = original.getMaxItems();
    Integer updMaxItems = update.getMaxItems();
    if (!Objects.equals(origMaxItems, updMaxItems)) {
      if (origMaxItems == null) {
        ctx.addDifference("maxItems", MAX_ITEMS_ADDED);
      } else if (updMaxItems == null) {
        ctx.addDifference("maxItems", MAX_ITEMS_REMOVED);
      } else if (origMaxItems < updMaxItems) {
        ctx.addDifference("maxItems", MAX_ITEMS_INCREASED);
      } else {
        ctx.addDifference("maxItems", MAX_ITEMS_DECREASED);
      }
    }

    Integer origMinItems = original.getMinItems();
    Integer updMinItems = update.getMinItems();
    if (!Objects.equals(origMinItems, updMinItems)) {
      if (origMinItems == null) {
        ctx.addDifference("minItems", MIN_ITEMS_ADDED);
      } else if (updMinItems == null) {
        ctx.addDifference("minItems", MIN_ITEMS_REMOVED);
      } else if (origMinItems < updMinItems) {
        ctx.addDifference("minItems", MIN_ITEMS_INCREASED);
      } else {
        ctx.addDifference("minItems", MIN_ITEMS_DECREASED);
      }
    }

    if (original.needsUniqueItems() != update.needsUniqueItems()) {
      if (original.needsUniqueItems()) {
        ctx.addDifference("uniqueItems", UNIQUE_ITEMS_REMOVED);
      } else {
        ctx.addDifference("uniqueItems", UNIQUE_ITEMS_ADDED);
      }
    }
  }

  private static void compareAdditionalItems(
      final Context ctx, final ArraySchema original, final ArraySchema update) {
    try (Context.PathScope pathScope = ctx.enterPath("additionalItems")) {
      boolean origPermits = original.permitsAdditionalItems();
      boolean updPermits = update.permitsAdditionalItems();
      if (origPermits != updPermits) {
        if (origPermits) {
          ctx.addDifference(ADDITIONAL_ITEMS_REMOVED);
        } else {
          ctx.addDifference(ADDITIONAL_ITEMS_ADDED);
        }
      } else {
        Schema origAiSchema = original.getSchemaOfAdditionalItems();
        Schema updAiSchema = update.getSchemaOfAdditionalItems();
        if (origAiSchema == null && updAiSchema != null) {
          ctx.addDifference(ADDITIONAL_ITEMS_NARROWED);
        } else if (updAiSchema == null && origAiSchema != null) {
          ctx.addDifference(ADDITIONAL_ITEMS_EXTENDED);
        } else {
          SchemaDiff.compare(ctx, origAiSchema, updAiSchema);
        }
      }
    }
  }

  private static void compareItemSchemaArray(
      final Context ctx, final ArraySchema original, final ArraySchema update) {
    List<Schema> originalSchemas = original.getItemSchemas();
    if (originalSchemas == null) {
      originalSchemas = Collections.emptyList();
    }
    List<Schema> updateSchemas = update.getItemSchemas();
    if (updateSchemas == null) {
      updateSchemas = Collections.emptyList();
    }
    int originalSize = originalSchemas.size();
    int updateSize = updateSchemas.size();

    final Iterator<Schema> originalIterator = originalSchemas.iterator();
    final Iterator<Schema> updateIterator = updateSchemas.iterator();
    int index = 0;
    while (originalIterator.hasNext() && index < Math.min(originalSize, updateSize)) {
      try (Context.PathScope pathScope = ctx.enterPath("items/" + index)) {
        SchemaDiff.compare(ctx, originalIterator.next(), updateIterator.next());
      }
      index++;
    }
    while (originalIterator.hasNext()) {
      try (Context.PathScope pathScope = ctx.enterPath("items/" + index)) {
        Schema originalSchema = originalIterator.next();
        if (isOpenContentModelForItems(update)) {
          ctx.addDifference(ITEM_REMOVED_FROM_OPEN_CONTENT_MODEL);
        } else {
          Schema schemaFromPartial = update.getSchemaOfAdditionalItems();
          if (schemaFromPartial != null) {
            final Context subctx = ctx.getSubcontext();
            SchemaDiff.compare(subctx, originalSchema, schemaFromPartial);
            ctx.addDifferences(subctx.getDifferences());
            if (subctx.isCompatible()) {
              ctx.addDifference(ITEM_REMOVED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
            } else {
              ctx.addDifference(ITEM_REMOVED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
            }
          } else {
            if (originalSchema instanceof FalseSchema) {
              ctx.addDifference(ITEM_WITH_FALSE_REMOVED_FROM_CLOSED_CONTENT_MODEL);
            } else {
              ctx.addDifference(ITEM_REMOVED_FROM_CLOSED_CONTENT_MODEL);
            }
          }
        }
      }
      index++;
    }
    while (updateIterator.hasNext()) {
      try (Context.PathScope pathScope = ctx.enterPath("items/" + index)) {
        Schema updateSchema = updateIterator.next();
        if (isOpenContentModelForItems(original)) {
          if (updateSchema instanceof EmptySchema) {
            ctx.addDifference(ITEM_WITH_EMPTY_SCHEMA_ADDED_TO_OPEN_CONTENT_MODEL);
          } else {
            ctx.addDifference(ITEM_ADDED_TO_OPEN_CONTENT_MODEL);
          }
        } else {
          Schema schemaFromPartial = original.getSchemaOfAdditionalItems();
          if (schemaFromPartial != null) {
            final Context subctx = ctx.getSubcontext();
            SchemaDiff.compare(subctx, schemaFromPartial, updateSchema);
            ctx.addDifferences(subctx.getDifferences());
            if (subctx.isCompatible()) {
              ctx.addDifference(ITEM_ADDED_IS_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
            } else {
              ctx.addDifference(ITEM_ADDED_NOT_COVERED_BY_PARTIALLY_OPEN_CONTENT_MODEL);
            }
          } else {
            ctx.addDifference(ITEM_ADDED_TO_CLOSED_CONTENT_MODEL);
          }
        }
      }
      index++;
    }
  }

  private static void compareItemSchemaObject(
      final Context ctx, final ArraySchema original, final ArraySchema update) {
    try (Context.PathScope pathScope = ctx.enterPath("items")) {
      SchemaDiff.compare(ctx, original.getAllItemSchema(), update.getAllItemSchema());
    }
  }

  private static boolean isOpenContentModelForItems(ArraySchema schema) {
    return schema.getSchemaOfAdditionalItems() == null
        && schema.permitsAdditionalItems();
  }
}
