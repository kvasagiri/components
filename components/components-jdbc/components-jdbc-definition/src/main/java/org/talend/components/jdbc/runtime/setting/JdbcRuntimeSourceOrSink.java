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
package org.talend.components.jdbc.runtime.setting;

import java.sql.SQLException;
import java.util.List;

import org.apache.avro.Schema;
import org.talend.components.api.component.runtime.SourceOrSink;
import org.talend.components.api.container.RuntimeContainer;

/**
 * common JDBC runtime execution object
 *
 */
public interface JdbcRuntimeSourceOrSink extends SourceOrSink {

    public Schema getSchemaFromQuery(RuntimeContainer runtime, String query);

    // TODO remove it as not useful now
    // work for the wizard : catalog show, TODO make it common
    public List<String> getDBCatalogs(RuntimeContainer runtime) throws ClassNotFoundException, SQLException;

    // TODO remove it as not useful now
    // work for the wizard : schema show after catalog TODO make it common
    public List<String> getDBSchemas(RuntimeContainer runtime, String catalog) throws ClassNotFoundException, SQLException;

    // TODO remove it as not useful now
    // work for the wizard : table show after schema after catalog TODO make it common
    public List<ModuleMetadata> getDBTables(RuntimeContainer runtime, String catalog, String dbschema, String tableNamePattern,
            String[] tableTypes) throws ClassNotFoundException, SQLException;

    // TODO remove it as not useful now
    // work for the schemas store after click finish button TODO make it common
    public List<ModuleMetadata> getDBTables(RuntimeContainer runtime, ModuleMetadata tableid)
            throws ClassNotFoundException, SQLException;
}
