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
package org.talend.components.jdbc.tjdbcinput;

import static org.talend.daikon.properties.presentation.Widget.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.Connector;
import org.talend.components.api.component.ISchemaListener;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.api.properties.ComponentReferenceProperties;
import org.talend.components.common.FixedConnectorsComponentProperties;
import org.talend.components.common.SchemaProperties;
import org.talend.components.common.TrimFieldsTable;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.JdbcRuntimeInfo;
import org.talend.components.jdbc.RuntimeSettingProvider;
import org.talend.components.jdbc.module.DBTypes;
import org.talend.components.jdbc.module.JDBCConnectionModule;
import org.talend.components.jdbc.module.JDBCTableSelectionModule;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.components.jdbc.runtime.setting.JDBCSQLBuilder;
import org.talend.components.jdbc.runtime.setting.JdbcRuntimeSourceOrSink;
import org.talend.components.jdbc.tjdbcconnection.TJDBCConnectionDefinition;
import org.talend.components.jdbc.tjdbcconnection.TJDBCConnectionProperties;
import org.talend.daikon.properties.PresentationItem;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;
import org.talend.daikon.properties.property.Property;
import org.talend.daikon.properties.property.PropertyFactory;
import org.talend.daikon.runtime.RuntimeUtil;
import org.talend.daikon.sandbox.SandboxedInstance;

public class TJDBCInputProperties extends FixedConnectorsComponentProperties implements RuntimeSettingProvider {

    static final Logger LOG = LoggerFactory.getLogger(TJDBCInputProperties.class);

    public TJDBCInputProperties(String name) {
        super(name);
    }

    // main
    public ComponentReferenceProperties<TJDBCConnectionProperties> referencedComponent = new ComponentReferenceProperties<>(
            "referencedComponent", TJDBCConnectionDefinition.COMPONENT_NAME);

    public JDBCConnectionModule connection = new JDBCConnectionModule("connection");

    public final PropertyPathConnector mainConnector = new PropertyPathConnector(Connector.MAIN_NAME, "main");

    public ISchemaListener schemaListener;

    public SchemaProperties main = new SchemaProperties("main") {

        public void afterSchema() {
            if (schemaListener != null) {
                schemaListener.afterSchema();
            }
        }

    };

    public JDBCTableSelectionModule tableSelection = new JDBCTableSelectionModule("tableSelection");

    // TODO query type

    public Property<String> sql = PropertyFactory.newString("sql").setRequired(true);

    public final PresentationItem fetchSchemaFromQuery = new PresentationItem("fetchSchemaFromQuery", "Fetch schema from query");

    public final PresentationItem guessQueryFromSchema = new PresentationItem("guessQueryFromSchema", "Guess query from schema");

    public Property<Boolean> useDataSource = PropertyFactory.newBoolean("useDataSource").setRequired();

    public Property<String> dataSource = PropertyFactory.newProperty("dataSource").setRequired();

    // advanced
    public Property<Boolean> useCursor = PropertyFactory.newBoolean("useCursor").setRequired();

    public Property<Integer> cursor = PropertyFactory.newInteger("cursor").setRequired();

    public Property<Boolean> trimStringOrCharColumns = PropertyFactory.newBoolean("trimStringOrCharColumns").setRequired();

    public TrimFieldsTable trimTable = new TrimFieldsTable("trimTable");

    public Property<Boolean> enableDBMapping = PropertyFactory.newBoolean("enableDBMapping").setRequired();

    public Property<DBTypes> dbMapping = PropertyFactory.newEnum("dbMapping", DBTypes.class);

    @Override
    public void setupLayout() {
        super.setupLayout();

        Form mainForm = CommonUtils.addForm(this, Form.MAIN);

        Widget compListWidget = widget(referencedComponent).setWidgetType(Widget.COMPONENT_REFERENCE_WIDGET_TYPE);
        mainForm.addRow(compListWidget);

        mainForm.addRow(connection.getForm(Form.MAIN));
        mainForm.addRow(main.getForm(Form.REFERENCE));

        mainForm.addRow(tableSelection.getForm(Form.REFERENCE));
        mainForm.addRow(Widget.widget(sql).setWidgetType(Widget.TEXT_AREA_WIDGET_TYPE));

        mainForm.addRow(Widget.widget(fetchSchemaFromQuery).setWidgetType(Widget.BUTTON_WIDGET_TYPE));
        mainForm.addColumn(Widget.widget(guessQueryFromSchema).setWidgetType(Widget.BUTTON_WIDGET_TYPE));

        mainForm.addRow(useDataSource);
        mainForm.addRow(dataSource);

        Form advancedForm = CommonUtils.addForm(this, Form.ADVANCED);
        advancedForm.addRow(useCursor);
        advancedForm.addRow(cursor);
        advancedForm.addRow(trimStringOrCharColumns);
        advancedForm.addRow(widget(trimTable).setWidgetType(Widget.TABLE_WIDGET_TYPE));
        advancedForm.addRow(enableDBMapping);
        advancedForm.addRow(widget(dbMapping).setWidgetType(Widget.ENUMERATION_WIDGET_TYPE));
    }

    @Override
    public void setupProperties() {
        super.setupProperties();

        sql.setValue("select id, name from employee");

        cursor.setValue(1000);

        tableSelection.setConnection(this);

        dbMapping.setValue(DBTypes.MYSQL);

        // FIXME now the trigger can't work very well, need a check why
        schemaListener = new ISchemaListener() {

            @Override
            public void afterSchema() {
                updateTrimTable();
            }

        };
    }

    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);

        String refComponentIdValue = referencedComponent.componentInstanceId.getStringValue();
        boolean useOtherConnection = refComponentIdValue != null
                && refComponentIdValue.startsWith(TJDBCConnectionDefinition.COMPONENT_NAME);

        if (form.getName().equals(Form.MAIN)) {
            form.getChildForm(connection.getName()).setHidden(useOtherConnection);
            if (useOtherConnection) {
                form.getWidget(useDataSource.getName()).setHidden(true);
                form.getWidget(dataSource.getName()).setHidden(true);
            } else {
                form.getWidget(useDataSource.getName()).setHidden(false);
                form.getWidget(dataSource.getName()).setHidden(!useDataSource.getValue());
            }
        }

        if (form.getName().equals(Form.ADVANCED)) {
            form.getWidget(cursor.getName()).setHidden(!useCursor.getValue());
            form.getWidget(trimTable.getName()).setHidden(trimStringOrCharColumns.getValue());
            form.getWidget(dbMapping.getName()).setVisible(enableDBMapping.getValue());
        }
    }

    private void updateTrimTable() {
        Schema schema = main.schema.getValue();
        if (schema == null) {
            return;
        }

        List<String> fieldNames = new ArrayList<>();
        for (Schema.Field f : schema.getFields()) {
            fieldNames.add(f.name());
        }
        trimTable.columnName.setValue(fieldNames);
    }

    public void afterReferencedComponent() {
        refreshLayout(getForm(Form.MAIN));
    }

    public void afterUseDataSource() {
        refreshLayout(getForm(Form.MAIN));
    }

    public void afterUseCursor() {
        refreshLayout(getForm(Form.ADVANCED));
    }

    public void afterTrimStringOrCharColumns() {
        refreshLayout(getForm(Form.ADVANCED));
    }

    public void afterDbMapping() {
        refreshLayout(getForm(Form.ADVANCED));
    }

    @Override
    protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(boolean isOutputConnection) {
        if (isOutputConnection) {
            return Collections.singleton(mainConnector);
        }
        return Collections.emptySet();
    }

    public ValidationResult afterFetchSchemaFromQuery() {
        JdbcRuntimeInfo jdbcRuntimeInfo = new JdbcRuntimeInfo(this, "org.talend.components.jdbc.runtime.JDBCSourceOrSink");
        try (SandboxedInstance sandboxI = RuntimeUtil.createRuntimeClass(jdbcRuntimeInfo,
                connection.getClass().getClassLoader())) {
            JdbcRuntimeSourceOrSink ss = (JdbcRuntimeSourceOrSink) sandboxI.getInstance();
            ss.initialize(null, this);
            try {
                Schema schema = ss.getSchemaFromQuery(null, sql.getValue());
                main.schema.setValue(schema);
            } catch (Exception e) {
                LOG.error("failed to retrieve the schema :", e);
                return new ValidationResult(ValidationResult.Result.ERROR, e.getCause().getMessage());
            }
        }
        return ValidationResult.OK;
    }

    public ValidationResult afterGuessQueryFromSchema() {
        String tablename = tableSelection.tablename.getValue();
        Schema schema = main.schema.getValue();
        if (tablename == null || tablename.isEmpty()) {
            return new ValidationResult(ValidationResult.Result.ERROR, "Please set the table name before it");
        }
        if (schema == null || schema.getFields().isEmpty()) {
            return new ValidationResult(ValidationResult.Result.ERROR, "Please set the schema before it");
        }
        String query = JDBCSQLBuilder.getInstance().generateSQL4SelectTable(tablename, schema);
        sql.setValue("\"" + query + "\"");

        // TODO : it doesn't work
        refreshLayout(getForm(Form.MAIN));

        return ValidationResult.OK;
    }

    @Override
    public AllSetting getRuntimeSetting() {
        AllSetting setting = new AllSetting();

        setting.setReferencedComponentId(referencedComponent.componentInstanceId.getValue());
        setting.setReferencedComponentProperties(referencedComponent.getReference());

        CommonUtils.setCommonConnectionInfo(setting, connection);

        setting.setTablename(this.tableSelection.tablename.getValue());
        setting.setSql(this.sql.getValue());

        setting.setUseCursor(this.useCursor.getValue());
        setting.setCursor(this.cursor.getValue());

        setting.setTrimStringOrCharColumns(this.trimStringOrCharColumns.getValue());

        Object v1 = this.trimTable.columnName.getValue();
        if (v1 != null && v1 instanceof List) {
            setting.setTrimColumns((List) v1);
        }

        Object v2 = this.trimTable.trim.getValue();
        if (v2 != null && v2 instanceof List) {
            setting.setTrimValues((List) v2);
        }

        setting.setEnableDBMapping(this.enableDBMapping.getValue());
        setting.setDbMapping(this.dbMapping.getValue());

        setting.setUseDataSource(useDataSource.getValue());
        setting.setDataSource(dataSource.getValue());

        setting.setSchema(main.schema.getValue());

        return setting;
    }

}
