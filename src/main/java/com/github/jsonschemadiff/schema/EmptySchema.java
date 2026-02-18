package com.github.jsonschemadiff.schema;

public class EmptySchema extends Schema {

  private EmptySchema(Builder builder) {
    super(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    public EmptySchema build() {
      return new EmptySchema(this);
    }
  }
}
