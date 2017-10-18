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
package org.talend.components.jdbc.wizard;

import static org.talend.daikon.properties.presentation.Widget.widget;

import java.util.List;

import org.apache.avro.Schema;
import org.talend.components.api.properties.ComponentPropertiesImpl;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.JdbcRuntimeInfo;
import org.talend.components.jdbc.RuntimeSettingProvider;
import org.talend.components.jdbc.module.JDBCConnectionModule;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.components.jdbc.runtime.setting.JDBCSQLBuilder;
import org.talend.components.jdbc.runtime.setting.JdbcRuntimeSourceOrSink;
import org.talend.components.jdbc.runtime.setting.ModuleMetadata;
import org.talend.daikon.NamedThing;
import org.talend.daikon.properties.PresentationItem;
import org.talend.daikon.properties.Properties;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;
import org.talend.daikon.properties.property.Property;
import org.talend.daikon.properties.property.PropertyFactory;
import org.talend.daikon.properties.service.Repository;
import org.talend.daikon.runtime.RuntimeUtil;
import org.talend.daikon.sandbox.SandboxedInstance;

/**
 * this is the entrance properties for all the wizards(root jdbc wizard, all the right click wizards), we will store it in the
 * item file as the root properties, the schemas are the sub elements of it, we may also need to store the query as a property of
 * this
 * properties for the reading and editing query wizard though the query will not be displayed in this properties
 *
 */
public class JDBCConnectionWizardProperties extends ComponentPropertiesImpl implements RuntimeSettingProvider {

    public Property<String> name = PropertyFactory.newString("name").setRequired();

    private String repositoryLocation;

    public JDBCConnectionModule connection = new JDBCConnectionModule("connection");
    
    public Property<String> mappingFile = PropertyFactory.newProperty("mappingFile");

    public PresentationItem testConnection = new PresentationItem("testConnection", "Test connection");

    // only for store information to item file
    public List<NamedThing> querys;

    public String filter;

    public List<NamedThing> moduleNames;

    public JDBCConnectionWizardProperties(String name) {
        super(name);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();
    }

    @Override
    public void setupLayout() {
        super.setupLayout();

        Form wizardForm = CommonUtils.addForm(this, Form.MAIN);
        wizardForm.addRow(name);
        wizardForm.addRow(connection.getForm(Form.MAIN));
        wizardForm.addRow(widget(mappingFile).setWidgetType("widget.type.mappingType"));
        wizardForm.addRow(widget(testConnection).setLongRunning(true).setWidgetType(Widget.BUTTON_WIDGET_TYPE));
    }

    public ValidationResult validateTestConnection() {
        JdbcRuntimeInfo jdbcRuntimeInfo = new JdbcRuntimeInfo(this, "org.talend.components.jdbc.runtime.JDBCSourceOrSink");
        try (SandboxedInstance sandboxI = RuntimeUtil.createRuntimeClass(jdbcRuntimeInfo,
                connection.getClass().getClassLoader())) {
            JdbcRuntimeSourceOrSink sourceOrSink = (JdbcRuntimeSourceOrSink) sandboxI.getInstance();
            sourceOrSink.initialize(null, this);
            ValidationResult vr = sourceOrSink.validate(null);
            if (vr.getStatus() == ValidationResult.Result.OK) {
                vr = new ValidationResult(ValidationResult.Result.OK, "Connection successful");
                getForm(Form.MAIN).setAllowFinish(true);
            } else {
                getForm(Form.MAIN).setAllowFinish(false);
            }
            return vr;
        }
    }

    public JDBCConnectionWizardProperties setRepositoryLocation(String location) {
        repositoryLocation = location;
        return this;
    }

    public ValidationResult afterFormFinishMain(Repository<Properties> repo) throws Exception {
        JdbcRuntimeInfo jdbcRuntimeInfo = new JdbcRuntimeInfo(this, "org.talend.components.jdbc.runtime.JDBCSourceOrSink");
        try (SandboxedInstance sandboxI = RuntimeUtil.createRuntimeClass(jdbcRuntimeInfo,
                connection.getClass().getClassLoader())) {
            JdbcRuntimeSourceOrSink sourceOrSink = (JdbcRuntimeSourceOrSink) sandboxI.getInstance();
            sourceOrSink.initialize(null, this);
            ValidationResult vr = sourceOrSink.validate(null);
            if (vr.getStatus() != ValidationResult.Result.OK) {
                return vr;
            }

            String connRepLocation = repo.storeProperties(this, this.name.getValue(), repositoryLocation, null);
            // we have to store schemas as the implement of storeProperties method, it will overwrite the item every time, so we
            // have to store old schemas here
            // as we will use the old way for schema list retrieve, so not sure we need to do this here, maybe should remove it,
            // and adjust storeProperties, TODO need to discuss with studio team
            if (moduleNames != null) {
                for (NamedThing nl : moduleNames) {
                    String tablename = nl.getName();
                    // Schema schema = sourceOrSink.getEndpointSchema(null, tablename);
                    List<ModuleMetadata> modules = sourceOrSink.getDBTables(null,
                            new ModuleMetadata(null/* catalog : need to fetch it from somewhere */,
                                    null/* schema : need to fetch it from somewhere */, tablename,
                                    null/* type : need to fetch it from somewhere */, null, null)// a ID for a
                                                                                                 // database
                                                                                                 // module
                    );

                    if (modules == null || modules.isEmpty()) {
                        continue;
                    }

                    JDBCSchemaWizardProperties properties = new JDBCSchemaWizardProperties(tablename);
                    properties.init();

                    properties.tableSelection.tablename.setValue(tablename);

                    Schema schema = modules.get(0).schema;
                    properties.main.schema.setValue(schema);
                    properties.sql.setValue(JDBCSQLBuilder.getInstance().generateSQL4SelectTable(tablename, schema));
                    repo.storeProperties(properties, tablename, connRepLocation, "main.schema");
                }
            }
            return ValidationResult.OK;
        }
    }

    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);

    }

    @Override
    public AllSetting getRuntimeSetting() {
        AllSetting setting = new AllSetting();

        CommonUtils.setCommonConnectionInfo(setting, connection);

        return setting;
    }

}
