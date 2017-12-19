package org.talend.components.jdbc.query;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.SchemaBuilder.FieldAssembler;
import org.junit.Test;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.daikon.avro.AvroUtils;
import org.talend.daikon.avro.SchemaConstants;

// TODO convert the System.out to Assert, now the test results are all right.
public class QueryGeneratorTest {

    @Test
    public void testCommonQueryGeneratorWithStringLiteral() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("General JDBC", null, null, "\"mytable\"", setting);
        System.out.println(result);
    }

    @Test
    public void testCommonQueryGeneratorWithStringLiteralAndDBCatalogAndDBSchema() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("General JDBC", "\"mydatabase\"", "\"mydbschema\"", "\"mytable\"", setting);
        System.out.println(result);
    }

    @Test
    public void testCommonQueryGeneratorWithContext() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("General JDBC", null, null, "context.mytable", setting);
        System.out.println(result);
    }

    @Test
    public void testCommonQueryGeneratorWithContextAndDBCatalogAndDBSchema() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("General JDBC", "context.mydatabase", "context.mydbschema", "context.mytable",
                setting);
        System.out.println(result);
    }

    @Test
    public void testCommonQueryGeneratorWithComplexVarAndDBCatalogAndDBSchema() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("General JDBC", "context.mydatabase + \"mydatabasesubfix\"",
                "context.mydbschema + \"mydbschemasubfix\"", "context.mytable + \"mytablesubfix\"", setting);
        System.out.println(result);
    }

    @Test
    public void testCommonQueryGeneratorWithComplexVar() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("General JDBC", null, null, "context.mytable + \"mytablesubfix\"", setting);
        System.out.println(result);
    }

    @Test
    public void testMySQL() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("MySQL", "\"mydatabase\"", null, "\"mytable\"", setting);
        System.out.println(result);
    }

    @Test
    public void testOracle() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("Oracle with SID", null, "\"myschema\"", "\"mytable\"", setting);
        System.out.println(result);
    }

    @Test
    public void testSQLServer() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("Microsoft SQL Server", "\"mydatabase\"", "\"myschema\"", "\"mytable\"",
                setting);
        System.out.println(result);
    }

    @Test
    public void testAS400() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("AS400", "\"mydatabase\"", "\"myschema\"", "\"mytable\"", setting);
        System.out.println(result);
    }

    @Test
    public void testHive() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("Hive", "\"mydatabase\"", "\"myschema\"", "\"mytable\"", setting);
        System.out.println(result);
    }

    @Test
    public void testNetezza() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("Netezza", "\"mydatabase\"", "\"myschema\"", "\"mytable\"", setting);
        System.out.println(result);
    }

    @Test
    public void testGenericJDBCWithSQLServerDriverClass() {
        AllSetting setting = new AllSetting();
        setting.setDriverClass("net.sourceforge.jtds.jdbc.Driver");
        setting.setSchema(createTestSchema());
        String result = QueryUtils.generateNewQuery("General JDBC", null, null, "context.mytable", setting);
        System.out.println(result);
    }

    @Test
    public void testCommonQueryGeneratorWithEmptySchema() {
        AllSetting setting = new AllSetting();
        setting.setSchema(createEmptySchema());
        // TODO now we only expect no exception, maybe should throw exception?
        QueryUtils.generateNewQuery("General JDBC", null, null, "\"mytable\"", setting);
    }

    private Schema createEmptySchema() {
        return SchemaBuilder.builder().record("TEST").fields().endRecord();
    }

    private Schema createTestSchema() {
        FieldAssembler<Schema> builder = SchemaBuilder.builder().record("TEST").fields();

        Schema schema = AvroUtils._int();
        schema = wrap(schema);
        builder = builder.name("ID").prop(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME, "ID1")
                .prop(SchemaConstants.TALEND_COLUMN_IS_KEY, "true").type(schema).noDefault();

        schema = AvroUtils._string();
        schema = wrap(schema);
        builder = builder.name("NAME").prop(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME, "NAME1").type(schema).noDefault();

        return builder.endRecord();
    }

    private Schema wrap(Schema schema) {
        return SchemaBuilder.builder().nullable().type(schema);
    }
}
