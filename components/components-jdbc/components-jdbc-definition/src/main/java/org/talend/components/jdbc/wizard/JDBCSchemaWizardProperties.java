package org.talend.components.jdbc.wizard;

import org.talend.components.api.properties.ComponentPropertiesImpl;
import org.talend.components.common.SchemaProperties;
import org.talend.components.jdbc.module.JDBCTableSelectionModule;
import org.talend.daikon.properties.property.Property;
import org.talend.daikon.properties.property.PropertyFactory;

//only work for the mapping and serialization
public class JDBCSchemaWizardProperties extends ComponentPropertiesImpl {

    public JDBCSchemaWizardProperties(String name) {
        super(name);
    }

    //the same path with the properties which it need to do mapping
    public JDBCTableSelectionModule tableSelection = new JDBCTableSelectionModule("tableSelection");
    public SchemaProperties main = new SchemaProperties("main");
    
    //no need to store it for schema storing in fact, we store it for correct the sql when drag and drop to tjdbcinput and tjdbcrow, regenerate the sql by the tablename which come from the schema
    //maybe this regenerate process need to be done by studio part, but don't know how, so store it
    public Property<String> sql = PropertyFactory.newString("sql").setRequired(true);
}
