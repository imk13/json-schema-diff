package com.github.jsonschemadiff.diff;

import com.github.jsonschemadiff.schema.EnumSchema;

import java.util.Set;

import static com.github.jsonschemadiff.diff.Difference.Type.ENUM_ARRAY_CHANGED;
import static com.github.jsonschemadiff.diff.Difference.Type.ENUM_ARRAY_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.ENUM_ARRAY_NARROWED;

class EnumSchemaDiff {

  static void compare(final Context ctx, final EnumSchema original, final EnumSchema update) {
    Set<Object> origValues = original.getPossibleValues();
    Set<Object> updValues = update.getPossibleValues();
    if (!origValues.equals(updValues)) {
      if (updValues.containsAll(origValues)) {
        ctx.addDifference("enum", ENUM_ARRAY_EXTENDED);
      } else if (origValues.containsAll(updValues)) {
        ctx.addDifference("enum", ENUM_ARRAY_NARROWED);
      } else {
        ctx.addDifference("enum", ENUM_ARRAY_CHANGED);
      }
    }
  }
}
