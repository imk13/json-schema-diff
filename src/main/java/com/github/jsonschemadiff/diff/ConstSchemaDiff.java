package com.github.jsonschemadiff.diff;

import com.github.jsonschemadiff.schema.ConstSchema;

import java.util.Objects;

import static com.github.jsonschemadiff.diff.Difference.Type.ENUM_ARRAY_CHANGED;

class ConstSchemaDiff {

  static void compare(final Context ctx, final ConstSchema original, final ConstSchema update) {
    if (!Objects.equals(original.getPermittedValue(), update.getPermittedValue())) {
      ctx.addDifference("const", ENUM_ARRAY_CHANGED);
    }
  }
}
