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
package org.talend.components.jdbc.query.generator;

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.talend.components.jdbc.query.AbstractQueryGenerator;
import org.talend.components.jdbc.query.EDatabaseTypeName;
import org.talend.components.jdbc.query.IQueryGenerator;

public class OracleQueryGenerator extends AbstractQueryGenerator {

    public OracleQueryGenerator(EDatabaseTypeName dbType) {
        super(dbType);
    }

    @Override
    public String generateQueryDelegate() {
        Schema schema = setting.getSchema();
        if (schema == null) {
            return EMPTY;
        }

        List<Field> fields = schema.getFields();
        if (fields == null || fields.isEmpty()) {
            return EMPTY;
        }

        String tableName = null;
        String schemaName = null;
        tableName = getDBTableName();
        // tableName = realTableName;
        final String tableNameWithDBAndSchema = getTableNameWithDBAndSchema("", schemaName, tableName);

        StringBuffer sql = new StringBuffer(100);
        sql.append(IQueryGenerator.JAVA_TEXT_FENCE);
        sql.append(SQL_SELECT);
        sql.append(SPACE);
        // columns
        sql.append(generateColumnFields(tableNameWithDBAndSchema));
        //
        sql.append(ENTER);
        sql.append(SQL_FROM);
        sql.append(SPACE);
        sql.append(tableNameWithDBAndSchema);

        sql.append(IQueryGenerator.JAVA_TEXT_FENCE);

        return processResultSQL(sql.toString());
    }

}
