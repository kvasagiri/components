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
import java.util.List;

/**
 * Please see org.talend.core.database.EDatabase4DriverClassName and org.talend.designer.core.ui.editor.cmd.QueryGuessCommand, we
 * need to keep the old
 * action works, so only duplicated the logic from there
 */
public enum DatabaseType {
    ACCESS("net.ucanaccess.jdbc.UcanaccessDriver"),
    AS400("com.ibm.as400.access.AS400JDBCDriver"),
    FIREBIRD("org.firebirdsql.jdbc.FBDriver"),
    GODBC("sun.jdbc.odbc.JdbcOdbcDriver"),

    HSQLDB("org.hsqldb.jdbcDriver"),
    HSQLDB_IN_PROGRESS("org.hsqldb.jdbcDriver"),
    HSQLDB_SERVER("org.hsqldb.jdbcDriver"),
    HSQLDB_WEBSERVER("org.hsqldb.jdbcDriver"),

    IBMDB2("com.ibm.db2.jcc.DB2Driver"),
    IBMDB2ZOS("COM.ibm.db2os390.sqlj.jdbc.DB2SQLJDriver"),

    INFORMIX("com.informix.jdbc.IfxDriver"),
    INGRES("com.ingres.jdbc.IngresDriver"),
    INTERBASE("interbase.interclient.Driver"),
    VECTORWISE("com.ingres.jdbc.IngresDriver"),

    JAVADB_DERBYCLIENT("org.apache.derby.jdbc.ClientDriver"),
    JAVADB_EMBEDED("org.apache.derby.jdbc.EmbeddedDriver"),
    JAVADB_JCCJDBC("com.ibm.db2.jcc.DB2Driver"),

    MAXDB("com.sap.dbtech.jdbc.DriverSapDB"),

    MSODBC("sun.jdbc.odbc.JdbcOdbcDriver"),
    MSSQL("net.sourceforge.jtds.jdbc.Driver"),
    MSSQL2("com.microsoft.sqlserver.jdbc.SQLServerDriver"),

    MYSQL("org.gjt.mm.mysql.Driver"),
    MYSQL2("com.mysql.jdbc.Driver"),
    MARIADB("org.mariadb.jdbc.Driver"),
    AMAZON_AURORA("org.gjt.mm.mysql.Driver"),
    NETEZZA("org.netezza.Driver"),

    ORACLEFORSID("oracle.jdbc.OracleDriver", "oracle.jdbc.driver.OracleDriver"),
    ORACLESN("oracle.jdbc.OracleDriver", "oracle.jdbc.driver.OracleDriver"),
    ORACLE_OCI("oracle.jdbc.OracleDriver", "oracle.jdbc.driver.OracleDriver"),
    ORACLE_CUSTOM("oracle.jdbc.OracleDriver", "oracle.jdbc.driver.OracleDriver"),

    PARACCEL("com.paraccel.Driver"),
    REDSHIFT("com.amazon.redshift.jdbc41.Driver"),

    PSQL("org.postgresql.Driver"),
    PLUSPSQL("org.postgresql.Driver"),
    GREENPLUM("org.postgresql.Driver"),

    SAS("com.sas.rio.MVADriver"),
    SAPHana("com.sap.db.jdbc.Driver"),
    SQLITE("org.sqlite.JDBC"),

    SYBASEASE("com.sybase.jdbc3.jdbc.SybDriver", "com.sybase.jdbc3.jdbc.SybDataSource"),
    SYBASEIQ("com.sybase.jdbc3.jdbc.SybDriver", "com.sybase.jdbc3.jdbc.SybDataSource"),
    SYBASEIQ_16("com.sybase.jdbc4.jdbc.SybDriver", "com.sybase.jdbc4.jdbc.SybDataSource"),

    EXASOLUTION("com.exasol.jdbc.EXADriver"),
    TERADATA("com.teradata.jdbc.TeraDriver"),

    VERTICA("com.vertica.Driver"),
    VERTICA2("com.vertica.jdbc.Driver"),

    HIVE("org.apache.hadoop.hive.jdbc.HiveDriver"),

    HIVE2("org.apache.hive.jdbc.HiveDriver"),

    IMPALA("org.apache.hive.jdbc.HiveDriver"),

    H2("org.h2.Driver");

    private String[] usingDriverClasses;

    private DatabaseType(String... usingDriverClasses) {
        this.usingDriverClasses = usingDriverClasses;
    }

    private String[] getDriverClasses() {
        return this.usingDriverClasses;
    }

    private static List<DatabaseType> getDatabaseTypeByDriverClass(String driverClass) {
        List<DatabaseType> dbType4Drivers = new ArrayList<DatabaseType>();
        for (DatabaseType t4d : DatabaseType.values()) {
            String[] drivers = t4d.getDriverClasses();
            if (drivers != null) {
                for (String driver : drivers) {
                    if (driver.equals(driverClass)) {
                        dbType4Drivers.add(t4d);
                        break;
                    }
                }
            }
        }
        return dbType4Drivers;
    }

    public static DatabaseType getDbTypeByClassNameAndDriverJar(String driverClassName, String driverJar) {
        List<DatabaseType> t4d = getDatabaseTypeByDriverClass(driverClassName);
        if (t4d.size() == 1) {
            return t4d.get(0);
        } else if (t4d.size() > 1) {
            if (driverJar == null || "".equals(driverJar) || !driverJar.contains(".jar")) {
                return t4d.get(0);
            } else if (driverJar.contains("postgresql-8.3-603.jdbc3.jar") || driverJar.contains("postgresql-8.3-603.jdbc4.jar")
                    || driverJar.contains("postgresql-8.3-603.jdbc2.jar")) {
                return PSQL;
            } else {
                return t4d.get(0);
            }
        }
        return ORACLE_OCI;
    }

}
