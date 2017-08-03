// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.jdbc.runtime.setting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.JDBCTemplate;
import org.talend.components.jdbc.module.AdditionalColumnsTable;
import org.talend.daikon.avro.SchemaConstants;

/**
 * SQL build tool
 *
 */
public class JDBCSQLBuilder {

    private JDBCSQLBuilder() {
    }

    public static JDBCSQLBuilder getInstance() {
        return new JDBCSQLBuilder();
    }

    protected String getProtectedChar() {
        return "";
    }

    public String generateSQL4SelectTable(String tablename, Schema schema) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ");
        List<Schema.Field> fields = schema.getFields();
        boolean firstOne = true;
        for (Schema.Field field : fields) {
            if (firstOne) {
                firstOne = false;
            } else {
                sql.append(", ");
            }

            String dbColumnName = field.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME);
            sql.append(tablename).append(".").append(dbColumnName);
        }
        sql.append(" FROM ").append(getProtectedChar()).append(tablename).append(getProtectedChar());

        return sql.toString();
    }

    public String generateSQL4DeleteTable(String tablename) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(getProtectedChar()).append(tablename).append(getProtectedChar());
        return sql.toString();
    }

    public class Column {

        public String columnLabel;

        public String dbColumnName;

        public String sqlStmt = "?";

        public boolean isKey;

        public boolean updateKey;

        public boolean deletionKey;

        public boolean updatable = true;

        public boolean insertable = true;

        public boolean addCol;

        public List<Column> replacements;

        void replace(Column replacement) {
            if (replacements == null) {
                replacements = new ArrayList<Column>();
            }

            replacements.add(replacement);
        }

        public boolean isReplaced() {
            return this.replacements != null && !this.replacements.isEmpty();
        }

    }

    public String generateSQL4Insert(String tablename, List<Column> columnList) {
        List<String> dbColumnNames = new ArrayList<>();
        List<String> expressions = new ArrayList<>();

        for (Column column : columnList) {
            if (column.replacements != null && !column.replacements.isEmpty()) {
                for (Column replacement : column.replacements) {
                    if (replacement.insertable) {
                        dbColumnNames.add(replacement.dbColumnName);
                        expressions.add(replacement.sqlStmt);
                    }
                }
            } else {
                if (column.insertable) {
                    dbColumnNames.add(column.dbColumnName);
                    expressions.add(column.sqlStmt);
                }
            }
        }

        return generateSQL4Insert(tablename, dbColumnNames, expressions);
    }

    public List<Column> createColumnList(AllSetting setting, Schema schema) {
        Map<String, Column> columnMap = new HashMap<>();
        List<Column> columnList = new ArrayList<>();

        List<Field> fields = schema.getFields();

        for (Field field : fields) {
            Column column = new Column();
            column.columnLabel = field.name();
            column.dbColumnName = field.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME);

            boolean isKey = Boolean.valueOf(field.getProp(SchemaConstants.TALEND_COLUMN_IS_KEY));
            if (isKey) {
                column.updateKey = true;
                column.deletionKey = true;
                column.updatable = false;
            } else {
                column.updateKey = false;
                column.deletionKey = false;
                column.updatable = true;
            }

            columnMap.put(field.name(), column);
            columnList.add(column);
        }

        boolean enableFieldOptions = setting.getEnableFieldOptions();
        if (enableFieldOptions) {
            List<String> schemaColumns4FieldOption = setting.getSchemaColumns4FieldOption();
            List<Boolean> updateKey = setting.getUpdateKey4FieldOption();
            List<Boolean> deletionKey = setting.getDeletionKey4FieldOption();
            List<Boolean> updatable = setting.getUpdatable4FieldOption();
            List<Boolean> insertable = setting.getInsertable4FieldOption();

            int i = 0;
            for (String columnName : schemaColumns4FieldOption) {
                Column column = columnMap.get(columnName);
                column.updateKey = updateKey.get(i);
                column.deletionKey = deletionKey.get(i);
                column.updatable = updatable.get(i);
                column.insertable = insertable.get(i);

                i++;
            }
        }

        List<String> newDBColumnNames = setting.getNewDBColumnNames4AdditionalParameters();
        List<String> sqlExpressions = setting.getSqlExpressions4AdditionalParameters();
        List<AdditionalColumnsTable.Position> positions = setting.getPositions4AdditionalParameters();
        List<String> referenceColumns = setting.getReferenceColumns4AdditionalParameters();

        int i = 0;
        for (String referenceColumn : referenceColumns) {
            int j = 0;
            Column currentColumn = null;
            for (Column column : columnList) {
                if (column.columnLabel.equals(referenceColumn)) {
                    currentColumn = column;
                    break;
                }
                j++;
            }

            String newDBColumnName = newDBColumnNames.get(i);
            String sqlExpression = sqlExpressions.get(i);

            AdditionalColumnsTable.Position position = positions.get(i);
            if (position == AdditionalColumnsTable.Position.AFTER) {
                Column newColumn = new Column();
                newColumn.columnLabel = newDBColumnName;
                newColumn.dbColumnName = newDBColumnName;
                newColumn.sqlStmt = sqlExpression;
                newColumn.addCol = true;

                columnList.add(j + 1, newColumn);
            } else if (position == AdditionalColumnsTable.Position.BEFORE) {
                Column newColumn = new Column();
                newColumn.columnLabel = newDBColumnName;
                newColumn.dbColumnName = newDBColumnName;
                newColumn.sqlStmt = sqlExpression;
                newColumn.addCol = true;

                columnList.add(j, newColumn);
            } else if (position == AdditionalColumnsTable.Position.REPLACE) {
                Column replacementColumn = new Column();
                replacementColumn.columnLabel = newDBColumnName;
                replacementColumn.dbColumnName = newDBColumnName;
                replacementColumn.sqlStmt = sqlExpression;

                Column replacedColumn = currentColumn;

                replacementColumn.isKey = replacedColumn.isKey;
                replacementColumn.updateKey = replacedColumn.updateKey;
                replacementColumn.deletionKey = replacedColumn.deletionKey;
                replacementColumn.insertable = replacedColumn.insertable;
                replacementColumn.updatable = replacedColumn.updatable;

                replacedColumn.replace(replacementColumn);
            }

            i++;
        }
        return columnList;
    }

    private String generateSQL4Insert(String tablename, List<String> insertableDBColumns, List<String> expressions) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(getProtectedChar()).append(tablename).append(getProtectedChar()).append(" ");

        sb.append("(");
        boolean firstOne = true;
        for (String dbColumnName : insertableDBColumns) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(",");
            }

            sb.append(dbColumnName);
        }
        sb.append(")");

        sb.append(" VALUES ");

        sb.append("(");

        if (expressions != null && !expressions.isEmpty()) {
            firstOne = true;
            for (String expression : expressions) {
                if (firstOne) {
                    firstOne = false;
                } else {
                    sb.append(",");
                }

                sb.append(expression);
            }
            sb.append(")");

            return sb.toString();
        }

        firstOne = true;
        for (@SuppressWarnings("unused")
        String dbColumnName : insertableDBColumns) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(",");
            }

            sb.append("?");
        }
        sb.append(")");

        return sb.toString();

    }

    public String generateSQL4Insert(String tablename, Schema schema) {
        return generateSQL4Insert(tablename, CommonUtils.getAllSchemaFieldDBNames(schema), null);
    }

    public String generateSQL4Delete(String tablename, Schema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(getProtectedChar()).append(tablename).append(getProtectedChar()).append(" WHERE ");

        List<Schema.Field> keys = JDBCTemplate.getKeyColumns(schema.getFields());
        boolean firstOne = true;
        for (Schema.Field key : keys) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(" AND ");
            }

            String dbColumnName = key.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME);
            sb.append(getProtectedChar()).append(dbColumnName).append(getProtectedChar()).append(" = ").append("?");
        }

        return sb.toString();
    }

    public String generateSQL4Delete(String tablename, List<Column> columnList) {
        List<String> deleteKeys = new ArrayList<>();
        List<String> expressions = new ArrayList<>();

        for (Column column : columnList) {
            if (column.replacements != null && !column.replacements.isEmpty()) {
                for (Column replacement : column.replacements) {
                    if (replacement.deletionKey) {
                        deleteKeys.add(replacement.dbColumnName);
                        expressions.add(replacement.sqlStmt);
                    }
                }
            } else {
                if (column.deletionKey) {
                    deleteKeys.add(column.dbColumnName);
                    expressions.add(column.sqlStmt);
                }
            }
        }

        return generateSQL4Insert(tablename, deleteKeys, expressions);
    }

    public String generateSQL4Delete(String tablename, List<String> deleteKeys, List<String> expressions) {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(getProtectedChar()).append(tablename).append(getProtectedChar()).append(" WHERE ");

        int i = 0;

        boolean firstOne = true;
        for (String dbColumnName : deleteKeys) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(" AND ");
            }

            sb.append(getProtectedChar()).append(dbColumnName).append(getProtectedChar()).append(" = ")
                    .append(expressions.get(i++));
        }

        return sb.toString();
    }

    public String generateSQL4Update(String tablename, Schema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(getProtectedChar()).append(tablename).append(getProtectedChar()).append(" SET ");

        List<Schema.Field> values = JDBCTemplate.getValueColumns(schema.getFields());
        boolean firstOne = true;
        for (Schema.Field value : values) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(",");
            }

            String dbColumnName = value.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME);
            sb.append(getProtectedChar()).append(dbColumnName).append(getProtectedChar()).append(" = ").append("?");
        }

        sb.append(" WHERE ");

        List<Schema.Field> keys = JDBCTemplate.getKeyColumns(schema.getFields());
        firstOne = true;
        for (Schema.Field key : keys) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(" AND ");
            }

            String dbColumnName = key.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME);
            sb.append(getProtectedChar()).append(dbColumnName).append(getProtectedChar()).append(" = ").append("?");
        }

        return sb.toString();
    }

    public String generateSQL4Update(String tablename, List<String> updateValues, List<String> updateKeys,
            List<String> updateExpressions) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(getProtectedChar()).append(tablename).append(getProtectedChar()).append(" SET ");

        int i = 0;

        boolean firstOne = true;
        for (String dbColumnName : updateValues) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(",");
            }

            sb.append(getProtectedChar()).append(dbColumnName).append(getProtectedChar()).append(" = ")
                    .append(updateExpressions.get(i++));
        }

        sb.append(" WHERE ");

        firstOne = true;
        for (String dbColumnName : updateKeys) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(" AND ");
            }

            sb.append(getProtectedChar()).append(dbColumnName).append(getProtectedChar()).append(" = ")
                    .append(updateExpressions.get(i++));
        }

        return sb.toString();
    }

    public String generateSQL4Update(String tablename, List<Column> columnList) {
        List<String> updateValues = new ArrayList<>();
        List<String> updateKeys = new ArrayList<>();
        List<String> updateExpressions = new ArrayList<>();

        for (Column column : columnList) {
            if (column.replacements != null && !column.replacements.isEmpty()) {
                for (Column replacement : column.replacements) {
                    if (replacement.updatable) {
                        updateValues.add(replacement.dbColumnName);
                        updateExpressions.add(replacement.sqlStmt);
                    }

                    if (replacement.updateKey) {
                        updateKeys.add(replacement.dbColumnName);
                        updateExpressions.add(replacement.sqlStmt);
                    }
                }
            } else {
                if (column.updatable) {
                    updateValues.add(column.dbColumnName);
                    updateExpressions.add(column.sqlStmt);
                }

                if (column.updateKey) {
                    updateKeys.add(column.dbColumnName);
                    updateExpressions.add(column.sqlStmt);
                }
            }
        }

        return generateSQL4Update(tablename, updateValues, updateKeys, updateExpressions);
    }

    public String generateQuerySQL4InsertOrUpdate(String tablename, Schema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(1) FROM ").append(getProtectedChar()).append(tablename).append(getProtectedChar())
                .append(" WHERE ");

        List<Schema.Field> keys = JDBCTemplate.getKeyColumns(schema.getFields());
        boolean firstOne = true;
        for (Schema.Field key : keys) {
            if (firstOne) {
                firstOne = false;
            } else {
                sb.append(" AND ");
            }

            String dbColumnName = key.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME);
            sb.append(getProtectedChar()).append(dbColumnName).append(getProtectedChar()).append(" = ").append("?");
        }

        return sb.toString();
    }

}
