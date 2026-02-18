package com.github.jsonschemadiff.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Supported JSON Schema specification drafts.
 *
 * Each draft maps to one or more {@code $schema} URLs for auto-detection,
 * and exposes methods that describe the keyword semantics for that draft.
 */
public enum JsonSchemaVersion {

  DRAFT_4(List.of(
      "http://json-schema.org/draft-04/schema",
      "https://json-schema.org/draft-04/schema",
      "http://json-schema.org/draft-04/schema#",
      "https://json-schema.org/draft-04/schema#"
  )),

  DRAFT_6(List.of(
      "http://json-schema.org/draft-06/schema",
      "https://json-schema.org/draft-06/schema",
      "http://json-schema.org/draft-06/schema#",
      "https://json-schema.org/draft-06/schema#"
  )),

  DRAFT_7(List.of(
      "http://json-schema.org/draft-07/schema",
      "https://json-schema.org/draft-07/schema",
      "http://json-schema.org/draft-07/schema#",
      "https://json-schema.org/draft-07/schema#"
  )),

  DRAFT_2019_09(List.of(
      "http://json-schema.org/draft/2019-09/schema",
      "https://json-schema.org/draft/2019-09/schema",
      "http://json-schema.org/draft/2019-09/schema#",
      "https://json-schema.org/draft/2019-09/schema#"
  )),

  DRAFT_2020_12(List.of(
      "http://json-schema.org/draft/2020-12/schema",
      "https://json-schema.org/draft/2020-12/schema",
      "http://json-schema.org/draft/2020-12/schema#",
      "https://json-schema.org/draft/2020-12/schema#"
  ));

  private final List<String> schemaUrls;

  private static final Map<String, JsonSchemaVersion> URL_LOOKUP = new HashMap<>();

  static {
    for (JsonSchemaVersion v : values()) {
      for (String url : v.schemaUrls) {
        URL_LOOKUP.put(url, v);
      }
    }
  }

  JsonSchemaVersion(List<String> schemaUrls) {
    this.schemaUrls = schemaUrls;
  }

  public List<String> getSchemaUrls() {
    return schemaUrls;
  }

  /**
   * Detect the draft version from a {@code $schema} URL.
   * Returns {@code null} if the URL is not recognized.
   */
  public static JsonSchemaVersion fromSchemaUrl(String url) {
    if (url == null) {
      return null;
    }
    // Strip trailing # if present
    String normalized = url.endsWith("#") ? url.substring(0, url.length() - 1) : url;
    JsonSchemaVersion v = URL_LOOKUP.get(normalized);
    if (v != null) {
      return v;
    }
    return URL_LOOKUP.get(url);
  }

  /**
   * The keyword used for schema identity.
   * Draft-04 uses {@code "id"}; all later drafts use {@code "$id"}.
   */
  public String idKeyword() {
    return this == DRAFT_4 ? "id" : "$id";
  }

  /**
   * Whether {@code exclusiveMaximum} and {@code exclusiveMinimum} are numeric values.
   * In Draft-04 they are booleans that modify {@code maximum}/{@code minimum};
   * in Draft-06+ they are standalone numeric limits.
   */
  public boolean usesNumericExclusiveBounds() {
    return this != DRAFT_4;
  }

  /**
   * Whether tuple validation uses {@code prefixItems} (Draft 2020-12)
   * instead of an array-valued {@code items} (older drafts).
   */
  public boolean usesPrefixItems() {
    return this == DRAFT_2020_12;
  }

  /**
   * Whether this draft uses {@code dependentRequired}/{@code dependentSchemas}
   * instead of the combined {@code dependencies} keyword.
   */
  public boolean usesDependentKeywords() {
    return this == DRAFT_2019_09 || this == DRAFT_2020_12;
  }

  /**
   * Whether {@code const} is a supported keyword (Draft-06+).
   */
  public boolean supportsConst() {
    return this != DRAFT_4;
  }

  /**
   * The keyword used for reusable schema definitions.
   * Draft 2019-09+ prefer {@code "$defs"}; older drafts use {@code "definitions"}.
   */
  public String definitionsKeyword() {
    return usesDependentKeywords() ? "$defs" : "definitions";
  }
}
