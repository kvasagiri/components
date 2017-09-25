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
package org.talend.components.jdbc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.talend.components.api.component.Connector;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.api.properties.ComponentReferenceProperties;
import org.talend.components.jdbc.module.JDBCConnectionModule;
import org.talend.components.jdbc.runtime.setting.AllSetting;

import org.talend.daikon.exception.TalendRuntimeException;

import org.talend.components.jdbc.tjdbcconnection.TJDBCConnectionDefinition;
import org.talend.components.jdbc.tjdbcconnection.TJDBCConnectionProperties;

import org.talend.daikon.avro.SchemaConstants;

import org.talend.daikon.properties.Properties;
import org.talend.daikon.properties.presentation.Form;

public class CommonUtils {

    /**
     * install the form for the properties
     * 
     * @param props : the properties which the form install on
     * @param formName : the name for the form
     * @return
     */
    public static Form addForm(Properties props, String formName) {
        return new Form(props, formName);
    }

    /**
     * get main schema from the out connector of input components
     * 
     * @param properties
     * @return
     */
    public static Schema getMainSchemaFromOutputConnector(ComponentProperties properties) {
        return getOutputSchema(properties);
    }

    /**
     * get main schema from the input connector of output components
     * 
     * @param properties
     * @return
     */
    public static Schema getMainSchemaFromInputConnector(ComponentProperties properties) {
        Set<? extends Connector> inputConnectors = properties.getPossibleConnectors(false);

        if (inputConnectors == null) {
            return null;
        }

        for (Connector connector : inputConnectors) {
            if (Connector.MAIN_NAME.equals(connector.getName())) {
                return properties.getSchema(connector, false);
            }
        }

        return null;
    }

    /**
     * get the output schema from the properties
     * 
     * @param properties
     * @return
     */
    public static Schema getOutputSchema(ComponentProperties properties) {
        Set<? extends Connector> outputConnectors = properties.getPossibleConnectors(true);

        if (outputConnectors == null) {
            return null;
        }

        for (Connector connector : outputConnectors) {
            if (Connector.MAIN_NAME.equals(connector.getName())) {
                return properties.getSchema(connector, true);
            }
        }

        return null;
    }

    /**
     * get the reject schema from the properties
     * 
     * @param properties
     * @return
     */
    public static Schema getRejectSchema(ComponentProperties properties) {
        Set<? extends Connector> outputConnectors = properties.getPossibleConnectors(true);

        if (outputConnectors == null) {
            return null;
        }

        for (Connector connector : outputConnectors) {
            if (Connector.REJECT_NAME.equals(connector.getName())) {
                return properties.getSchema(connector, true);
            }
        }

        return null;
    }

    /**
     * clone the source schema with a new name and add some fields
     * 
     * @param metadataSchema
     * @param newSchemaName
     * @param moreFields
     * @return
     */
    public static Schema newSchema(Schema metadataSchema, String newSchemaName, List<Schema.Field> moreFields) {
        return newSchema(metadataSchema, newSchemaName, moreFields, metadataSchema.getFields().size());
    }

    public static Schema newSchema(Schema metadataSchema, String newSchemaName, List<Schema.Field> moreFields, int insertPoint) {
        Schema newSchema = Schema.createRecord(newSchemaName, metadataSchema.getDoc(), metadataSchema.getNamespace(),
                metadataSchema.isError());

        List<Field> fields = metadataSchema.getFields();
        List<Schema.Field> copyFieldList = cloneFieldsAndResetPosition(fields);

        copyFieldList.addAll(insertPoint, moreFields);

        newSchema.setFields(copyFieldList);
        for (Map.Entry<String, Object> entry : metadataSchema.getObjectProps().entrySet()) {
            newSchema.addProp(entry.getKey(), entry.getValue());
        }

        return newSchema;
    }

    private static List<Schema.Field> cloneFieldsAndResetPosition(List<Field> fields) {
        List<Schema.Field> copyFieldList = new ArrayList<>();
        for (Schema.Field se : fields) {
            Schema.Field field = new Schema.Field(se.name(), se.schema(), se.doc(), se.defaultVal(), se.order());
            field.getObjectProps().putAll(se.getObjectProps());
            for (Map.Entry<String, Object> entry : se.getObjectProps().entrySet()) {
                field.addProp(entry.getKey(), entry.getValue());
            }
            copyFieldList.add(field);
        }
        return copyFieldList;
    }

    public static Schema mergeRuntimeSchema2DesignSchema4Dynamic(Schema designSchema, Schema runtimeSchema) {
        List<Field> designFields = designSchema.getFields();
        Set<String> designFieldSet = new HashSet<>();
        for (Field designField : designFields) {
            String oname = designField.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME);
            designFieldSet.add(oname);
        }

        List<Schema.Field> dynamicFields = new ArrayList<>();

        for (Field runtimeField : runtimeSchema.getFields()) {
            String oname = runtimeField.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME);
            if (!designFieldSet.contains(oname)) {
                dynamicFields.add(runtimeField);
            }
        }

        dynamicFields = cloneFieldsAndResetPosition(dynamicFields);

        int dynPosition = Integer.valueOf(designSchema.getProp(ComponentConstants.TALEND6_DYNAMIC_COLUMN_POSITION));
        return CommonUtils.newSchema(designSchema, designSchema.getName(), dynamicFields, dynPosition);
    }

    /**
     * fill the connection runtime setting by the connection properties
     * 
     * @param setting
     * @param connection
     */
    public static void setCommonConnectionInfo(AllSetting setting, JDBCConnectionModule connection) {
        if (connection == null) {
            return;
        }

        setting.setDriverPaths(connection.driverTable.drivers.getValue());
        setting.setDriverClass(connection.driverClass.getValue());
        setting.setJdbcUrl(connection.jdbcUrl.getValue());
        setting.setUsername(connection.userPassword.userId.getValue());
        setting.setPassword(connection.userPassword.password.getValue());
    }

    public static boolean setReferenceInfo(AllSetting setting,
            ComponentReferenceProperties<TJDBCConnectionProperties> referencedComponent) {
        if (referencedComponent == null) {
            return false;
        }

        String refComponentIdValue = referencedComponent.componentInstanceId.getStringValue();
        boolean useOtherConnection = refComponentIdValue != null
                && refComponentIdValue.startsWith(TJDBCConnectionDefinition.COMPONENT_NAME);

        if (useOtherConnection) {
            setting.setReferencedComponentId(referencedComponent.componentInstanceId.getValue());
            setting.setReferencedComponentProperties(referencedComponent.getReference());
            return true;
        } else {
            return false;
        }
    }

    public static void setReferenceInfoAndConnectionInfo(AllSetting setting,
            ComponentReferenceProperties<TJDBCConnectionProperties> referencedComponent, JDBCConnectionModule connection) {
        boolean useExistedConnection = setReferenceInfo(setting, referencedComponent);

        if (useExistedConnection) {
            if (referencedComponent.getReference() != null) {//avoid the NPE in JdbcRuntimeInfo
                setCommonConnectionInfo(setting, ((TJDBCConnectionProperties) referencedComponent.getReference()).connection);
            }
        } else {
            setCommonConnectionInfo(setting, connection);
        }
    }

    public static List<String> getAllSchemaFieldNames(Schema schema) {
        List<String> values = new ArrayList<>();

        if (schema == null) {
            return values;
        }

        for (Schema.Field field : schema.getFields()) {
            values.add(field.name());
        }

        return values;
    }

    private static Pattern pattern = Pattern.compile(
            "^SELECT\\s+((?!((\\bINTO\\b)|(\\bFOR\\s+UPDATE\\b)|(\\bLOCK\\s+IN\\s+SHARE\\s+MODE\\b))).)+$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    /**
     * validate if the sql is a pure query statement which don't write or lock something, if a query, return it, if not, throw
     * some exception
     * 
     * @param query
     * @return
     */
    public static String validateQuery(String query) {
        if ((query == null) || "".equals(query) || pattern.matcher(query.trim()).matches()) {
            return query;
        }

        throw TalendRuntimeException.createUnexpectedException(
                "Please check your sql as we only allow the query which don't do write or lock action.");
	}

    public static List<String> getAllSchemaFieldDBNames(Schema schema) {
        List<String> values = new ArrayList<>();

        if (schema == null) {
            return values;
        }

        for (Schema.Field field : schema.getFields()) {
            values.add(field.getProp(SchemaConstants.TALEND_COLUMN_DB_COLUMN_NAME));
        }

        return values;
    }

    // the code below come from azure, will move to tcomp common
    public static String getStudioNameFromProperty(final String inputString) {
        StringBuilder result = new StringBuilder();
        if (inputString == null || inputString.isEmpty()) {
            return inputString;
        }

        for (int i = 0; i < inputString.length(); i++) {
            Character c = inputString.charAt(i);
            result.append(Character.isUpperCase(c) && i > 0 ? "_" + c : c);
        }
        return result.toString().toUpperCase(Locale.ENGLISH);
    }

    public static Schema.Field getField(Schema schema, String fieldName) {
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

}
