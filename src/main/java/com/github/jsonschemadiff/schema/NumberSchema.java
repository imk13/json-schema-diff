package com.github.jsonschemadiff.schema;

import java.math.BigDecimal;

public class NumberSchema extends Schema {

  private final Number maximum;
  private final Number minimum;
  private final Number exclusiveMaximumLimit;
  private final Number exclusiveMinimumLimit;
  private final Number multipleOf;
  private final boolean requiresInteger;

  private NumberSchema(Builder builder) {
    super(builder);
    this.maximum = builder.maximum;
    this.minimum = builder.minimum;
    this.exclusiveMaximumLimit = builder.exclusiveMaximumLimit;
    this.exclusiveMinimumLimit = builder.exclusiveMinimumLimit;
    this.multipleOf = builder.multipleOf;
    this.requiresInteger = builder.requiresInteger;
  }

  public Number getMaximum() {
    return maximum;
  }

  public Number getMinimum() {
    return minimum;
  }

  public Number getExclusiveMaximumLimit() {
    return exclusiveMaximumLimit;
  }

  public Number getExclusiveMinimumLimit() {
    return exclusiveMinimumLimit;
  }

  public Number getMultipleOf() {
    return multipleOf;
  }

  public boolean requiresInteger() {
    return requiresInteger;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    private Number maximum;
    private Number minimum;
    private Number exclusiveMaximumLimit;
    private Number exclusiveMinimumLimit;
    private Number multipleOf;
    private boolean requiresInteger;

    public Builder maximum(Number maximum) {
      this.maximum = maximum;
      return this;
    }

    public Builder minimum(Number minimum) {
      this.minimum = minimum;
      return this;
    }

    public Builder exclusiveMaximumLimit(Number exclusiveMaximumLimit) {
      this.exclusiveMaximumLimit = exclusiveMaximumLimit;
      return this;
    }

    public Builder exclusiveMinimumLimit(Number exclusiveMinimumLimit) {
      this.exclusiveMinimumLimit = exclusiveMinimumLimit;
      return this;
    }

    public Builder multipleOf(Number multipleOf) {
      this.multipleOf = multipleOf;
      return this;
    }

    public Builder requiresInteger(boolean requiresInteger) {
      this.requiresInteger = requiresInteger;
      return this;
    }

    public NumberSchema build() {
      return new NumberSchema(this);
    }
  }
}
