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
import static org.talend.daikon.properties.property.PropertyFactory.newProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.talend.components.api.properties.ComponentPropertiesImpl;
import org.talend.components.jdbc.CommonUtils;
import org.talend.components.jdbc.JdbcRuntimeInfo;
import org.talend.components.jdbc.RuntimeSettingProvider;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.components.jdbc.runtime.setting.JDBCSQLBuilder;
import org.talend.components.jdbc.runtime.setting.JdbcRuntimeSourceOrSink;
import org.talend.components.jdbc.runtime.setting.ModuleMetadata;
import org.talend.daikon.NamedThing;
import org.talend.daikon.properties.Properties;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;
import org.talend.daikon.properties.property.Property;
import org.talend.daikon.properties.property.PropertyFactory;
import org.talend.daikon.properties.service.Repository;
import org.talend.daikon.runtime.RuntimeUtil;
import org.talend.daikon.sandbox.SandboxedInstance;

public class JDBCRetrieveSchemaWizardProperties extends ComponentPropertiesImpl implements RuntimeSettingProvider {

    public static final String FORM_PAGE1 = "page1";

    public static final String FORM_PAGE2 = "page2";

    public static final String FORM_PAGE3 = "page3";

    private JDBCConnectionWizardProperties wizardConnectionProperties;

    private String repositoryLocation;

    // write a simple pages firstly, TODO the complex one
    // page1
    public Property<String> filter = PropertyFactory.newString("filter");

    // page2
    // studio will fill the value for the property below after beforeFormPresent and before afterFormFinish method auto, don't
    // know how
    // they do that, but it mean we no
    // need to store it to the item file. But we will store it in fact as it should works with the filter property above together
    public Property<List<NamedThing>> selectedModuleNames = newProperty(new TypeLiteral<List<NamedThing>>() {
    }, "selectedModuleNames");

    // page3 TODO or no

    public JDBCRetrieveSchemaWizardProperties(String name) {
        super(name);
    }

    public JDBCRetrieveSchemaWizardProperties setConnection(JDBCConnectionWizardProperties connection) {
        this.wizardConnectionProperties = connection;
        if (connection.filter != null) {
            filter.setValue(connection.filter);
        }
        if (connection.moduleNames != null) {
            selectedModuleNames.setValue(connection.moduleNames);
        }
        return this;
    }

    public JDBCRetrieveSchemaWizardProperties setRepositoryLocation(String location) {
        repositoryLocation = location;
        return this;
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form page1 = Form.create(this, FORM_PAGE1);
        page1.addRow(filter);

        Form page2 = Form.create(this, FORM_PAGE2);
        page2.addRow(widget(selectedModuleNames).setWidgetType(Widget.NAME_SELECTION_AREA_WIDGET_TYPE));

        // TODO or no page3
    }

    public void beforeFormPresentPage1() throws Exception {
        getForm(FORM_PAGE1).setAllowBack(true);
        getForm(FORM_PAGE1).setAllowForward(true);
        getForm(FORM_PAGE1).setAllowFinish(true);
    }

    // we use this to make sure not lost modules
    private Set<NamedThing> checkedModulesButWillNotShow = new HashSet<>();

    public void beforeFormPresentPage2() throws Exception {
        checkedModulesButWillNotShow.clear();

        JdbcRuntimeInfo jdbcRuntimeInfo = new JdbcRuntimeInfo(this, "org.talend.components.jdbc.runtime.JDBCSourceOrSink");
        try (SandboxedInstance sandboxI = RuntimeUtil.createRuntimeClass(jdbcRuntimeInfo,
                wizardConnectionProperties.getClass().getClassLoader())) {
            JdbcRuntimeSourceOrSink sourceOrSink = (JdbcRuntimeSourceOrSink) sandboxI.getInstance();
            sourceOrSink.initialize(null, this);
            List<NamedThing> moduleNames = sourceOrSink.getSchemaNames(null);

            String filterValue = filter.getValue();
            final List<NamedThing> result;
            if (filterValue == null || filterValue.isEmpty()) {
                result = moduleNames;
            } else {
                result = new ArrayList<>();
                for (NamedThing name : moduleNames) {
                    if (name.getName().contains(filterValue)) {
                        result.add(name);
                    }
                }
            }

            if (wizardConnectionProperties.moduleNames != null) {
                for (NamedThing moduleName : wizardConnectionProperties.moduleNames) {
                    if (!result.contains(moduleName)) {
                        checkedModulesButWillNotShow.add(moduleName);
                    }
                }
            }

            selectedModuleNames.setPossibleValues(result);

            getForm(FORM_PAGE2).setAllowBack(true);
            getForm(FORM_PAGE2).setAllowFinish(true);
        }
    }

    public ValidationResult afterFormFinishPage2(Repository<Properties> repo) throws Exception {
        // store them to item file
        wizardConnectionProperties.filter = filter.getValue();

        List<NamedThing> currentSelectedModules = selectedModuleNames.getValue();
        currentSelectedModules.addAll(checkedModulesButWillNotShow);
        wizardConnectionProperties.moduleNames = currentSelectedModules;

        JdbcRuntimeInfo jdbcRuntimeInfo = new JdbcRuntimeInfo(this, "org.talend.components.jdbc.runtime.JDBCSourceOrSink");
        try (SandboxedInstance sandboxI = RuntimeUtil.createRuntimeClass(jdbcRuntimeInfo,
                wizardConnectionProperties.getClass().getClassLoader())) {
            JdbcRuntimeSourceOrSink sourceOrSink = (JdbcRuntimeSourceOrSink) sandboxI.getInstance();
            sourceOrSink.initialize(null, this);
            ValidationResult vr = sourceOrSink.validate(null);
            if (vr.getStatus() != ValidationResult.Result.OK) {
                return vr;
            }

            String connRepLocation = repo.storeProperties(wizardConnectionProperties, wizardConnectionProperties.name.getValue(),
                    repositoryLocation, null);

            // store schemas
            for (NamedThing nl : wizardConnectionProperties.moduleNames) {
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

            return ValidationResult.OK;
        }
    }

    @Override
    public AllSetting getRuntimeSetting() {
        AllSetting setting = new AllSetting();

        if (wizardConnectionProperties != null) {
            CommonUtils.setCommonConnectionInfo(setting, wizardConnectionProperties.connection);
        }

        return setting;
    }
}
