package org.talend.components.jdbc.query;

import java.util.List;

import org.talend.components.jdbc.runtime.setting.AllSetting;

public class QueryUtils {

    public String generateNewQuery(final String dbType, final String databaseDisplayed/* not used now */,
            final String dbschemaDisplayed/* not used now */,
            final String tableDisplayed/* "mytable" or context.mytable or more complex */,
            final AllSetting setting/*
                                     * all values in it already be converted to real value, for example, context.id is changed to "myid"
                                     * already
                                     */) {
        final String realDbType = getRealDBType(setting, dbType);

        IQueryGenerator generator = GenerateQueryFactory.getGenerator(realDbType);
        
        generator.setParameters(databaseDisplayed, dbschemaDisplayed, tableDisplayed, setting);
        return generator.generateQuery();
    }

    /**
     * computer the real database type by driver jar and class, this is useful for the tjdbcxxx
     * 
     * @param setting
     * @param dbType
     * @return
     */
    private String getRealDBType(AllSetting setting, String dbType) {
        if (dbType == null || dbType.equals(EDatabaseTypeName.GENERAL_JDBC.getDisplayName())) {
            String driverClassName = setting.getDriverClass();

            if ("com.sybase.jdbc3.jdbc.SybDataSource".equals(driverClassName)) {
                driverClassName = EDatabase4DriverClassName.SYBASEASE.getDriverClass();
            }

            List<String> driverPaths = setting.getDriverPaths();
            StringBuilder sb = new StringBuilder();
            for (String path : driverPaths) {
                sb.append(path);
            }
            String driverJarInfo = sb.toString();

            dbType = getDbTypeByClassNameAndDriverJar(driverClassName, driverJarInfo);

            if (dbType == null) {
                // if we can not get the DB Type from the existing driver list, just set back the type to ORACLE
                // since it's one DB unknown from Talend.
                // it might not work directly for all DB, but it will generate a standard query.
                dbType = EDatabaseTypeName.ORACLE_OCI.getDisplayName();
            }
        }
        return dbType;
    }

    // hywang add for bug 7575
    private static String getDbTypeByClassNameAndDriverJar(String driverClassName, String driverJar) {
        List<EDatabase4DriverClassName> t4d = EDatabase4DriverClassName.indexOfByDriverClass(driverClassName);
        if (t4d.size() == 1) {
            return t4d.get(0).getDbTypeName();
        } else if (t4d.size() > 1) {
            // for some dbs use the same driverClassName.
            if (driverJar == null || "".equals(driverJar) || !driverJar.contains(".jar")) {
                return t4d.get(0).getDbTypeName();
            } else if (driverJar.contains("postgresql-8.3-603.jdbc3.jar") || driverJar.contains("postgresql-8.3-603.jdbc4.jar")
                    || driverJar.contains("postgresql-8.3-603.jdbc2.jar")) {
                return EDatabase4DriverClassName.PSQL.getDbTypeName();
            } else {
                return t4d.get(0).getDbTypeName(); // first default
            }
        }
        return null;
    }

}
