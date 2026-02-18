package com.github.jsonschemadiff.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class CombinedSchema extends Schema {

  public enum ValidationCriterion {
    ALL("allOf"),
    ANY("anyOf"),
    ONE("oneOf");

    private final String keyword;

    ValidationCriterion(String keyword) {
      this.keyword = keyword;
    }

    public String getKeyword() {
      return keyword;
    }

    @Override
    public String toString() {
      return keyword;
    }
  }

  public static final ValidationCriterion ALL_CRITERION = ValidationCriterion.ALL;
  public static final ValidationCriterion ANY_CRITERION = ValidationCriterion.ANY;
  public static final ValidationCriterion ONE_CRITERION = ValidationCriterion.ONE;

  private final ValidationCriterion criterion;
  private final Set<Schema> subschemas;

  private CombinedSchema(Builder builder) {
    super(builder);
    this.criterion = builder.criterion;
    this.subschemas = builder.subschemas != null
        ? Collections.unmodifiableSet(new LinkedHashSet<>(builder.subschemas))
        : Collections.emptySet();
  }

  public ValidationCriterion getCriterion() {
    return criterion;
  }

  public Collection<Schema> getSubschemas() {
    return subschemas;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    private ValidationCriterion criterion;
    private Collection<Schema> subschemas;

    public Builder criterion(ValidationCriterion criterion) {
      this.criterion = criterion;
      return this;
    }

    public Builder subschemas(Collection<Schema> subschemas) {
      this.subschemas = subschemas;
      return this;
    }

    public CombinedSchema build() {
      return new CombinedSchema(this);
    }
  }
}
