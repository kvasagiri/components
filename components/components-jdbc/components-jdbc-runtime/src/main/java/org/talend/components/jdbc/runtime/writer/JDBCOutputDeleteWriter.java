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
import java.sql.SQLException;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.runtime.setting.JDBCSQLBuilder;
import org.talend.components.jdbc.runtime.setting.JDBCSQLBuilder.Column;
import org.talend.components.jdbc.runtime.type.JDBCMapping;

public class JDBCOutputDeleteWriter extends JDBCOutputWriter {

    private transient static final Logger LOG = LoggerFactory.getLogger(JDBCOutputDeleteWriter.class);

    private String sql;

    public JDBCOutputDeleteWriter(WriteOperation<Result> writeOperation, RuntimeContainer runtime) {
        super(writeOperation, runtime);
    }

    @Override
    public void open(String uId) throws IOException {
        super.open(uId);
        try {
            conn = sink.getConnection(runtime);
            sql = JDBCSQLBuilder.getInstance().generateSQL4Delete(setting.getTablename(), columnList);
            statement = conn.prepareStatement(sql);
        } catch (ClassNotFoundException | SQLException e) {
            throw new ComponentException(e);
        }

    }

    @Override
    public void write(Object datum) throws IOException {
        super.write(datum);

        IndexedRecord input = this.getFactory(datum).convertToAvro(datum);
        Schema inputSchema = input.getSchema();

        try {
            int index = 0;
            for (Column column : columnList) {
                if (column.addCol || column.isReplaced()) {
                    continue;
                }

                if (!column.deletionKey) {
                    continue;
                }

                Field field = CommonUtils.getField(inputSchema, column.columnLabel);
                JDBCMapping.setValue(++index, statement, field, input.get(field.pos()));
            }
        } catch (SQLException e) {
            throw new ComponentException(e);
        }

        try {
            deleteCount += execute(input, statement);
        } catch (SQLException e) {
            if (dieOnError) {
                throw new ComponentException(e);
            } else {
                LOG.warn(e.getMessage());
            }

            handleReject(input, e);
        }

        try {
            deleteCount += executeCommit(statement);
        } catch (SQLException e) {
            if (dieOnError) {
                throw new ComponentException(e);
            } else {
                LOG.warn(e.getMessage());
            }
        }
    }

    @Override
    public Result close() throws IOException {
        if (useBatch && batchCount > 0) {
            try {
                batchCount = 0;
                deleteCount += executeBatchAndGetCount(statement);
            } catch (SQLException e) {
                if (dieOnError) {
                    throw new ComponentException(e);
                } else {
                    LOG.warn(e.getMessage());
                }
            }
        }

        closeStatementQuietly(statement);
        statement = null;

        commitAndCloseAtLast();

        constructResult();

        return result;
    }

}
