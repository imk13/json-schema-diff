package com.github.jsonschemadiff.schema;

public class FalseSchema extends Schema {

  private FalseSchema(Builder builder) {
    super(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    public FalseSchema build() {
      return new FalseSchema(this);
    }
  }
}
