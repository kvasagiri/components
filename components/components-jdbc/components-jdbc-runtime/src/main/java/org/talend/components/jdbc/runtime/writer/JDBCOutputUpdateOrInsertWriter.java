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
import java.sql.PreparedStatement;
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

public class JDBCOutputUpdateOrInsertWriter extends JDBCOutputWriter {

    private transient static final Logger LOG = LoggerFactory.getLogger(JDBCOutputUpdateOrInsertWriter.class);

    private String sqlInsert;

    private String sqlUpdate;

    private PreparedStatement statementInsert;

    private PreparedStatement statementUpdate;

    public JDBCOutputUpdateOrInsertWriter(WriteOperation<Result> writeOperation, RuntimeContainer runtime) {
        super(writeOperation, runtime);
    }

    @Override
    public void open(String uId) throws IOException {
        super.open(uId);
        try {
            conn = sink.getConnection(runtime);

            sqlInsert = JDBCSQLBuilder.getInstance().generateSQL4Insert(setting.getTablename(), columnList);
            statementInsert = conn.prepareStatement(sqlInsert);

            sqlUpdate = JDBCSQLBuilder.getInstance().generateSQL4Update(setting.getTablename(), columnList);
            statementUpdate = conn.prepareStatement(sqlUpdate);

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

                if (column.updatable) {
                    Field field = CommonUtils.getField(inputSchema, column.columnLabel);
                    JDBCMapping.setValue(++index, statementUpdate, field, input.get(field.pos()));
                }
            }

            for (Column column : columnList) {
                if (column.addCol || column.isReplaced()) {
                    continue;
                }

                if (column.updateKey) {
                    Field field = CommonUtils.getField(inputSchema, column.columnLabel);
                    JDBCMapping.setValue(++index, statementUpdate, field, input.get(field.pos()));
                }
            }
        } catch (SQLException e) {
            throw new ComponentException(e);
        }

        try {
            int count = statementUpdate.executeUpdate();

            updateCount += count;

            boolean noDataUpdate = (count == 0);

            if (noDataUpdate) {
                int index = 0;
                for (Column column : columnList) {
                    if (column.addCol || column.isReplaced()) {
                        continue;
                    }

                    if (column.insertable) {
                        Field field = CommonUtils.getField(inputSchema, column.columnLabel);
                        JDBCMapping.setValue(++index, statementInsert, field, input.get(field.pos()));
                    }
                }

                insertCount += execute(input, statementInsert);
            } else {
                handleSuccess(input);
            }
        } catch (SQLException e) {
            if (dieOnError) {
                throw new ComponentException(e);
            } else {
                LOG.warn(e.getMessage());
            }

            handleReject(input, e);
        }

        try {
            executeCommit(null);
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
        closeStatementQuietly(statementUpdate);
        closeStatementQuietly(statementInsert);

        statementUpdate = null;
        statementInsert = null;

        commitAndCloseAtLast();

        constructResult();

        return result;
    }

}
