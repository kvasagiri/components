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
package org.talend.components.jdbc.runtime;

import java.sql.Connection;
import java.sql.SQLException;

import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.component.runtime.Sink;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.container.RuntimeContainer;

/**
 * JDBC runtime execution object for output action
 *
 */
public class JDBCSink extends JDBCSourceOrSink implements Sink {

    private static final long serialVersionUID = 3228265006313531905L;

    @Override
    public WriteOperation<Result> createWriteOperation() {
        return new JDBCOutputWriteOperation(this);
    }

    @Override
    public Connection connect(RuntimeContainer runtime) throws ClassNotFoundException, SQLException {
        String refComponentId = setting.getReferencedComponentId();
        // using another component's connection
        if (refComponentId != null) {
            return JdbcRuntimeUtils.fetchConnectionFromContextOrCreateNew(setting, runtime);
        } else {
            // TODO now we use routines.system.TalendDataSource to get the data connection from the ESB runtime, but now we
            // can't
            // refer it by the new framework, so will fix it later
            
            Connection conn = JdbcRuntimeUtils.createConnection(setting);

            Integer commitEvery = setting.getCommitEvery();
            if (commitEvery != null && commitEvery > 0) {
                conn.setAutoCommit(false);
            }

            return conn;
        }
    }

}
