package com.github.jsonschemadiff.schema;

import java.util.regex.Pattern;

public class StringSchema extends Schema {

  private final Integer maxLength;
  private final Integer minLength;
  private final Pattern pattern;

  private StringSchema(Builder builder) {
    super(builder);
    this.maxLength = builder.maxLength;
    this.minLength = builder.minLength;
    this.pattern = builder.pattern;
  }

  public Integer getMaxLength() {
    return maxLength;
  }

  public Integer getMinLength() {
    return minLength;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    private Integer maxLength;
    private Integer minLength;
    private Pattern pattern;

    public Builder maxLength(Integer maxLength) {
      this.maxLength = maxLength;
      return this;
    }

    public Builder minLength(Integer minLength) {
      this.minLength = minLength;
      return this;
    }

    public Builder pattern(String pattern) {
      this.pattern = pattern != null ? Pattern.compile(pattern) : null;
      return this;
    }

    public StringSchema build() {
      return new StringSchema(this);
    }
  }
}
