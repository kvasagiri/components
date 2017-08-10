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
package org.talend.components.jdbc.runtime;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.ComponentConstants;
import org.talend.components.jdbc.RuntimeSettingProvider;
import org.talend.components.jdbc.module.SPParameterTable;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.components.jdbc.runtime.setting.JdbcRuntimeSourceOrSinkDefault;
import org.talend.components.jdbc.runtime.type.JDBCMapping;
import org.talend.daikon.NamedThing;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.ValidationResult.Result;
import org.talend.daikon.properties.ValidationResultMutable;

/**
 * JDBC SP runtime execution object
 *
 */
public class JDBCSPSourceOrSink extends JdbcRuntimeSourceOrSinkDefault {

    private static final long serialVersionUID = -1730391293657968628L;

    public RuntimeSettingProvider properties;

    protected AllSetting setting;

    private Connection conn;

    private boolean useExistedConnection;

    @Override
    public ValidationResult initialize(RuntimeContainer runtime, ComponentProperties properties) {
        this.properties = (RuntimeSettingProvider) properties;
        setting = this.properties.getRuntimeSetting();
        useExistedConnection = setting.getReferencedComponentId() != null;
        return ValidationResult.OK;
    }

    @Override
    public ValidationResult validate(RuntimeContainer runtime) {
        ValidationResultMutable vr = new ValidationResultMutable();

        Connection conn = null;
        try {
            conn = connect(runtime);
        } catch (ClassNotFoundException | SQLException e) {
            throw new ComponentException(e);
        }

        Schema componentSchema = CommonUtils.getMainSchemaFromInputConnector((ComponentProperties) properties);

        try {
            CallableStatement cs = conn.prepareCall(getSPStatement(setting));

            try {
                if (setting.isFunction()) {
                    String columnName = setting.getReturnResultIn();
                    Field field = CommonUtils.getField(componentSchema, columnName);
                    cs.registerOutParameter(1, JDBCMapping.getSQLTypeFromAvroType(field));
                }

                List<String> columns = setting.getSchemaColumns4SPParameters();
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
                            Field field = CommonUtils.getField(componentSchema, columnName);
                            cs.registerOutParameter(i, JDBCMapping.getSQLTypeFromAvroType(field));
                        }

                        i++;
                    }
                }

                cs.execute();
            } finally {
                cs.close();
            }

            if (!useExistedConnection) {
                conn.commit();
                conn.close();
            }
        } catch (Exception ex) {
            vr.setStatus(Result.ERROR);
            vr.setMessage(ex.getMessage());
        }
        return vr;
    }

    public String getSPStatement(AllSetting setting) {
        String spName = setting.getSpName();
        boolean isFunction = setting.isFunction();
        List<SPParameterTable.ParameterType> parameterTypes = setting.getParameterTypes();

        StringBuilder statementBuilder = new StringBuilder();
        statementBuilder.append("{");

        if (isFunction) {
            statementBuilder.append("? = ");
        }

        statementBuilder.append("call ").append(spName).append("(");

        if (parameterTypes != null) {
            boolean first = true;
            for (SPParameterTable.ParameterType parameterType : parameterTypes) {
                if (parameterType == SPParameterTable.ParameterType.RECORDSET) {
                    continue;
                }

                if (first) {
                    statementBuilder.append("?");
                    first = false;
                } else {
                    statementBuilder.append(",?");
                }
            }
        }

        statementBuilder.append(")}");
        return statementBuilder.toString();
    }

    /**
     * not necessary for this execution object
     */
    @Override
    public List<NamedThing> getSchemaNames(RuntimeContainer runtime) throws IOException {
        return null;
    }

    /**
     * not necessary for this execution object
     */
    @Override
    public Schema getEndpointSchema(RuntimeContainer runtime, String tableName) throws IOException {
        return null;
    }

    public Connection connect(RuntimeContainer runtime) throws ClassNotFoundException, SQLException {
        // TODO now we use routines.system.TalendDataSource to get the data connection from the ESB runtime, but now we
        // can't
        // refer it by the new framework, so will fix it later

        // TODO routines.system.SharedDBConnectionLog4j, the same with the TODO above

        // using another component's connection
        if (useExistedConnection) {
            if (runtime != null) {
                String refComponentId = setting.getReferencedComponentId();
                Object existedConn = runtime.getComponentData(ComponentConstants.CONNECTION_KEY, refComponentId);
                if (existedConn == null) {
                    throw new RuntimeException("Referenced component: " + refComponentId + " is not connected");
                }
                return (Connection) existedConn;
            }

            return JdbcRuntimeUtils.createConnection(setting);
        } else {
            Connection conn = JdbcRuntimeUtils.createConnection(properties.getRuntimeSetting());

            if (conn.getAutoCommit()) {
                conn.setAutoCommit(false);
            }

            return conn;
        }
    }

    public Connection getConnection(RuntimeContainer runtime) throws ClassNotFoundException, SQLException {
        if (conn == null) {
            conn = connect(runtime);
        }
        return conn;
    }

}
