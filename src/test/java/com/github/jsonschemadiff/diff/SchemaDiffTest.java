package com.github.jsonschemadiff.diff;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonschemadiff.schema.JsonSchemaVersion;
import com.github.jsonschemadiff.schema.Schema;
import com.github.jsonschemadiff.schema.SchemaLoader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Test;

public class SchemaDiffTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void checkJsonSchemaCompatibility() throws Exception {
    ArrayNode testCases = (ArrayNode) MAPPER.readTree(
        Objects.requireNonNull(readFile("diff-schema-examples.json")));
    checkJsonSchemaCompatibility(testCases);
  }

  @Test
  public void checkJsonSchemaCompatibilityForCombinedSchemas() throws Exception {
    ArrayNode testCases = (ArrayNode) MAPPER.readTree(
        Objects.requireNonNull(readFile("diff-combined-schema-examples.json")));
    checkJsonSchemaCompatibility(testCases);
  }

  private void checkJsonSchemaCompatibility(ArrayNode testCases) {
    for (JsonNode testCaseNode : testCases) {
      ObjectNode testCase = (ObjectNode) testCaseNode;
      JsonNode originalSchemaNode = testCase.get("original_schema");
      JsonNode updateSchemaNode = testCase.get("update_schema");
      ArrayNode changesArray = (ArrayNode) testCase.get("changes");
      boolean isCompatible = testCase.get("compatible").asBoolean();
      String description = testCase.get("description").asText();

      Schema originalSchema = SchemaLoader.load(originalSchemaNode);
      Schema updateSchema = SchemaLoader.load(updateSchemaNode);

      List<String> expectedChanges = new java.util.ArrayList<>();
      for (JsonNode change : changesArray) {
        expectedChanges.add(change.asText());
      }

      List<Difference> differences = SchemaDiff.compare(originalSchema, updateSchema);
      final List<Difference> incompatibleDiffs = differences.stream()
          .filter(diff -> !SchemaDiff.COMPATIBLE_CHANGES_STRICT.contains(diff.getType()))
          .collect(Collectors.toList());

      assertThat(description,
          differences.stream()
              .map(change -> change.getType().toString() + " " + change.getJsonPath())
              .collect(toList()),
          is(expectedChanges));
      assertEquals(description, isCompatible, incompatibleDiffs.isEmpty());

      boolean isCompatibleLenient = isCompatible;
      if (testCase.has("compatible_lenient")) {
        isCompatibleLenient = testCase.get("compatible_lenient").asBoolean();
      }
      List<Difference> differencesLenient = SchemaDiff.compare(
          SchemaDiff.COMPATIBLE_CHANGES_LENIENT, originalSchema, updateSchema);
      final List<Difference> incompatibleDiffsLenient = differences.stream()
          .filter(diff -> !SchemaDiff.COMPATIBLE_CHANGES_LENIENT.contains(diff.getType()))
          .collect(Collectors.toList());
      assertEquals(description, isCompatibleLenient, incompatibleDiffsLenient.isEmpty());
    }
  }

  @Test
  public void testSchemaAddsProperties() throws Exception {
    Schema first = SchemaLoader.load("{}");
    Schema second = SchemaLoader.load("{\"properties\": {}}");
    List<Difference> changes = SchemaDiff.compare(first, second);
    assertFalse(changes.isEmpty());
  }

  @Test
  public void testConnectTypeAsBytes() throws Exception {
    String firstSchema = "{\"type\":\"string\",\"title\":\"org.apache.kafka.connect.data.Decimal\","
        + "\"connect.version\":1,\"connect.type\":\"bytes\",\"connect.parameters\":{\"scale\":\"2\"}}";
    String secondSchema = "{\"type\":\"number\",\"title\":\"org.apache.kafka.connect.data.Decimal\","
        + "\"connect.version\":1,\"connect.type\":\"bytes\",\"connect.parameters\":{\"scale\":\"2\"}}";
    Schema first = SchemaLoader.load(firstSchema);
    Schema second = SchemaLoader.load(secondSchema);
    List<Difference> changes = SchemaDiff.compare(first, second);
    assertTrue(changes.isEmpty());
  }

  @Test
  public void testStringConvenienceApi() {
    List<Difference> changes = SchemaDiff.compare(
        "{\"type\":\"string\",\"maxLength\":10}",
        "{\"type\":\"string\",\"maxLength\":20}");
    assertEquals(1, changes.size());
    assertEquals(Difference.Type.MAX_LENGTH_INCREASED, changes.get(0).getType());
  }

  @Test
  public void testIdenticalSchemasNoDifferences() throws Exception {
    String schema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
    Schema node1 = SchemaLoader.load(schema);
    Schema node2 = SchemaLoader.load(schema);
    List<Difference> changes = SchemaDiff.compare(node1, node2);
    assertTrue(changes.isEmpty());
  }

  // --- Draft-04: boolean exclusiveMaximum/exclusiveMinimum ---

  @Test
  public void testDraft04BooleanExclusiveMaximum() {
    String original = "{\"type\":\"number\",\"maximum\":100,\"exclusiveMaximum\":true}";
    String update   = "{\"type\":\"number\",\"maximum\":200,\"exclusiveMaximum\":true}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_4);
    assertEquals(2, diffs.size());
    assertEquals(Difference.Type.MAXIMUM_INCREASED, diffs.get(0).getType());
    assertEquals(Difference.Type.EXCLUSIVE_MAXIMUM_INCREASED, diffs.get(1).getType());
  }

  @Test
  public void testDraft04BooleanExclusiveMinimum() {
    String original = "{\"type\":\"number\",\"minimum\":10,\"exclusiveMinimum\":true}";
    String update   = "{\"type\":\"number\",\"minimum\":5,\"exclusiveMinimum\":true}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_4);
    assertEquals(2, diffs.size());
    assertEquals(Difference.Type.MINIMUM_DECREASED, diffs.get(0).getType());
    assertEquals(Difference.Type.EXCLUSIVE_MINIMUM_DECREASED, diffs.get(1).getType());
  }

  @Test
  public void testDraft04ExclusiveMaximumAddedAsBoolean() {
    String original = "{\"type\":\"number\",\"maximum\":100}";
    String update   = "{\"type\":\"number\",\"maximum\":100,\"exclusiveMaximum\":true}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_4);
    assertEquals(1, diffs.size());
    assertEquals(Difference.Type.EXCLUSIVE_MAXIMUM_ADDED, diffs.get(0).getType());
  }

  // --- Draft-04: "id" keyword ---

  @Test
  public void testDraft04UsesIdNotDollarId() {
    String original = "{\"id\":\"urn:example:orig\",\"type\":\"string\"}";
    String update   = "{\"id\":\"urn:example:updated\",\"type\":\"string\"}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_4);
    assertEquals(1, diffs.size());
    assertEquals(Difference.Type.ID_CHANGED, diffs.get(0).getType());
  }

  @Test
  public void testDraft07UsesDollarId() {
    String original = "{\"$id\":\"urn:example:orig\",\"type\":\"string\"}";
    String update   = "{\"$id\":\"urn:example:updated\",\"type\":\"string\"}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_7);
    assertEquals(1, diffs.size());
    assertEquals(Difference.Type.ID_CHANGED, diffs.get(0).getType());
  }

  // --- Draft 2019-09: dependentRequired / dependentSchemas ---

  @Test
  public void testDraft201909DependentRequiredAdded() {
    String original = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}";
    String update = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},"
        + "\"dependentRequired\":{\"a\":[\"b\"]}}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_2019_09);
    boolean hasDependencyAdded = diffs.stream()
        .anyMatch(d -> d.getType() == Difference.Type.DEPENDENCY_ARRAY_ADDED);
    assertTrue("Expected DEPENDENCY_ARRAY_ADDED for dependentRequired", hasDependencyAdded);
  }

  @Test
  public void testDraft201909DependentSchemasAdded() {
    String original = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}";
    String update = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},"
        + "\"dependentSchemas\":{\"a\":{\"properties\":{\"b\":{\"type\":\"integer\"}}}}}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_2019_09);
    boolean hasDependencyAdded = diffs.stream()
        .anyMatch(d -> d.getType() == Difference.Type.DEPENDENCY_SCHEMA_ADDED);
    assertTrue("Expected DEPENDENCY_SCHEMA_ADDED for dependentSchemas", hasDependencyAdded);
  }

  @Test
  public void testDraft201909DependentRequiredNarrowed() {
    String original = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},"
        + "\"dependentRequired\":{\"a\":[\"b\",\"c\"]}}";
    String update = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},"
        + "\"dependentRequired\":{\"a\":[\"b\"]}}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_2019_09);
    boolean hasNarrowed = diffs.stream()
        .anyMatch(d -> d.getType() == Difference.Type.DEPENDENCY_ARRAY_NARROWED);
    assertTrue("Expected DEPENDENCY_ARRAY_NARROWED", hasNarrowed);
  }

  // --- Draft 2020-12: prefixItems ---

  @Test
  public void testDraft202012PrefixItemsIdentical() {
    String schema = "{\"type\":\"array\",\"prefixItems\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}";
    List<Difference> diffs = SchemaDiff.compare(schema, schema, JsonSchemaVersion.DRAFT_2020_12);
    assertTrue("Identical prefixItems schemas should produce no diffs", diffs.isEmpty());
  }

  @Test
  public void testDraft202012PrefixItemsTypeChanged() {
    String original = "{\"type\":\"array\",\"prefixItems\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}";
    String update   = "{\"type\":\"array\",\"prefixItems\":[{\"type\":\"string\"},{\"type\":\"string\"}]}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_2020_12);
    boolean hasTypeChanged = diffs.stream()
        .anyMatch(d -> d.getType() == Difference.Type.TYPE_CHANGED);
    assertTrue("Expected TYPE_CHANGED for prefixItems element", hasTypeChanged);
  }

  @Test
  public void testDraft202012PrefixItemsItemAdded() {
    String original = "{\"type\":\"array\",\"prefixItems\":[{\"type\":\"string\"}]}";
    String update   = "{\"type\":\"array\",\"prefixItems\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}";
    List<Difference> diffs = SchemaDiff.compare(original, update, JsonSchemaVersion.DRAFT_2020_12);
    assertFalse("Should detect added prefixItem", diffs.isEmpty());
  }

  // --- Auto-detection from $schema URL ---

  @Test
  public void testAutoDetectDraft04() {
    String original = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\","
        + "\"type\":\"number\",\"maximum\":100,\"exclusiveMaximum\":true}";
    String update   = "{\"$schema\":\"http://json-schema.org/draft-04/schema#\","
        + "\"type\":\"number\",\"maximum\":200,\"exclusiveMaximum\":true}";
    // Without specifying version -- should auto-detect Draft-04
    Schema orig = SchemaLoader.load(original);
    Schema upd  = SchemaLoader.load(update);
    List<Difference> diffs = SchemaDiff.compare(orig, upd);
    boolean hasExclusiveMaxInc = diffs.stream()
        .anyMatch(d -> d.getType() == Difference.Type.EXCLUSIVE_MAXIMUM_INCREASED);
    assertTrue("Auto-detect should handle Draft-04 boolean exclusiveMaximum", hasExclusiveMaxInc);
  }

  @Test
  public void testAutoDetectDraft202012() {
    String schema = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
        + "\"type\":\"array\",\"prefixItems\":[{\"type\":\"string\"}]}";
    Schema loaded = SchemaLoader.load(schema);
    // Should parse as array with tuple items from prefixItems
    List<Difference> diffs = SchemaDiff.compare(loaded, loaded);
    assertTrue("Identical 2020-12 schema should produce no diffs", diffs.isEmpty());
  }

  @Test
  public void testAutoDetectFallbackToDraft7() {
    // No $schema present -- should default to Draft-7
    SchemaLoader loader = new SchemaLoader(
        MAPPER.createObjectNode().put("type", "string"));
    assertEquals(JsonSchemaVersion.DRAFT_7, loader.getVersion());
  }

  // --- Cross-draft comparison ---

  @Test
  public void testCrossDraftComparison() {
    String draft07Original = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},"
        + "\"dependencies\":{\"a\":[\"b\"]}}";
    String draft2019Update = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},"
        + "\"dependentRequired\":{\"a\":[\"b\"]}}";
    Schema orig = SchemaLoader.load(draft07Original, JsonSchemaVersion.DRAFT_7);
    Schema upd  = SchemaLoader.load(draft2019Update, JsonSchemaVersion.DRAFT_2019_09);
    List<Difference> diffs = SchemaDiff.compare(orig, upd);
    assertTrue("Equivalent dependencies across drafts should produce no diffs", diffs.isEmpty());
  }

  // --- JsonSchemaVersion enum ---

  @Test
  public void testFromSchemaUrlKnownDrafts() {
    assertEquals(JsonSchemaVersion.DRAFT_4,
        JsonSchemaVersion.fromSchemaUrl("http://json-schema.org/draft-04/schema#"));
    assertEquals(JsonSchemaVersion.DRAFT_6,
        JsonSchemaVersion.fromSchemaUrl("http://json-schema.org/draft-06/schema#"));
    assertEquals(JsonSchemaVersion.DRAFT_7,
        JsonSchemaVersion.fromSchemaUrl("http://json-schema.org/draft-07/schema#"));
    assertEquals(JsonSchemaVersion.DRAFT_2019_09,
        JsonSchemaVersion.fromSchemaUrl("https://json-schema.org/draft/2019-09/schema"));
    assertEquals(JsonSchemaVersion.DRAFT_2020_12,
        JsonSchemaVersion.fromSchemaUrl("https://json-schema.org/draft/2020-12/schema"));
  }

  @Test
  public void testFromSchemaUrlUnknownReturnsNull() {
    assertThat(JsonSchemaVersion.fromSchemaUrl("http://example.com/custom-schema"),
        is((JsonSchemaVersion) null));
    assertThat(JsonSchemaVersion.fromSchemaUrl(null),
        is((JsonSchemaVersion) null));
  }

  @Test
  public void testVersionKeywordBehaviors() {
    assertEquals("id", JsonSchemaVersion.DRAFT_4.idKeyword());
    assertEquals("$id", JsonSchemaVersion.DRAFT_7.idKeyword());
    assertFalse(JsonSchemaVersion.DRAFT_4.usesNumericExclusiveBounds());
    assertTrue(JsonSchemaVersion.DRAFT_7.usesNumericExclusiveBounds());
    assertFalse(JsonSchemaVersion.DRAFT_7.usesPrefixItems());
    assertTrue(JsonSchemaVersion.DRAFT_2020_12.usesPrefixItems());
    assertFalse(JsonSchemaVersion.DRAFT_7.usesDependentKeywords());
    assertTrue(JsonSchemaVersion.DRAFT_2019_09.usesDependentKeywords());
    assertTrue(JsonSchemaVersion.DRAFT_2020_12.usesDependentKeywords());
  }

  public static String readFile(String fileName) {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    InputStream is = classLoader.getResourceAsStream(fileName);
    if (is != null) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      return reader.lines().collect(Collectors.joining(System.lineSeparator()));
    }
    return null;
  }
}
