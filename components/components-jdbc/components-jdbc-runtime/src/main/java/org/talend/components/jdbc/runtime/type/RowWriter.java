package org.talend.components.jdbc.runtime.type;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.IndexedRecord;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.runtime.setting.JDBCSQLBuilder;
import org.talend.daikon.avro.AvroUtils;

public class RowWriter {

    private TypeWriter[] typeWriters;

    public RowWriter(List<JDBCSQLBuilder.Column> columnList, Schema inputSchema, Schema componentSchema,
            PreparedStatement statement) {
        List<TypeWriter> writers = new ArrayList<TypeWriter>();

        int statementIndex = 0;

        // work for the case for the rest service
        if (inputSchema == null || inputSchema.getFields() == null || inputSchema.getFields().isEmpty()) {
            inputSchema = componentSchema;
        }

        for (JDBCSQLBuilder.Column column : columnList) {
            Field inputField = CommonUtils.getField(inputSchema, column.columnLabel);

            Field componentField = CommonUtils.getField(componentSchema, column.columnLabel);
            int inputValueLocation = inputField.pos();
            statementIndex++;

            Schema basicSchema = AvroUtils.unwrapIfNullable(componentField.schema());
            // TODO any difference for nullable
            // boolean nullable = AvroUtils.isNullable(componentField.schema());

            TypeWriter writer = null;

            if (AvroUtils.isSameType(basicSchema, AvroUtils._string())) {
                writer = new StringTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._int())) {
                writer = new IntTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._date())) {
                writer = new DateTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._decimal())) {
                writer = new BigDecimalTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._long())) {
                writer = new LongTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._double())) {
                writer = new DoubleTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._float())) {
                writer = new FloatTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._boolean())) {
                writer = new BooleanTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._short())) {
                writer = new ShortTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._character())) {
                writer = new CharacterTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._byte())) {
                writer = new ByteTypeWriter(statement, statementIndex, inputValueLocation);
            } else if (AvroUtils.isSameType(basicSchema, AvroUtils._bytes())) {
                writer = new BytesTypeWriter(statement, statementIndex, inputValueLocation);
            } else {
                writer = new ObjectTypeWriter(statement, statementIndex, inputValueLocation);
            }

            writers.add(writer);
        }

        typeWriters = writers.toArray(new TypeWriter[0]);
    }

    public void write(IndexedRecord input) throws SQLException {
        for (TypeWriter writer : typeWriters) {
            writer.write(input);
        }
    }

    class TypeWriter {

        protected final PreparedStatement statement;

        protected final int statementIndex;

        protected final int inputValueLocation;

        protected TypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            this.statement = statement;
            this.statementIndex = statementIndex;
            this.inputValueLocation = inputValueLocation;
        }

        void write(IndexedRecord input) throws SQLException {
            // do nothing
        }

    }

    class StringTypeWriter extends TypeWriter {

        StringTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.VARCHAR);
            } else {
                statement.setString(statementIndex, (String) inputValue);
            }
        }

    }

    class IntTypeWriter extends TypeWriter {

        IntTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.INTEGER);
            } else {
                statement.setInt(statementIndex, (int) inputValue);
            }
        }

    }

    class DateTypeWriter extends TypeWriter {

        DateTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.TIMESTAMP);
            } else {
                if (inputValue instanceof Date) {
                    statement.setTimestamp(statementIndex, new Timestamp(((Date) inputValue).getTime()));
                } else {
                    statement.setTimestamp(statementIndex, new Timestamp((long) inputValue));
                }
            }
        }

    }

    class BigDecimalTypeWriter extends TypeWriter {

        BigDecimalTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.DECIMAL);
            } else {
                // TODO check if it's right
                statement.setBigDecimal(statementIndex, (BigDecimal) inputValue);
            }
        }

    }

    class LongTypeWriter extends TypeWriter {

        LongTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.INTEGER);
            } else {
                statement.setLong(statementIndex, (long) inputValue);
            }
        }

    }

    class DoubleTypeWriter extends TypeWriter {

        DoubleTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.DOUBLE);
            } else {
                statement.setDouble(statementIndex, (double) inputValue);
            }
        }

    }

    class FloatTypeWriter extends TypeWriter {

        FloatTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.FLOAT);
            } else {
                statement.setFloat(statementIndex, (float) inputValue);
            }
        }

    }

    class BooleanTypeWriter extends TypeWriter {

        BooleanTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.BOOLEAN);
            } else {
                statement.setBoolean(statementIndex, (boolean) inputValue);
            }
        }

    }

    class ShortTypeWriter extends TypeWriter {

        ShortTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.INTEGER);
            } else {
                statement.setShort(statementIndex, (short) inputValue);
            }
        }

    }

    class ByteTypeWriter extends TypeWriter {

        ByteTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.INTEGER);
            } else {
                statement.setByte(statementIndex, (byte) inputValue);
            }
        }

    }

    class CharacterTypeWriter extends TypeWriter {

        CharacterTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.CHAR);
            } else {
                statement.setInt(statementIndex, (char) inputValue);
            }
        }

    }

    class BytesTypeWriter extends TypeWriter {

        BytesTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            // TODO
        }

    }

    class ObjectTypeWriter extends TypeWriter {

        ObjectTypeWriter(PreparedStatement statement, int statementIndex, int inputValueLocation) {
            super(statement, statementIndex, inputValueLocation);
        }

        public void write(IndexedRecord input) throws SQLException {
            Object inputValue = input.get(inputValueLocation);
            if (inputValue == null) {
                statement.setNull(statementIndex, java.sql.Types.JAVA_OBJECT);
            } else {
                statement.setObject(statementIndex, inputValue);
            }
        }

    }

}
