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
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.component.runtime.WriterWithFeedback;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.common.avro.JDBCAvroRegistry;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.avro.JDBCSPIndexedRecordCreator;
import org.talend.components.jdbc.module.SPParameterTable;
import org.talend.components.jdbc.runtime.JDBCSPSink;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.components.jdbc.runtime.type.JDBCMapping;
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

    private JDBCSPIndexedRecordCreator indexedRecordCreator;

    public void write(Object datum) throws IOException {
        result.totalCount++;

        successfulWrites.clear();
        rejectedWrites.clear();

        IndexedRecord inputRecord = this.getGenericIndexedRecordConverter(datum).convertToAvro(datum);

        Schema componentSchema = CommonUtils.getMainSchemaFromInputConnector((ComponentProperties) sink.properties);
        Schema inputSchema = inputRecord.getSchema();
        Schema outputSchema = CommonUtils.getOutputSchema((ComponentProperties) sink.properties);

        try {

            if (setting.isFunction()) {
                String columnName = setting.getReturnResultIn();
                Field outField = CommonUtils.getField(componentSchema, columnName);
                cs.registerOutParameter(1, JDBCMapping.getSQLTypeFromAvroType(outField));
            }

            List<String> columns = setting.getSchemaColumns4SPParameters();
            List<String> pts = setting.getParameterTypes();
            if (pts != null) {
                int i = setting.isFunction() ? 2 : 1;
                int j = -1;
                for (String each : pts) {
                    j++;
                    String columnName = columns.get(j);

                    SPParameterTable.ParameterType pt = SPParameterTable.ParameterType.valueOf(each);

                    if (SPParameterTable.ParameterType.RECORDSET == pt) {
                        continue;
                    }

                    if (SPParameterTable.ParameterType.OUT == pt || SPParameterTable.ParameterType.INOUT == pt) {
                        Schema.Field outField = CommonUtils.getField(componentSchema, columnName);
                        cs.registerOutParameter(i, JDBCMapping.getSQLTypeFromAvroType(outField));
                    }

                    if (SPParameterTable.ParameterType.IN == pt || SPParameterTable.ParameterType.INOUT == pt) {
                        Schema.Field inField = CommonUtils.getField(componentSchema, columnName);
                        Schema.Field inFieldInInput = CommonUtils.getField(inputSchema, columnName);
                        JDBCMapping.setValue(i, cs, inField, inputRecord.get(inFieldInInput.pos()));
                    }

                    i++;
                }
            }

            cs.execute();

            if (indexedRecordCreator == null) {
                indexedRecordCreator = new JDBCSPIndexedRecordCreator();
                indexedRecordCreator.init(componentSchema, outputSchema, setting);
            }

            IndexedRecord outputRecord = indexedRecordCreator.createOutputIndexedRecord(cs.getResultSet(), inputRecord);

            successfulWrites.add(outputRecord);
        } catch (Exception e) {
            throw new ComponentException(e);
        }

    }

    @Override
    public Result close() throws IOException {
        closeStatementQuietly(cs);

        closeAtLast();

        constructResult();

        return result;
    }

    private void closeAtLast() {
        if (useExistedConnection) {
            return;
        }

        try {
            if (conn != null) {
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

    // the converter convert all the data type to indexed record, in this class, it factly only convert the indexed record to
    // indexed record, not sure it's more than write like this :
    // (IndexedRecord)object
    private IndexedRecordConverter<Object, ? extends IndexedRecord> genericIndexedRecordConverter;

    @SuppressWarnings("unchecked")
    private IndexedRecordConverter<Object, ? extends IndexedRecord> getGenericIndexedRecordConverter(Object datum) {
        if (null == genericIndexedRecordConverter) {
            genericIndexedRecordConverter = (IndexedRecordConverter<Object, ? extends IndexedRecord>) JDBCAvroRegistry.get()
                    .createIndexedRecordConverter(datum.getClass());
        }
        return genericIndexedRecordConverter;
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
