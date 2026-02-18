package com.github.jsonschemadiff.diff;

import com.github.jsonschemadiff.schema.StringSchema;

import java.util.Objects;

import static com.github.jsonschemadiff.diff.Difference.Type.MAX_LENGTH_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_LENGTH_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_LENGTH_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAX_LENGTH_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_LENGTH_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_LENGTH_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_LENGTH_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MIN_LENGTH_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.PATTERN_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.PATTERN_CHANGED;
import static com.github.jsonschemadiff.diff.Difference.Type.PATTERN_REMOVED;

class StringSchemaDiff {

  static void compare(final Context ctx, final StringSchema original, final StringSchema update) {
    Integer origMaxLength = original.getMaxLength();
    Integer updMaxLength = update.getMaxLength();
    if (!Objects.equals(origMaxLength, updMaxLength)) {
      if (origMaxLength == null) {
        ctx.addDifference("maxLength", MAX_LENGTH_ADDED);
      } else if (updMaxLength == null) {
        ctx.addDifference("maxLength", MAX_LENGTH_REMOVED);
      } else if (origMaxLength < updMaxLength) {
        ctx.addDifference("maxLength", MAX_LENGTH_INCREASED);
      } else {
        ctx.addDifference("maxLength", MAX_LENGTH_DECREASED);
      }
    }

    Integer origMinLength = original.getMinLength();
    Integer updMinLength = update.getMinLength();
    if (!Objects.equals(origMinLength, updMinLength)) {
      if (origMinLength == null) {
        ctx.addDifference("minLength", MIN_LENGTH_ADDED);
      } else if (updMinLength == null) {
        ctx.addDifference("minLength", MIN_LENGTH_REMOVED);
      } else if (origMinLength < updMinLength) {
        ctx.addDifference("minLength", MIN_LENGTH_INCREASED);
      } else {
        ctx.addDifference("minLength", MIN_LENGTH_DECREASED);
      }
    }

    String origPattern = original.getPattern() != null ? original.getPattern().pattern() : null;
    String updPattern = update.getPattern() != null ? update.getPattern().pattern() : null;
    if (origPattern == null && updPattern != null) {
      ctx.addDifference("pattern", PATTERN_ADDED);
    } else if (origPattern != null && updPattern == null) {
      ctx.addDifference("pattern", PATTERN_REMOVED);
    } else if (origPattern != null && !origPattern.equals(updPattern)) {
      ctx.addDifference("pattern", PATTERN_CHANGED);
    }
  }
}
