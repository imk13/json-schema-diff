package com.github.jsonschemadiff.schema;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class EnumSchema extends Schema {

  private final Set<Object> possibleValues;

  private EnumSchema(Builder builder) {
    super(builder);
    this.possibleValues = builder.possibleValues != null
        ? Collections.unmodifiableSet(new LinkedHashSet<>(builder.possibleValues))
        : Collections.emptySet();
  }

  public Set<Object> getPossibleValues() {
    return possibleValues;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    private Set<Object> possibleValues;

    public Builder possibleValues(Set<Object> possibleValues) {
      this.possibleValues = possibleValues;
      return this;
    }

    public EnumSchema build() {
      return new EnumSchema(this);
    }
  }
}
