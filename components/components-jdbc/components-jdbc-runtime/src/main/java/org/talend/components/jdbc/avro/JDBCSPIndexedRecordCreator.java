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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;
import org.talend.components.common.avro.JDBCAvroRegistry;
import org.talend.components.jdbc.CommonUtils;
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

    private Schema currentComponentSchema;

    private Schema outputSchema;

    AllSetting setting;

    // the fields which need converter
    private Map<Integer, AvroConverter> outputFieldLocation2AvroConverter = new HashMap<>();// more often

    // the field which store the whole result set object
    private int resultSetPostionOfOutputSchema = -1;// less often

    // the fields which propagate from input to output directly
    private Map<Integer, Integer> autoPropagatedFieldsFromInputToOutput = new HashMap<>();// less less often

    public void init(Schema currentComponentSchema, Schema outputSchema, AllSetting setting) {
        // for tjdbcsp component, the output schema is the same with current component schema
        this.currentComponentSchema = currentComponentSchema;
        this.outputSchema = outputSchema;
        this.setting = setting;

        if (setting.isFunction()) {
            Schema.Field outputField = CommonUtils.getField(outputSchema, setting.getReturnResultIn());
            outputFieldLocation2AvroConverter.put(outputField.pos(), JDBCAvroRegistry.get().getConverter(outputField, 1));
        }

        List<String> parameterColumns = setting.getSchemaColumns4SPParameters();
        List<SPParameterTable.ParameterType> pts = setting.getParameterTypes();
        if (pts != null) {
            int i = setting.isFunction() ? 2 : 1;
            int j = -1;
            for (SPParameterTable.ParameterType pt : pts) {
                j++;
                String columnName = parameterColumns.get(j);

                if (SPParameterTable.ParameterType.RECORDSET == pt) {
                    Schema.Field outputField = CommonUtils.getField(outputSchema, columnName);
                    resultSetPostionOfOutputSchema = outputField.pos();
                    continue;
                }

                if (SPParameterTable.ParameterType.OUT == pt || SPParameterTable.ParameterType.INOUT == pt) {
                    Schema.Field outputField = CommonUtils.getField(outputSchema, columnName);
                    outputFieldLocation2AvroConverter.put(outputField.pos(), JDBCAvroRegistry.get().getConverter(outputField, i));
                }

                i++;
            }
        }
    }

    private boolean firstRowHaveCame = false;

    public IndexedRecord createOutputIndexedRecord(ResultSet value, IndexedRecord inputRecord) {
        if (!firstRowHaveCame) {
            firstRowHaveCame = true;

            Schema inputSchema = null;
            if (inputRecord != null) {
                inputSchema = inputRecord.getSchema();
            }

            Map<String, Field> inputFieldMap = null;

            for (Schema.Field outputField : outputSchema.getFields()) {
                if (outputFieldLocation2AvroConverter.containsKey(outputField.pos())
                        || (resultSetPostionOfOutputSchema == outputField.pos())) {
                    continue;
                }

                if (inputSchema == null) {
                    continue;
                }

                List<Field> inputFields = inputSchema.getFields();

                if (inputFieldMap == null) {
                    inputFieldMap = new HashMap<>();
                    for (Field inputField : inputFields) {
                        inputFieldMap.put(inputField.name(), inputField);
                    }
                }

                Field inputField = inputFieldMap.get(outputField.name());
                if (inputField != null) {
                    autoPropagatedFieldsFromInputToOutput.put(outputField.pos(), inputField.pos());
                }
            }
        }

        return new ResultSetIndexedRecord(value, inputRecord);
    }

    private class ResultSetIndexedRecord implements IndexedRecord {

        private Object[] values;

        public ResultSetIndexedRecord(ResultSet resultSet, IndexedRecord inputRecord) {
            try {
                List<Field> outputFields = outputSchema.getFields();
                values = new Object[outputFields.size()];
                for (int i = 0; i < values.length; i++) {
                    AvroConverter converter = outputFieldLocation2AvroConverter.get(i);
                    if (converter != null) {
                        values[i] = converter.convertToAvro(resultSet);
                        continue;
                    }

                    if (resultSetPostionOfOutputSchema == i) {
                        values[i] = resultSet;
                        continue;
                    }

                    Integer inputLocation = autoPropagatedFieldsFromInputToOutput.get(i);
                    if (inputLocation != null && inputRecord != null) {
                        values[i] = inputRecord.get(inputLocation);
                    }

                    // the other fields in the output indexed record is null
                }
            } catch (Exception e) {
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
