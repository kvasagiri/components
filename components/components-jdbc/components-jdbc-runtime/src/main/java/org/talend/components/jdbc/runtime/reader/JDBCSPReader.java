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
package org.talend.components.jdbc.runtime.reader;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;
import org.talend.components.api.component.runtime.AbstractBoundedReader;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.RuntimeSettingProvider;
import org.talend.components.jdbc.avro.JDBCSPIndexedRecordCreator;
import org.talend.components.jdbc.module.SPParameterTable;
import org.talend.components.jdbc.runtime.JDBCSPSource;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.components.jdbc.runtime.type.JDBCMapping;

/**
 * JDBC reader for JDBC SP
 *
 */
public class JDBCSPReader extends AbstractBoundedReader<IndexedRecord> {

    protected RuntimeSettingProvider properties;

    protected RuntimeContainer container;

    protected Connection conn;

    private JDBCSPSource source;

    private CallableStatement cs;

    private Statement statement;

    private Result result;

    private boolean useExistedConnection;

    private AllSetting setting;

    public JDBCSPReader(RuntimeContainer container, JDBCSPSource source, RuntimeSettingProvider props) {
        super(source);
        this.container = container;
        this.properties = props;
        this.source = (JDBCSPSource) getCurrentSource();

        this.setting = props.getRuntimeSetting();
    }

    @Override
    public boolean start() throws IOException {
        result = new Result();

        try {
            conn = source.getConnection(container);
        } catch (ClassNotFoundException | SQLException e) {
            throw new ComponentException(e);
        }

        return true;
    }

    @Override
    public boolean advance() throws IOException {
        return false;// only one row
    }

    private JDBCSPIndexedRecordCreator indexedRecordCreator;
    
    @Override
    public IndexedRecord getCurrent() {
        try {
            cs = conn.prepareCall(source.getSPStatement(setting));

            Schema outputSchema = setting.getSchema();

            if (setting.isFunction()) {
                String columnName = setting.getReturnResultIn();
                Field field = CommonUtils.getField(outputSchema, columnName);
                cs.registerOutParameter(1, JDBCMapping.getSQLTypeFromAvroType(field));
            }

            List<String> columns = setting.getSchemaColumns();
            List<SPParameterTable.ParameterType> pts = setting.getParameterTypes();
            if (pts != null) {
                int i = setting.isFunction() ? 2 : 1;
                int j = -1;
                for (SPParameterTable.ParameterType pt : pts) {
                    j++;
                    String columnName = columns.get(j);

                    if (SPParameterTable.ParameterType.RECORDSET == pt) {
                        continue;
                    }

                    if (SPParameterTable.ParameterType.OUT == pt) {
                        Field field = CommonUtils.getField(outputSchema, columnName);
                        cs.registerOutParameter(i, JDBCMapping.getSQLTypeFromAvroType(field));
                    }

                    i++;
                }
            }

            cs.execute();
            
            if (indexedRecordCreator == null) {
                indexedRecordCreator = new JDBCSPIndexedRecordCreator();
                indexedRecordCreator.init(null, outputSchema, setting);
            }

            IndexedRecord outputRecord = indexedRecordCreator.createOutputIndexedRecord(cs.getResultSet(), null);
            
            return outputRecord;
        } catch (SQLException e) {
            throw new ComponentException(e);
        }

    }

    @Override
    public void close() throws IOException {
        try {
            if (cs != null) {
                cs.close();
                cs = null;
            }

            if (statement != null) {
                statement.close();
                statement = null;
            }

            if (!useExistedConnection && conn != null) {
                conn.commit();
                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            throw new ComponentException(e);
        }
    }

    @Override
    public Map<String, Object> getReturnValues() {
        return result.toMap();
    }

}
