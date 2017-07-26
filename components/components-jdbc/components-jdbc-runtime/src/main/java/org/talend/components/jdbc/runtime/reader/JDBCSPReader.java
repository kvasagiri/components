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
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.jdbc.CommonUtils;
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

            // TODO correct the type
            if (setting.isFunction()) {
                cs.registerOutParameter(1, java.sql.Types.VARCHAR);
            }

            boolean propagateResultSet = false;
            List<SPParameterTable.ParameterType> pts = setting.getParameterTypes();
            if (pts != null) {
                int index = setting.isFunction() ? 2 : 1;
                for (SPParameterTable.ParameterType pt : pts) {
                    if (SPParameterTable.ParameterType.OUT == pt) {
                        cs.registerOutParameter(index, java.sql.Types.VARCHAR);
                    }

                    if (SPParameterTable.ParameterType.RECORDSET == pt) {

                    } else {
                        index++;
                    }
                }
            }

            cs.execute();

            handleSuccess();
        } catch (SQLException e) {
            throw new ComponentException(e);
        }
        return null;
    }

    private IndexedRecord handleSuccess() throws SQLException {
        Schema outSchema = CommonUtils.getOutputSchema((ComponentProperties) properties);
        IndexedRecord output = new GenericData.Record(outSchema);

        List<SPParameterTable.ParameterType> pts = setting.getParameterTypes();
        List<String> columns = setting.getSchemaColumns();
        String returnColumnName = setting.getReturnResultIn();
        if (pts != null) {
            int index = setting.isFunction() ? 2 : 1;
            for (SPParameterTable.ParameterType pt : pts) {
                if (SPParameterTable.ParameterType.OUT == pt) {
                    // TODO put the value to record and correct the type
                    cs.getString(index);
                }

                if (SPParameterTable.ParameterType.RECORDSET == pt) {
                    for (Schema.Field outField : output.getSchema().getFields()) {
                        if (outField.name().equals("TDDO")) {
                            output.put(outField.pos(), cs.getResultSet());
                        }
                    }
                } else {
                    index++;
                }
            }
        }

        return output;
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
