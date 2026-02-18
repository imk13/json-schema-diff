package com.github.jsonschemadiff.diff;

import com.github.jsonschemadiff.schema.NumberSchema;

import java.math.BigDecimal;
import java.util.Objects;

import static com.github.jsonschemadiff.diff.Difference.Type.EXCLUSIVE_MAXIMUM_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.EXCLUSIVE_MAXIMUM_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.EXCLUSIVE_MAXIMUM_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.EXCLUSIVE_MAXIMUM_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.EXCLUSIVE_MINIMUM_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.EXCLUSIVE_MINIMUM_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.EXCLUSIVE_MINIMUM_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.EXCLUSIVE_MINIMUM_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAXIMUM_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAXIMUM_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAXIMUM_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MAXIMUM_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.MINIMUM_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MINIMUM_DECREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MINIMUM_INCREASED;
import static com.github.jsonschemadiff.diff.Difference.Type.MINIMUM_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.MULTIPLE_OF_ADDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MULTIPLE_OF_CHANGED;
import static com.github.jsonschemadiff.diff.Difference.Type.MULTIPLE_OF_EXPANDED;
import static com.github.jsonschemadiff.diff.Difference.Type.MULTIPLE_OF_REDUCED;
import static com.github.jsonschemadiff.diff.Difference.Type.MULTIPLE_OF_REMOVED;
import static com.github.jsonschemadiff.diff.Difference.Type.TYPE_EXTENDED;
import static com.github.jsonschemadiff.diff.Difference.Type.TYPE_NARROWED;

class NumberSchemaDiff {

  static void compare(final Context ctx, final NumberSchema original, final NumberSchema update) {
    BigDecimal origMax = toBigDecimal(original.getMaximum());
    BigDecimal updMax = toBigDecimal(update.getMaximum());
    if (!Objects.equals(origMax, updMax)) {
      if (origMax == null) {
        ctx.addDifference("maximum", MAXIMUM_ADDED);
      } else if (updMax == null) {
        ctx.addDifference("maximum", MAXIMUM_REMOVED);
      } else if (origMax.compareTo(updMax) < 0) {
        ctx.addDifference("maximum", MAXIMUM_INCREASED);
      } else {
        ctx.addDifference("maximum", MAXIMUM_DECREASED);
      }
    }

    BigDecimal origMin = toBigDecimal(original.getMinimum());
    BigDecimal updMin = toBigDecimal(update.getMinimum());
    if (!Objects.equals(origMin, updMin)) {
      if (origMin == null) {
        ctx.addDifference("minimum", MINIMUM_ADDED);
      } else if (updMin == null) {
        ctx.addDifference("minimum", MINIMUM_REMOVED);
      } else if (origMin.compareTo(updMin) < 0) {
        ctx.addDifference("minimum", MINIMUM_INCREASED);
      } else {
        ctx.addDifference("minimum", MINIMUM_DECREASED);
      }
    }

    BigDecimal origExMax = toBigDecimal(original.getExclusiveMaximumLimit());
    BigDecimal updExMax = toBigDecimal(update.getExclusiveMaximumLimit());
    if (!Objects.equals(origExMax, updExMax)) {
      if (origExMax == null) {
        ctx.addDifference("exclusiveMaximum", EXCLUSIVE_MAXIMUM_ADDED);
      } else if (updExMax == null) {
        ctx.addDifference("exclusiveMaximum", EXCLUSIVE_MAXIMUM_REMOVED);
      } else if (origExMax.compareTo(updExMax) < 0) {
        ctx.addDifference("exclusiveMaximum", EXCLUSIVE_MAXIMUM_INCREASED);
      } else {
        ctx.addDifference("exclusiveMaximum", EXCLUSIVE_MAXIMUM_DECREASED);
      }
    }

    BigDecimal origExMin = toBigDecimal(original.getExclusiveMinimumLimit());
    BigDecimal updExMin = toBigDecimal(update.getExclusiveMinimumLimit());
    if (!Objects.equals(origExMin, updExMin)) {
      if (origExMin == null) {
        ctx.addDifference("exclusiveMinimum", EXCLUSIVE_MINIMUM_ADDED);
      } else if (updExMin == null) {
        ctx.addDifference("exclusiveMinimum", EXCLUSIVE_MINIMUM_REMOVED);
      } else if (origExMin.compareTo(updExMin) < 0) {
        ctx.addDifference("exclusiveMinimum", EXCLUSIVE_MINIMUM_INCREASED);
      } else {
        ctx.addDifference("exclusiveMinimum", EXCLUSIVE_MINIMUM_DECREASED);
      }
    }

    BigDecimal origMultipleOf = toBigDecimal(original.getMultipleOf());
    BigDecimal updMultipleOf = toBigDecimal(update.getMultipleOf());
    if (!Objects.equals(origMultipleOf, updMultipleOf)) {
      if (origMultipleOf == null) {
        ctx.addDifference("multipleOf", MULTIPLE_OF_ADDED);
      } else if (updMultipleOf == null) {
        ctx.addDifference("multipleOf", MULTIPLE_OF_REMOVED);
      } else if (updMultipleOf.intValue() != 0
          && origMultipleOf.intValue() != 0
          && updMultipleOf.intValue() % origMultipleOf.intValue() == 0) {
        ctx.addDifference("multipleOf", MULTIPLE_OF_EXPANDED);
      } else if (origMultipleOf.intValue() != 0
          && updMultipleOf.intValue() != 0
          && origMultipleOf.intValue() % updMultipleOf.intValue() == 0) {
        ctx.addDifference("multipleOf", MULTIPLE_OF_REDUCED);
      } else {
        ctx.addDifference("multipleOf", MULTIPLE_OF_CHANGED);
      }
    }

    if (original.requiresInteger() != update.requiresInteger()) {
      if (original.requiresInteger()) {
        ctx.addDifference(TYPE_EXTENDED);
      } else {
        ctx.addDifference(TYPE_NARROWED);
      }
    }
  }

  private static BigDecimal toBigDecimal(Number n) {
    if (n == null) return null;
    if (n instanceof BigDecimal) return (BigDecimal) n;
    return new BigDecimal(n.toString());
  }
}
