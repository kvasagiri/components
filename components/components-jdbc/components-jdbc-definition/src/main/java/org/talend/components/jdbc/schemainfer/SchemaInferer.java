package org.talend.components.jdbc.schemainfer;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.SchemaBuilder;
import org.codehaus.jackson.JsonNode;
import org.talend.components.common.avro.JDBCTableMetadata;
import org.talend.components.common.config.jdbc.Dbms;
import org.talend.daikon.avro.AvroUtils;
import org.talend.daikon.avro.SchemaConstants;

public class SchemaInferer {

    public static Schema infer(ResultSetMetaData metadata, Dbms mapping) throws SQLException {
        List<Field> fields = new ArrayList<>();

        int count = metadata.getColumnCount();
        for (int i = 1; i <= count; i++) {
            int size = metadata.getPrecision(i);
            int scale = metadata.getScale(i);
            boolean nullable = ResultSetMetaData.columnNullable == metadata.isNullable(i);

            int dbtype = metadata.getColumnType(i);
            String fieldName = metadata.getColumnLabel(i);
            String dbColumnName = metadata.getColumnName(i);

            // not necessary for the result schema from the query statement
            boolean isKey = false;

            Field field = sqlType2Avro(size, scale, dbtype, nullable, fieldName, dbColumnName, null, isKey, mapping);

            fields.add(field);
        }

        return Schema.createRecord("DYNAMIC", null, null, false, fields);
    }

    public static Schema infer(JDBCTableMetadata tableMetadata, Dbms mapping) throws SQLException {
        DatabaseMetaData databaseMetdata = tableMetadata.getDatabaseMetaData();

        Set<String> keys = getPrimaryKeys(databaseMetdata, tableMetadata.getCatalog(), tableMetadata.getDbSchema(),
                tableMetadata.getTablename());

        try (ResultSet metadata = databaseMetdata.getColumns(tableMetadata.getCatalog(), tableMetadata.getDbSchema(),
                tableMetadata.getTablename(), null)) {
            if (!metadata.next()) {
                return null;
            }

            List<Field> fields = new ArrayList<>();
            String tablename = metadata.getString("TABLE_NAME");

            do {
                int size = metadata.getInt("COLUMN_SIZE");
                int scale = metadata.getInt("DECIMAL_DIGITS");
                int dbtype = metadata.getInt("DATA_TYPE");
                boolean nullable = DatabaseMetaData.columnNullable == metadata.getInt("NULLABLE");

                String columnName = metadata.getString("COLUMN_NAME");
                boolean isKey = keys.contains(columnName);

                String defaultValue = metadata.getString("COLUMN_DEF");

                Field field = sqlType2Avro(size, scale, dbtype, nullable, columnName, columnName, defaultValue, isKey, mapping);

                fields.add(field);
            } while (metadata.next());

            return Schema.createRecord(tablename, null, null, false, fields);
        }
    }

    private static Set<String> getPrimaryKeys(DatabaseMetaData databaseMetdata, String catalogName, String schemaName,
            String tableName) throws SQLException {
        Set<String> result = new HashSet<>();

        try (ResultSet resultSet = databaseMetdata.getPrimaryKeys(catalogName, schemaName, tableName)) {
            if (resultSet != null) {
                while (resultSet.next()) {
                    result.add(resultSet.getString("COLUMN_NAME"));
                }
            }
        }

        return result;
    }

    private static Field sqlType2Avro(int size, int scale, int dbtype, boolean nullable, String name, String dbColumnName,
            Object defaultValue, boolean isKey, Dbms mapping) {
        Field field = null;
        Schema schema = null;

        switch (dbtype) {
        case java.sql.Types.VARCHAR:
            schema = AvroUtils._string();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_DB_LENGTH, size);
            break;
        case java.sql.Types.INTEGER:
            schema = AvroUtils._int();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PRECISION, size);
            break;
        case java.sql.Types.DECIMAL:
            schema = AvroUtils._decimal();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PRECISION, size);
            field.addProp(SchemaConstants.TALEND_COLUMN_SCALE, scale);
            break;
        case java.sql.Types.BIGINT:
            schema = AvroUtils._long();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PRECISION, size);
            break;
        case java.sql.Types.NUMERIC:
            schema = AvroUtils._decimal();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PRECISION, size);
            field.addProp(SchemaConstants.TALEND_COLUMN_SCALE, scale);
            break;
        case java.sql.Types.TINYINT:
            schema = AvroUtils._byte();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PRECISION, size);
            break;
        case java.sql.Types.DOUBLE:
            schema = AvroUtils._double();
            field = wrap(nullable, schema, name);
            break;
        case java.sql.Types.FLOAT:
            schema = AvroUtils._float();
            field = wrap(nullable, schema, name);
            break;
        case java.sql.Types.DATE:
            schema = AvroUtils._date();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PATTERN, "yyyy-MM-dd");
            break;
        case java.sql.Types.TIME:
            schema = AvroUtils._date();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PATTERN, "HH:mm:ss");
            break;
        case java.sql.Types.TIMESTAMP:
            schema = AvroUtils._date();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PATTERN, "yyyy-MM-dd HH:mm:ss.SSS");
            break;
        case java.sql.Types.BOOLEAN:
            schema = AvroUtils._boolean();
            field = wrap(nullable, schema, name);
            break;
        case java.sql.Types.REAL:
            schema = AvroUtils._float();
            field = wrap(nullable, schema, name);
            break;
        case java.sql.Types.SMALLINT:
            schema = AvroUtils._short();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_PRECISION, size);
            break;
        case java.sql.Types.LONGVARCHAR:
            schema = AvroUtils._string();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_DB_LENGTH, size);
            break;
        case java.sql.Types.CHAR:
            schema = AvroUtils._string();
            field = wrap(nullable, schema, name);
            field.addProp(SchemaConstants.TALEND_COLUMN_DB_LENGTH, size);
            break;
        default:
            schema = AvroUtils._string();
            field = wrap(nullable, schema, name);
            break;
        }

        field.addProp(SchemaConstants.TALEND_COLUMN_DB_TYPE, dbtype);
        field.addProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME, dbColumnName);

        if (defaultValue != null) {
            field.addProp(SchemaConstants.TALEND_COLUMN_DEFAULT, defaultValue);
        }

        if (isKey) {
            field.addProp(SchemaConstants.TALEND_COLUMN_IS_KEY, "true");
        }

        return field;
    }

    private static Field wrap(boolean nullable, Schema base, String name) {
        Schema schema = nullable ? SchemaBuilder.builder().nullable().type(base) : base;
        return new Field(name, schema, null, (JsonNode) null);
    }
}
