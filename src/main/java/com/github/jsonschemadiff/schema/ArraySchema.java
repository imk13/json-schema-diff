package com.github.jsonschemadiff.schema;

import java.util.Collections;
import java.util.List;

public class ArraySchema extends Schema {

  private final Schema allItemSchema;
  private final List<Schema> itemSchemas;
  private final boolean permitsAdditionalItems;
  private final Schema schemaOfAdditionalItems;
  private final Integer maxItems;
  private final Integer minItems;
  private final boolean needsUniqueItems;

  private ArraySchema(Builder builder) {
    super(builder);
    this.allItemSchema = builder.allItemSchema;
    this.itemSchemas = builder.itemSchemas != null
        ? Collections.unmodifiableList(builder.itemSchemas)
        : null;
    this.permitsAdditionalItems = builder.permitsAdditionalItems;
    this.schemaOfAdditionalItems = builder.schemaOfAdditionalItems;
    this.maxItems = builder.maxItems;
    this.minItems = builder.minItems;
    this.needsUniqueItems = builder.needsUniqueItems;
  }

  public Schema getAllItemSchema() {
    return allItemSchema;
  }

  public List<Schema> getItemSchemas() {
    return itemSchemas;
  }

  public boolean permitsAdditionalItems() {
    return permitsAdditionalItems;
  }

  public Schema getSchemaOfAdditionalItems() {
    return schemaOfAdditionalItems;
  }

  public Integer getMaxItems() {
    return maxItems;
  }

  public Integer getMinItems() {
    return minItems;
  }

  public boolean needsUniqueItems() {
    return needsUniqueItems;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    private Schema allItemSchema;
    private List<Schema> itemSchemas;
    private boolean permitsAdditionalItems = true;
    private Schema schemaOfAdditionalItems;
    private Integer maxItems;
    private Integer minItems;
    private boolean needsUniqueItems;

    public Builder allItemSchema(Schema schema) {
      this.allItemSchema = schema;
      return this;
    }

    public Builder itemSchemas(List<Schema> schemas) {
      this.itemSchemas = schemas;
      return this;
    }

    public Builder permitsAdditionalItems(boolean permits) {
      this.permitsAdditionalItems = permits;
      return this;
    }

    public Builder schemaOfAdditionalItems(Schema schema) {
      this.schemaOfAdditionalItems = schema;
      return this;
    }

    public Builder maxItems(Integer maxItems) {
      this.maxItems = maxItems;
      return this;
    }

    public Builder minItems(Integer minItems) {
      this.minItems = minItems;
      return this;
    }

    public Builder needsUniqueItems(boolean needsUniqueItems) {
      this.needsUniqueItems = needsUniqueItems;
      return this;
    }

    public ArraySchema build() {
      return new ArraySchema(this);
    }
  }
}
