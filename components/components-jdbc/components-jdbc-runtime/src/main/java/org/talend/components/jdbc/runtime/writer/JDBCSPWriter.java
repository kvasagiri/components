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
package org.talend.components.jdbc.runtime.writer;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.component.runtime.WriterWithFeedback;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.common.avro.JDBCAvroRegistry;
import org.talend.components.jdbc.module.SPParameterTable;
import org.talend.components.jdbc.runtime.JDBCSPSink;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.daikon.avro.converter.IndexedRecordConverter;

/**
 * the JDBC writer for JDBC SP
 *
 */
public class JDBCSPWriter implements WriterWithFeedback<Result, IndexedRecord, IndexedRecord> {

    private transient static final Logger LOG = LoggerFactory.getLogger(JDBCSPWriter.class);

    private WriteOperation<Result> writeOperation;

    private Connection conn;

    private JDBCSPSink sink;

    private AllSetting setting;

    private RuntimeContainer runtime;

    private Result result;

    private boolean useExistedConnection;

    private Statement statement;

    private CallableStatement cs;

    private final List<IndexedRecord> successfulWrites = new ArrayList<>();

    private final List<IndexedRecord> rejectedWrites = new ArrayList<>();

    public JDBCSPWriter(WriteOperation<Result> writeOperation, RuntimeContainer runtime) {
        this.writeOperation = writeOperation;
        this.runtime = runtime;
        sink = (JDBCSPSink) writeOperation.getSink();
        setting = sink.properties.getRuntimeSetting();

        useExistedConnection = setting.getReferencedComponentId() != null;

        result = new Result();
    }

    public void open(String uId) throws IOException {
        try {
            conn = sink.getConnection(runtime);
            cs = conn.prepareCall(sink.getSPStatement(setting));
        } catch (SQLException | ClassNotFoundException e) {
            throw new ComponentException(e);
        }
    }

    public void write(Object datum) throws IOException {
        result.totalCount++;

        successfulWrites.clear();
        rejectedWrites.clear();

        IndexedRecord input = this.getFactory(datum).convertToAvro(datum);
        Schema inputSchema = input.getSchema();

        try {
            Schema outputSchema = setting.getSchema();
            // TODO correct the type

            if (setting.isFunction()) {
                cs.registerOutParameter(1, java.sql.Types.VARCHAR);
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

                    if (SPParameterTable.ParameterType.OUT == pt || SPParameterTable.ParameterType.INOUT == pt) {
                        cs.registerOutParameter(i, java.sql.Types.VARCHAR);
                    }

                    if (SPParameterTable.ParameterType.IN == pt || SPParameterTable.ParameterType.INOUT == pt) {
                        Schema.Field inputField = getField(inputSchema, columnName);
                        cs.setString(i, (String) input.get(inputField.pos()));
                    }

                    i++;
                }
            }

            cs.execute();

            IndexedRecord result = new GenericData.Record(outputSchema);

            if (setting.isFunction()) {
                Schema.Field field = getField(outputSchema, setting.getReturnResultIn());
                result.put(field.pos(), cs.getString(1));
            }

            if (pts != null) {
                int i = setting.isFunction() ? 2 : 1;
                int j = -1;
                for (SPParameterTable.ParameterType pt : pts) {
                    j++;
                    String columnName = columns.get(j);

                    if (SPParameterTable.ParameterType.RECORDSET == pt) {
                        Schema.Field outputField = getField(outputSchema, columnName);
                        result.put(outputField.pos(), cs.getResultSet());
                        continue;
                    }

                    if (SPParameterTable.ParameterType.OUT == pt || SPParameterTable.ParameterType.INOUT == pt) {
                        Schema.Field outputField = getField(outputSchema, columnName);
                        result.put(outputField.pos(), cs.getString(i));
                    }

                    if (SPParameterTable.ParameterType.IN == pt || SPParameterTable.ParameterType.INOUT == pt) {
                        Schema.Field inputField = getField(inputSchema, columnName);
                        Schema.Field outputField = getField(outputSchema, columnName);
                        result.put(outputField.pos(), (String) input.get(inputField.pos()));
                    }

                    i++;
                }
            }

            successfulWrites.add(result);
        } catch (Exception e) {
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
    public Result close() throws IOException {
        closeStatementQuietly(statement);
        statement = null;

        commitAndCloseAtLast();

        constructResult();

        return result;
    }

    private void commitAndCloseAtLast() {
        if (useExistedConnection) {
            return;
        }

        try {
            if (conn != null) {
                // need to call the commit before close for some database when do some read action like reading the resultset
                conn.commit();

                conn.close();
                conn = null;
            }
        } catch (SQLException e) {
            throw new ComponentException(e);
        }
    }

    @Override
    public WriteOperation<Result> getWriteOperation() {
        return writeOperation;
    }

    private IndexedRecordConverter<Object, ? extends IndexedRecord> factory;

    @SuppressWarnings("unchecked")
    private IndexedRecordConverter<Object, ? extends IndexedRecord> getFactory(Object datum) {
        if (null == factory) {
            factory = (IndexedRecordConverter<Object, ? extends IndexedRecord>) JDBCAvroRegistry.get()
                    .createIndexedRecordConverter(datum.getClass());
        }
        return factory;
    }

    private void closeStatementQuietly(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                // close quietly
            }
        }
    }

    private void constructResult() {
    }

    @Override
    public List<IndexedRecord> getSuccessfulWrites() {
        return Collections.unmodifiableList(successfulWrites);
    }

    @Override
    public List<IndexedRecord> getRejectedWrites() {
        return Collections.unmodifiableList(rejectedWrites);
    }

}
