package com.github.jsonschemadiff.diff;

import com.github.jsonschemadiff.schema.NotSchema;

import static com.github.jsonschemadiff.diff.Difference.Type.NOT_TYPE_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.NOT_TYPE_NARROWED;

class NotSchemaDiff {

  static void compare(final Context ctx, final NotSchema original, final NotSchema update) {
    try (Context.PathScope pathScope = ctx.enterPath("not")) {
      final Context subctx = ctx.getSubcontext();
      SchemaDiff.compare(subctx, update.getMustNotMatch(), original.getMustNotMatch());
      if (subctx.isCompatible()) {
        ctx.addDifference(NOT_TYPE_NARROWED);
      } else {
        ctx.addDifference(NOT_TYPE_EXTENDED);
      }
    }
  }
}
