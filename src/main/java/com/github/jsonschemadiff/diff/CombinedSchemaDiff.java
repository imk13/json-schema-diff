package com.github.jsonschemadiff.diff;

import com.github.jsonschemadiff.schema.CombinedSchema;
import com.github.jsonschemadiff.schema.CombinedSchema.ValidationCriterion;
import com.github.jsonschemadiff.schema.Schema;
import com.github.jsonschemadiff.utils.Edge;
import com.github.jsonschemadiff.utils.MaximumCardinalityMatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.github.jsonschemadiff.diff.Difference.Type.COMBINED_TYPE_CHANGED;
import static com.github.jsonschemadiff.diff.Difference.Type.COMBINED_TYPE_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.COMBINED_TYPE_SUBSCHEMAS_CHANGED;
import static com.github.jsonschemadiff.diff.Difference.Type.PRODUCT_TYPE_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.PRODUCT_TYPE_NARROWED;
import static com.github.jsonschemadiff.diff.Difference.Type.SUM_TYPE_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.SUM_TYPE_NARROWED;

class CombinedSchemaDiff {

  static void compare(
      final Context ctx, final CombinedSchema original, final CombinedSchema update) {
    ValidationCriterion origCriterion = original.getCriterion();
    ValidationCriterion updCriterion = update.getCriterion();

    List<Schema> origSubs = new ArrayList<>(original.getSubschemas());
    List<Schema> updSubs = new ArrayList<>(update.getSubschemas());

    Difference.Type type = compareCriteria(ctx, origCriterion, updCriterion, origSubs, updSubs);

    if (type != COMBINED_TYPE_CHANGED) {
      Set<SchemaWrapper> originalSubset = new LinkedHashSet<>();
      for (Schema sub : origSubs) {
        originalSubset.add(new SchemaWrapper(sub));
      }
      Set<SchemaWrapper> updateSubset = new LinkedHashSet<>();
      for (Schema sub : updSubs) {
        updateSubset.add(new SchemaWrapper(sub));
      }

      int originalSize = originalSubset.size();
      int updateSize = updateSubset.size();

      if (originalSize < updateSize) {
        if (updCriterion == CombinedSchema.ALL_CRITERION) {
          ctx.addDifference(PRODUCT_TYPE_EXTENDED);
        } else {
          ctx.addDifference(SUM_TYPE_EXTENDED);
        }
      } else if (originalSize > updateSize) {
        if (origCriterion == CombinedSchema.ANY_CRITERION
            || origCriterion == CombinedSchema.ONE_CRITERION) {
          ctx.addDifference(SUM_TYPE_NARROWED);
        } else {
          ctx.addDifference(PRODUCT_TYPE_NARROWED);
        }
      }

      int index = 0;
      Set<Edge<SchemaWrapper, List<Difference>>> compatibleEdges = new HashSet<>();
      for (SchemaWrapper origSub : originalSubset) {
        try (Context.PathScope pathScope = ctx.enterPath(
            origCriterion.getKeyword() + "/" + index)) {
          for (SchemaWrapper updSub : updateSubset) {
            final Context subctx = ctx.getSubcontext();
            SchemaDiff.compare(subctx, origSub.getSchema(), updSub.getSchema());
            if (subctx.isCompatible()) {
              compatibleEdges.add(
                  new Edge<>(origSub, updSub, subctx.getDifferences()));
            }
          }
        }
        index++;
      }

      MaximumCardinalityMatch<SchemaWrapper, List<Difference>> match =
          new MaximumCardinalityMatch<>(compatibleEdges, originalSubset, updateSubset);
      Set<Edge<SchemaWrapper, List<Difference>>> matching = match.getMatching();

      for (Edge<SchemaWrapper, List<Difference>> matchingEdge : matching) {
        ctx.addDifferences(matchingEdge.value());
      }
      if (matching.size() < Math.min(originalSize, updateSize)) {
        ctx.addDifference(COMBINED_TYPE_SUBSCHEMAS_CHANGED);
      }
    }
  }

  private static Difference.Type compareCriteria(
      final Context ctx,
      ValidationCriterion origCriterion, ValidationCriterion updCriterion,
      List<Schema> origSubs, List<Schema> updSubs) {
    Difference.Type type;
    if (origCriterion == updCriterion) {
      type = null;
    } else if (updCriterion == CombinedSchema.ANY_CRITERION
        || (isSingleton(origSubs) && isSingleton(updSubs))
        || (isSingleton(origSubs) && updCriterion == CombinedSchema.ONE_CRITERION)
        || (isSingleton(updSubs) && origCriterion == CombinedSchema.ALL_CRITERION)) {
      type = COMBINED_TYPE_EXTENDED;
    } else {
      type = COMBINED_TYPE_CHANGED;
    }
    if (type != null) {
      ctx.addDifference(type);
    }
    return type;
  }

  private static boolean isSingleton(Collection<Schema> subs) {
    return subs != null && subs.size() == 1;
  }

  static class SchemaWrapper {
    private final Schema schema;

    SchemaWrapper(Schema schema) {
      this.schema = schema;
    }

    Schema getSchema() {
      return schema;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SchemaWrapper that = (SchemaWrapper) o;
      return schema.equals(that.schema);
    }

    @Override
    public int hashCode() {
      return schema.hashCode();
    }
  }
}
