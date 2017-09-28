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
package org.talend.components.jdbc.module;

import static org.talend.daikon.properties.presentation.Widget.widget;

import org.talend.components.api.properties.ComponentPropertiesImpl;
import org.talend.components.common.UserPasswordProperties;
import org.talend.components.jdbc.CommonUtils;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;
import org.talend.daikon.properties.property.Property;
import org.talend.daikon.properties.property.PropertyFactory;

/**
 * common JDBC connection information properties
 *
 */
// have to implement ComponentProperties for the wizard part, not good
public class JDBCConnectionModule extends ComponentPropertiesImpl {

    public Property<String> jdbcUrl = PropertyFactory.newProperty("jdbcUrl").setRequired();

    // TODO use the right widget for it
    public DriverTable driverTable = new DriverTable("driverTable");

    public Property<String> driverClass = PropertyFactory.newProperty("driverClass").setRequired();

    public UserPasswordProperties userPassword = new UserPasswordProperties("userPassword");

    public JDBCConnectionModule(String name) {
        super(name);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();
        jdbcUrl.setValue("jdbc:");
    }

    @Override
    public void setupLayout() {
        super.setupLayout();

        Form form = CommonUtils.addForm(this, Form.MAIN);
        form.addRow(jdbcUrl);
        form.addRow(widget(driverTable).setWidgetType(Widget.TABLE_WIDGET_TYPE));
        form.addRow(driverClass);
        form.addRow(userPassword.getForm(Form.MAIN));
    }

    public void setNotRequired() {
        this.jdbcUrl.setRequired(false);
        this.driverClass.setRequired(false);
        this.userPassword.userId.setRequired(false);
        this.userPassword.password.setRequired(false);
    }

}
