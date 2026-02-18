package com.github.jsonschemadiff.schema;

public class ConstSchema extends Schema {

  private final Object permittedValue;

  private ConstSchema(Builder builder) {
    super(builder);
    this.permittedValue = builder.permittedValue;
  }

  public Object getPermittedValue() {
    return permittedValue;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    private Object permittedValue;

    public Builder permittedValue(Object permittedValue) {
      this.permittedValue = permittedValue;
      return this;
    }

    public ConstSchema build() {
      return new ConstSchema(this);
    }
  }
}
