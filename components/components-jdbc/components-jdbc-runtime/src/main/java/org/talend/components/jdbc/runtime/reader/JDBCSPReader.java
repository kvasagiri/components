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
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.talend.components.api.component.runtime.AbstractBoundedReader;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.jdbc.RuntimeSettingProvider;
import org.talend.components.jdbc.module.SPParameterTable;
import org.talend.components.jdbc.runtime.JDBCSPSource;
import org.talend.components.jdbc.runtime.setting.AllSetting;

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

    @Override
    public IndexedRecord getCurrent() {
        try {
            cs = conn.prepareCall(source.getSPStatement(setting));

            Schema schema = setting.getSchema();
            // TODO correct the type

            if (setting.isFunction()) {
                cs.registerOutParameter(1, java.sql.Types.VARCHAR);
            }

            List<SPParameterTable.ParameterType> pts = setting.getParameterTypes();
            if (pts != null) {
                int index = setting.isFunction() ? 2 : 1;
                for (SPParameterTable.ParameterType pt : pts) {
                    if (SPParameterTable.ParameterType.RECORDSET == pt) {
                        continue;
                    }

                    if (SPParameterTable.ParameterType.OUT == pt) {
                        cs.registerOutParameter(index, java.sql.Types.VARCHAR);
                    }

                    index++;
                }
            }

            cs.execute();

            IndexedRecord result = new GenericData.Record(schema);

            if (setting.isFunction()) {
                Schema.Field field = getField(schema, setting.getReturnResultIn());
                result.put(field.pos(), cs.getString(1));
            }

            List<String> columns = setting.getSchemaColumns();
            if (pts != null) {
                int i = setting.isFunction() ? 2 : 1;
                int j = -1;
                for (SPParameterTable.ParameterType pt : pts) {
                    j++;
                    String columnName = columns.get(j);

                    if (SPParameterTable.ParameterType.RECORDSET == pt) {
                        Schema.Field field = getField(schema, columnName);
                        result.put(field.pos(), cs.getResultSet());
                        continue;
                    }

                    if (SPParameterTable.ParameterType.OUT == pt) {
                        Schema.Field field = getField(schema, columnName);
                        result.put(field.pos(), cs.getString(i));
                    }

                    i++;
                }
            }

            return result;
        } catch (SQLException e) {
            throw new ComponentException(e);
        }

    }

    public Schema.Field getField(Schema schema, String fieldName) {
        if (schema == null) {
            return null;
        }

        for (Schema.Field outField : schema.getFields()) {
            if (outField.name().equals(fieldName)) {
                return outField;
            }
        }

        return null;
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
