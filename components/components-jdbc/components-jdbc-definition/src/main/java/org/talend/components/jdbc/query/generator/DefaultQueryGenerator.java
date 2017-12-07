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

public class DefaultQueryGenerator extends AbstractQueryGenerator {

    public DefaultQueryGenerator(EDatabaseTypeName dbType) {
        super(dbType);
    }

    @Override
    public String generateQueryDelegate() {
        Schema schema = setting.getSchema();
        List<Field> fields = schema.getFields();
        if (fields != null && !fields.isEmpty()) {
            String tableName = getDBTableName();
            final String tableNameWithDBAndSchema = getTableNameWithDBAndSchema(null, null, tableName);
            String columnField = null;
            if (dbType == EDatabaseTypeName.HIVE) {
                columnField = generateColumnFields(tableName);
            } else {
                generateColumnFields(tableNameWithDBAndSchema);
            }

            StringBuffer sql = new StringBuffer(100);
            sql.append(JAVA_TEXT_FENCE);
            sql.append(SQL_SELECT);
            sql.append(SPACE);

            sql.append(columnField);

            sql.append(ENTER);
            sql.append(SQL_FROM);
            sql.append(SPACE);
            sql.append(tableNameWithDBAndSchema);

            sql.append(JAVA_TEXT_FENCE);

            return processResultSQL(sql.toString());
        }

        return EMPTY;
    }
}
