package org.talend.components.jdbc.module;

import static org.talend.daikon.properties.property.PropertyFactory.newProperty;

import java.util.List;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.talend.components.api.properties.ComponentPropertiesImpl;
import org.talend.components.jdbc.CommonUtils;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.property.Property;

public class FieldOptionsTable extends ComponentPropertiesImpl {

    private static final TypeLiteral<List<String>> LIST_STRING_TYPE = new TypeLiteral<List<String>>() {
    };

    private static final TypeLiteral<List<Boolean>> LIST_BOOLEAN_TYPE = new TypeLiteral<List<Boolean>>() {
    };

    public FieldOptionsTable(String name) {
        super(name);
    }

    public Property<List<String>> schemaColumns = newProperty(LIST_STRING_TYPE, "schemaColumns");

    public Property<List<Boolean>> updateKey = newProperty(LIST_BOOLEAN_TYPE, "trim");

    public Property<List<Boolean>> deletionKey = newProperty(LIST_BOOLEAN_TYPE, "trim");

    public Property<List<Boolean>> updatable = newProperty(LIST_BOOLEAN_TYPE, "trim");

    public Property<List<Boolean>> insertable = newProperty(LIST_BOOLEAN_TYPE, "trim");

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = CommonUtils.addForm(this, Form.MAIN);
        mainForm.addColumn(schemaColumns);
        mainForm.addColumn(updateKey);
        mainForm.addColumn(deletionKey);
        mainForm.addColumn(updatable);
        mainForm.addColumn(insertable);
    }

}
