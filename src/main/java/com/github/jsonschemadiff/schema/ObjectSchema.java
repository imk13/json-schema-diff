package com.github.jsonschemadiff.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ObjectSchema extends Schema {

  private final Map<String, Schema> propertySchemas;
  private final Set<String> requiredProperties;
  private final boolean permitsAdditionalProperties;
  private final Schema schemaOfAdditionalProperties;
  private final Map<Pattern, Schema> patternProperties;
  private final Map<String, Set<String>> propertyDependencies;
  private final Map<String, Schema> schemaDependencies;
  private final Integer maxProperties;
  private final Integer minProperties;

  private ObjectSchema(Builder builder) {
    super(builder);
    this.propertySchemas = builder.propertySchemas != null
        ? Collections.unmodifiableMap(builder.propertySchemas)
        : Collections.emptyMap();
    this.requiredProperties = builder.requiredProperties != null
        ? Collections.unmodifiableSet(builder.requiredProperties)
        : Collections.emptySet();
    this.permitsAdditionalProperties = builder.permitsAdditionalProperties;
    this.schemaOfAdditionalProperties = builder.schemaOfAdditionalProperties;
    this.patternProperties = builder.patternProperties != null
        ? Collections.unmodifiableMap(builder.patternProperties)
        : Collections.emptyMap();
    this.propertyDependencies = builder.propertyDependencies != null
        ? Collections.unmodifiableMap(builder.propertyDependencies)
        : Collections.emptyMap();
    this.schemaDependencies = builder.schemaDependencies != null
        ? Collections.unmodifiableMap(builder.schemaDependencies)
        : Collections.emptyMap();
    this.maxProperties = builder.maxProperties;
    this.minProperties = builder.minProperties;
  }

  public Map<String, Schema> getPropertySchemas() {
    return propertySchemas;
  }

  public Set<String> getRequiredProperties() {
    return requiredProperties;
  }

  public boolean permitsAdditionalProperties() {
    return permitsAdditionalProperties;
  }

  public Schema getSchemaOfAdditionalProperties() {
    return schemaOfAdditionalProperties;
  }

  public Map<Pattern, Schema> getPatternProperties() {
    return patternProperties;
  }

  public Map<String, Set<String>> getPropertyDependencies() {
    return propertyDependencies;
  }

  public Map<String, Schema> getSchemaDependencies() {
    return schemaDependencies;
  }

  public Integer getMaxProperties() {
    return maxProperties;
  }

  public Integer getMinProperties() {
    return minProperties;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends Schema.Builder<Builder> {
    private Map<String, Schema> propertySchemas;
    private Set<String> requiredProperties;
    private boolean permitsAdditionalProperties = true;
    private Schema schemaOfAdditionalProperties;
    private Map<Pattern, Schema> patternProperties;
    private Map<String, Set<String>> propertyDependencies;
    private Map<String, Schema> schemaDependencies;
    private Integer maxProperties;
    private Integer minProperties;

    public Builder addPropertySchema(String name, Schema schema) {
      if (this.propertySchemas == null) {
        this.propertySchemas = new LinkedHashMap<>();
      }
      this.propertySchemas.put(name, schema);
      return this;
    }

    public Builder addRequiredProperty(String name) {
      if (this.requiredProperties == null) {
        this.requiredProperties = new LinkedHashSet<>();
      }
      this.requiredProperties.add(name);
      return this;
    }

    public Builder permitsAdditionalProperties(boolean permits) {
      this.permitsAdditionalProperties = permits;
      return this;
    }

    public Builder schemaOfAdditionalProperties(Schema schema) {
      this.schemaOfAdditionalProperties = schema;
      return this;
    }

    public Builder addPatternProperty(Pattern pattern, Schema schema) {
      if (this.patternProperties == null) {
        this.patternProperties = new LinkedHashMap<>();
      }
      this.patternProperties.put(pattern, schema);
      return this;
    }

    public Builder addPropertyDependency(String property, Set<String> dependencies) {
      if (this.propertyDependencies == null) {
        this.propertyDependencies = new LinkedHashMap<>();
      }
      this.propertyDependencies.put(property, dependencies);
      return this;
    }

    public Builder addSchemaDependency(String property, Schema schema) {
      if (this.schemaDependencies == null) {
        this.schemaDependencies = new LinkedHashMap<>();
      }
      this.schemaDependencies.put(property, schema);
      return this;
    }

    public Builder maxProperties(Integer maxProperties) {
      this.maxProperties = maxProperties;
      return this;
    }

    public Builder minProperties(Integer minProperties) {
      this.minProperties = minProperties;
      return this;
    }

    public ObjectSchema build() {
      return new ObjectSchema(this);
    }
  }
}
