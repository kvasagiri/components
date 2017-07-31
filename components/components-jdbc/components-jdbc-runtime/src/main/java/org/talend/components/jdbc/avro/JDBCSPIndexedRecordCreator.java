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
package org.talend.components.jdbc.avro;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;
import org.talend.components.common.avro.JDBCAvroRegistry;
import org.talend.components.jdbc.module.SPParameterTable;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.daikon.avro.converter.AvroConverter;
import org.talend.daikon.avro.converter.IndexedRecordConverter.UnmodifiableAdapterException;
import org.talend.daikon.exception.TalendRuntimeException;

/**
 * construct the output indexed record from input indexed record and input schema and current component schema and also the output
 * schema, we use this to avoid to do some same work for every input row
 *
 */
public class JDBCSPIndexedRecordCreator {

    private Schema inputSchema;

    private Schema currentComponentSchema;

    private Schema outputSchema;

    AllSetting setting;

    private Map<Integer, AvroConverter> outputFieldLocation2AvroConverter = new HashMap<>();// more often

    private Integer resultSetPostionOfOutputSchema;// less often

    private Map<Integer, Integer> autoPropagatedFieldsFromInputToOutput = new HashMap<>();// less less often

    public void init(Schema currentComponentSchema, Schema outputSchema, AllSetting setting) {
        // for tjdbcsp component, the output schema is the same with current component schema
        this.currentComponentSchema = currentComponentSchema;
        this.outputSchema = outputSchema;
        this.setting = setting;

        if (setting.isFunction()) {
            Schema.Field outputField = getField(outputSchema, setting.getReturnResultIn());
            outputFieldLocation2AvroConverter.put(outputField.pos(), JDBCAvroRegistry.get().getConverter(outputField, 1));
        }

        List<String> parameterColumns = setting.getSchemaColumns();
        List<SPParameterTable.ParameterType> pts = setting.getParameterTypes();
        if (pts != null) {
            int i = setting.isFunction() ? 2 : 1;
            int j = -1;
            for (SPParameterTable.ParameterType pt : pts) {
                j++;
                String columnName = parameterColumns.get(j);

                if (SPParameterTable.ParameterType.RECORDSET == pt) {
                    Schema.Field outputField = getField(outputSchema, columnName);
                    resultSetPostionOfOutputSchema = outputField.pos();
                    continue;
                }

                if (SPParameterTable.ParameterType.OUT == pt || SPParameterTable.ParameterType.INOUT == pt) {
                    Schema.Field outputField = getField(outputSchema, columnName);
                    outputFieldLocation2AvroConverter.put(outputField.pos(), JDBCAvroRegistry.get().getConverter(outputField, i));
                }

                i++;
            }
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

    public IndexedRecord createOutputIndexedRecord(IndexedRecord inputRecord, ResultSet value) {
        if (inputSchema == null) {
            inputSchema = inputRecord.getSchema();
            List<Field> inputFields = inputSchema.getFields();
            for (Schema.Field outputField : outputSchema.getFields()) {
                if (outputFieldLocation2AvroConverter.containsKey(outputField.pos())
                        || ((resultSetPostionOfOutputSchema != null) && (resultSetPostionOfOutputSchema == outputField.pos()))) {
                    continue;
                }

                if (outputField.pos() < inputFields.size()) {// try the more often case, if success, no need the cost time one
                    Field inputField = inputFields.get(outputField.pos());
                    if (inputField.name().equals(outputField.name())) {
                        autoPropagatedFieldsFromInputToOutput.put(outputField.pos(), inputField.pos());
                    }
                    continue;
                }

                // TODO
            }
        }

        return new ResultSetIndexedRecord(value);
    }

    private class ResultSetIndexedRecord implements IndexedRecord {

        private Object[] values;

        public ResultSetIndexedRecord(ResultSet resultSet) {
            try {
                values = new Object[resultSet.getMetaData().getColumnCount()];
                for (int i = 0; i < values.length; i++) {
                    // values[i] = fieldConverter[i].convertToAvro(resultSet);
                }
            } catch (SQLException e) {
                TalendRuntimeException.unexpectedException(e);
            }
        }

        @Override
        public Schema getSchema() {
            return JDBCSPIndexedRecordCreator.this.outputSchema;
        }

        @Override
        public void put(int i, Object v) {
            throw new UnmodifiableAdapterException();
        }

        @Override
        public Object get(int i) {
            return values[i];
        }
    }

}
