
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
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.SourceOrSink;

import org.apache.avro.generic.IndexedRecord;

import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.common.avro.JDBCAvroRegistry;
import org.talend.components.common.avro.JDBCResultSetIndexedRecordConverter;
import org.talend.components.common.avro.JDBCTableMetadata;
import org.talend.components.common.dataset.DatasetProperties;
import org.talend.components.common.datastore.DatastoreProperties;
import org.talend.components.jdbc.ComponentConstants;
import org.talend.components.jdbc.JdbcComponentErrorsCode;
import org.talend.components.jdbc.RuntimeSettingProvider;
import org.talend.components.jdbc.avro.JDBCAvroRegistryString;
import org.talend.components.jdbc.avro.ResultSetStringRecordConverter;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.components.jdbc.runtime.setting.JdbcRuntimeSourceOrSinkDefault;
import org.talend.daikon.NamedThing;
import org.talend.daikon.SimpleNamedThing;
import org.talend.daikon.avro.AvroRegistry;
import org.talend.daikon.avro.converter.IndexedRecordConverter;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.daikon.exception.error.CommonErrorCodes;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.ValidationResult.Result;
import org.talend.daikon.properties.ValidationResultMutable;

/**
 * common JDBC runtime execution object
 *
 */
public class JDBCSourceOrSink extends JdbcRuntimeSourceOrSinkDefault {

    private static final Logger LOGGER = LoggerFactory.getLogger(JDBCSourceOrSink.class);

    private static final long serialVersionUID = -1730391293657968628L;

    public RuntimeSettingProvider properties;

    protected AllSetting setting;

    private Connection conn;

    private transient AvroRegistry avroRegistry;

    private transient IndexedRecordConverter<ResultSet, IndexedRecord> converter;

    @Override
    public ValidationResult initialize(RuntimeContainer runtime, ComponentProperties properties) {
        this.properties = (RuntimeSettingProvider) properties;
        setting = this.properties.getRuntimeSetting();

        // TODO use another registry which use the db mapping files
        avroRegistry = JDBCAvroRegistry.get();
        converter = new JDBCResultSetIndexedRecordConverter();
        ((JDBCResultSetIndexedRecordConverter) converter).setInfluencer(setting);

        return ValidationResult.OK;
    }

    // TODO adjust it, now only as a temp workaround for reuse current class in the datastore runtime
    public ValidationResult initialize(RuntimeContainer runtime, DatastoreProperties properties) {
        this.properties = (RuntimeSettingProvider) properties;
        setting = this.properties.getRuntimeSetting();

        avroRegistry = JDBCAvroRegistryString.get();
        converter = new ResultSetStringRecordConverter();

        return ValidationResult.OK;
    }

    // TODO adjust it, now only as a temp workaround for reuse current class in the dataset runtime
    @SuppressWarnings("rawtypes")
    public ValidationResult initialize(RuntimeContainer runtime, DatasetProperties properties) {
        this.properties = (RuntimeSettingProvider) properties;
        setting = this.properties.getRuntimeSetting();

        avroRegistry = JDBCAvroRegistryString.get();
        converter = new ResultSetStringRecordConverter();

        return ValidationResult.OK;
    }

    @Override
    public ValidationResult validate(RuntimeContainer runtime) {
        ValidationResultMutable vr = new ValidationResultMutable();
        try {
            conn = connect(runtime);
        } catch (Exception ex) {
            vr.setStatus(Result.ERROR);
            vr.setMessage(ex.getMessage());
        }
        return vr;
    }

    @Override
    public List<NamedThing> getSchemaNames(RuntimeContainer runtime) throws IOException {
        List<NamedThing> result = new ArrayList<>();
        try (Connection conn = connect(runtime);
                ResultSet resultset = conn.getMetaData().getTables(null, null, null, new String[] { "TABLE" })) {
            while (resultset.next()) {
                String tablename = resultset.getString("TABLE_NAME");
                result.add(new SimpleNamedThing(tablename, tablename));
            }
        } catch (Exception e) {
            throw new ComponentException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e,
                    ExceptionContext.withBuilder().put("message", e.getMessage()).build());
        }
        return result;
    }

    @Override
    public Schema getEndpointSchema(RuntimeContainer runtime, String tableName) throws IOException {
        try (Connection conn = connect(runtime)) {
            JDBCTableMetadata tableMetadata = new JDBCTableMetadata();
            tableMetadata.setDatabaseMetaData(conn.getMetaData()).setTablename(tableName);
            return avroRegistry.inferSchema(tableMetadata);
        } catch (Exception e) {
            throw new ComponentException(CommonErrorCodes.UNEXPECTED_EXCEPTION, e,
                    ExceptionContext.withBuilder().put("message", e.getMessage()).build());
        }
    }

    public Schema getSchemaFromQuery(RuntimeContainer runtime, String query) {
        try (Connection conn = connect(runtime);
                Statement statement = conn.createStatement();
                ResultSet resultset = statement.executeQuery(query)) {
            ResultSetMetaData metadata = resultset.getMetaData();
            return avroRegistry.inferSchema(metadata);
        } catch (SQLSyntaxErrorException sqlSyntaxException) {
            throw new ComponentException(JdbcComponentErrorsCode.SQL_SYNTAX_ERROR, sqlSyntaxException);
        } catch (SQLException e) {
            throw new ComponentException(JdbcComponentErrorsCode.SQL_ERROR, e);
        } catch (ClassNotFoundException e) {
            throw new ComponentException(JdbcComponentErrorsCode.DRIVER_NOT_PRESENT_ERROR, e);
        }
    }

    public Connection connect(RuntimeContainer runtime) throws ClassNotFoundException, SQLException {
        // TODO now we use routines.system.TalendDataSource to get the data connection from the ESB runtime, but now we
        // can't
        // refer it by the new framework, so will fix it later

        // TODO routines.system.SharedDBConnectionLog4j, the same with the TODO above

        AllSetting setting = properties.getRuntimeSetting();

        // connection component
        Connection conn = JdbcRuntimeUtils.createConnection(setting);
        try {
            conn.setReadOnly(setting.isReadOnly());
        } catch (SQLFeatureNotSupportedException e) {
            LOGGER.debug("JDBC driver '{}' does not support read only mode.", setting.getDriverClass(), e);
        }

        if (setting.getUseAutoCommit()) {
            conn.setAutoCommit(setting.getAutocommit());
        }

        if (runtime != null) {
            // if you check the api, you will find the parameter is set to the wrong location, but it's right now, as we need to
            // keep the connection component can work with some old javajet components
            runtime.setComponentData(ComponentConstants.CONNECTION_KEY, runtime.getCurrentComponentId(), conn);
            runtime.setComponentData(ComponentConstants.URL_KEY, runtime.getCurrentComponentId(), setting.getJdbcUrl());
            runtime.setComponentData(ComponentConstants.USERNAME_KEY, runtime.getCurrentComponentId(), setting.getUsername());
        }

        return conn;
    }

    public Connection getConnection(RuntimeContainer runtime) throws ClassNotFoundException, SQLException {
        if (conn == null) {
            conn = connect(runtime);
        }
        return conn;
    }

    public AvroRegistry getAvroRegistry() {
        return avroRegistry;
    }

    public IndexedRecordConverter<ResultSet, IndexedRecord> getConverter() {
        return converter;
    }

}
