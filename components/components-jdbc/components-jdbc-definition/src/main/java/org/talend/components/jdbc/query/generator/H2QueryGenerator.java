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

import org.talend.components.jdbc.query.AbstractQueryGenerator;
import org.talend.components.jdbc.query.EDatabaseTypeName;
import org.talend.components.jdbc.query.SQLTextUtils;

public class H2QueryGenerator extends AbstractQueryGenerator {

    public H2QueryGenerator(EDatabaseTypeName dbType) {
        super(dbType);
    }

    @Override
    public String generateQueryDelegate() {
        String tableName = SQLTextUtils.addSQLTextFenceByDbType(tableDisplayed, dbType, true);
        tableName = SQLTextUtils.removeTextFenceForJavaLiteral(tableName);

        StringBuffer sql = new StringBuffer(100);
        sql.append(JAVA_TEXT_FENCE);
        sql.append(SQL_SELECT);
        sql.append(SPACE);
        // columns
        sql.append(generateColumnFields(tableName));
        //
        sql.append(ENTER);
        sql.append(SPACE);
        sql.append(SQL_FROM);
        sql.append(SPACE);
        sql.append(tableName);

        sql.append(JAVA_TEXT_FENCE);

        return processResultSQL(sql.toString());
    }

}
