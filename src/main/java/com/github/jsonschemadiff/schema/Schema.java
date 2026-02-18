package com.github.jsonschemadiff.schema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public abstract class Schema {

  private final String id;
  private final String title;
  private final String description;
  private final Object defaultValue;
  private final Map<String, JsonNode> unprocessedProperties;

  protected Schema(Builder<?> builder) {
    this.id = builder.id;
    this.title = builder.title;
    this.description = builder.description;
    this.defaultValue = builder.defaultValue;
    this.unprocessedProperties = builder.unprocessedProperties != null
        ? Collections.unmodifiableMap(builder.unprocessedProperties)
        : Collections.emptyMap();
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public boolean hasDefaultValue() {
    return defaultValue != null;
  }

  public Map<String, JsonNode> getUnprocessedProperties() {
    return unprocessedProperties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Schema schema = (Schema) o;
    return Objects.equals(id, schema.id)
        && Objects.equals(title, schema.title)
        && Objects.equals(description, schema.description)
        && Objects.equals(defaultValue, schema.defaultValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, description, defaultValue);
  }

  public abstract static class Builder<B extends Builder<B>> {
    private String id;
    private String title;
    private String description;
    private Object defaultValue;
    private Map<String, JsonNode> unprocessedProperties;

    @SuppressWarnings("unchecked")
    protected B self() {
      return (B) this;
    }

    public B id(String id) {
      this.id = id;
      return self();
    }

    public B title(String title) {
      this.title = title;
      return self();
    }

    public B description(String description) {
      this.description = description;
      return self();
    }

    public B defaultValue(Object defaultValue) {
      this.defaultValue = defaultValue;
      return self();
    }

    public B unprocessedProperties(Map<String, JsonNode> unprocessedProperties) {
      this.unprocessedProperties = unprocessedProperties;
      return self();
    }
  }
}
