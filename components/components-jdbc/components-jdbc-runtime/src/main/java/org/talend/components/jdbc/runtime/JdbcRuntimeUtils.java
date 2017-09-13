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

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.jdbc.ComponentConstants;
import org.talend.components.jdbc.module.PreparedStatementTable;
import org.talend.components.jdbc.runtime.setting.AllSetting;
import org.talend.components.jdbc.runtime.setting.JdbcRuntimeSourceOrSinkDefault;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.ValidationResult.Result;
import org.talend.daikon.properties.ValidationResultMutable;

public class JdbcRuntimeUtils {

    /**
     * get the JDBC connection object by the runtime setting
     * 
     * @param setting
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public static Connection createConnection(AllSetting setting) throws ClassNotFoundException, SQLException {
        java.lang.Class.forName(setting.getDriverClass());
        return java.sql.DriverManager.getConnection(setting.getJdbcUrl(), setting.getUsername(), setting.getPassword());
    }

    public static Connection fetchConnectionFromContextOrCreateNew(AllSetting setting, RuntimeContainer runtime)
            throws ClassNotFoundException, SQLException {
        if (runtime != null) {
            String refComponentId = setting.getReferencedComponentId();
            Object existedConn = runtime.getComponentData(ComponentConstants.CONNECTION_KEY, refComponentId);
            if (existedConn == null) {
                throw new RuntimeException("Referenced component: " + refComponentId + " is not connected");
            }
            return (Connection) existedConn;
        }
    
        return createConnection(setting);
    }
    
    public static ValidationResult validate(RuntimeContainer runtime, JdbcRuntimeSourceOrSinkDefault ss) {
        ValidationResultMutable vr = new ValidationResultMutable();
        try {
            ss.initConnection(runtime);
        } catch (Exception ex) {
            vr.setStatus(Result.ERROR);
            vr.setMessage(ex.getMessage());
        }
        return vr;
    }
    
    /**
     * fill the prepared statement object
     * 
     * @param pstmt
     * @param indexs
     * @param types
     * @param values
     * @throws SQLException
     */
    public static void setPreparedStatement(final PreparedStatement pstmt, final List<Integer> indexs, final List<String> types,
            final List<Object> values) throws SQLException {
        for (int i = 0; i < indexs.size(); i++) {
            Integer index = indexs.get(i);
            PreparedStatementTable.Type type = PreparedStatementTable.Type.valueOf(types.get(i));
            Object value = values.get(i);

            switch (type) {
            case BigDecimal:
                pstmt.setBigDecimal(index, (BigDecimal) value);
                break;
            case Blob:
                pstmt.setBlob(index, (Blob) value);
                break;
            case Boolean:
                pstmt.setBoolean(index, (boolean) value);
                break;
            case Byte:
                pstmt.setByte(index, (byte) value);
                break;
            case Bytes:
                pstmt.setBytes(index, (byte[]) value);
                break;
            case Clob:
                pstmt.setClob(index, (Clob) value);
                break;
            case Date:
                pstmt.setTimestamp(index, new Timestamp(((Date) value).getTime()));
                break;
            case Double:
                pstmt.setDouble(index, (double) value);
                break;
            case Float:
                pstmt.setFloat(index, (float) value);
                break;
            case Int:
                pstmt.setInt(index, (int) value);
                break;
            case Long:
                pstmt.setLong(index, (long) value);
                break;
            case Object:
                pstmt.setObject(index, value);
                break;
            case Short:
                pstmt.setShort(index, (short) value);
                break;
            case String:
                pstmt.setString(index, (String) value);
                break;
            case Time:
                pstmt.setTime(index, (Time) value);
                break;
            case Null:
                pstmt.setNull(index, (int) value);
                break;
            default:
                pstmt.setString(index, (String) value);
                break;
            }
        }
    }
}
