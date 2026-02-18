package com.github.jsonschemadiff.schema;

public class NotSchema extends Schema {

  private final Schema mustNotMatch;

  private NotSchema(Builder builder) {
    super(builder);
    this.mustNotMatch = builder.mustNotMatch;
  }

  public Schema getMustNotMatch() {
    return mustNotMatch;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    private Schema mustNotMatch;

    public Builder mustNotMatch(Schema mustNotMatch) {
      this.mustNotMatch = mustNotMatch;
      return this;
    }

    public NotSchema build() {
      return new NotSchema(this);
    }
  }
}
