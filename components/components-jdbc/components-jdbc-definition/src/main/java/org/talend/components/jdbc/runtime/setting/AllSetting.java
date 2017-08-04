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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.common.avro.JDBCAvroRegistryInfluencer;
import org.talend.components.jdbc.module.AdditionalColumnsTable;
import org.talend.components.jdbc.module.DBTypes;
import org.talend.components.jdbc.module.SPParameterTable;
import org.talend.components.jdbc.tjdbcoutput.TJDBCOutputProperties.DataAction;

/**
 * All the runtime setting for JDBC components
 *
 */
// Maybe we need to split it?
public class AllSetting implements Serializable, JDBCAvroRegistryInfluencer {

    private static final long serialVersionUID = 8998606157752865371L;

    private String jdbcUrl;

    private List<String> driverPaths;

    private String driverClass;

    private String username;

    private String password;

    private String tablename;

    private String sql;

    private Boolean useCursor;

    private Integer cursor;

    private Boolean trimStringOrCharColumns;

    private Boolean useAutoCommit;

    private Boolean autocommit;

    private DataAction dataAction;

    private Boolean clearDataInTable;

    private Boolean dieOnError;

    private Integer commitEvery;

    private Boolean debug;

    private Boolean useBatch;

    private Integer batchSize;

    private Boolean closeConnection;

    private Boolean propagateQueryResultSet;

    private String useColumn;

    private Boolean usePreparedStatement;

    private List<Integer> indexs;

    private List<String> types;

    private List<Object> values;

    private String referencedComponentId;

    private Boolean readOnly = false;

    private ComponentProperties referencedComponentProperties;

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        if (jdbcUrl != null) {
            this.jdbcUrl = jdbcUrl.trim();
        } else {
            this.jdbcUrl = null;
        }
    }

    public List<String> getDriverPaths() {
        return emptyListIfNull(driverPaths);
    }
    
    private List emptyListIfNull(List list) {
        return list == null ? new ArrayList() : list;
    }

    // we have a bug in the upriver from tup part or tcomp, a table widget may return a string value but we expect a list in fact,
    // so this
    // method for avoid some NPE check or type cast exception
    private List wrap(Object expectedList) {
        if (expectedList != null && expectedList instanceof List) {
            return (List) expectedList;
        }

        return new ArrayList();
    }

    public void setDriverPaths(Object driverPaths) {
        this.driverPaths = wrap(driverPaths);
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {
        this.tablename = tablename;
    }

    public Boolean getUseCursor() {
        return useCursor && cursor != null;
    }

    public void setUseCursor(Boolean useCursor) {
        this.useCursor = useCursor;
    }

    public Integer getCursor() {
        return cursor;
    }

    public void setCursor(Integer cursor) {
        this.cursor = cursor;
    }

    public void setTrimStringOrCharColumns(Boolean trimStringOrCharColumns) {
        this.trimStringOrCharColumns = trimStringOrCharColumns;
    }

    public Boolean getUseAutoCommit() {
        return useAutoCommit;
    }

    public void setUseAutoCommit(Boolean useAutoCommit) {
        this.useAutoCommit = useAutoCommit;
    }

    public Boolean getAutocommit() {
        return autocommit;
    }

    public void setAutocommit(Boolean autocommit) {
        this.autocommit = autocommit;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public DataAction getDataAction() {
        return dataAction;
    }

    public void setDataAction(DataAction dataAction) {
        this.dataAction = dataAction;
    }

    public Boolean getClearDataInTable() {
        return clearDataInTable;
    }

    public void setClearDataInTable(Boolean clearDataInTable) {
        this.clearDataInTable = clearDataInTable;
    }

    public Boolean getDieOnError() {
        return dieOnError;
    }

    public void setDieOnError(Boolean dieOnError) {
        this.dieOnError = dieOnError;
    }

    public Integer getCommitEvery() {
        return commitEvery;
    }

    public void setCommitEvery(Integer commitEvery) {
        this.commitEvery = commitEvery;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public Boolean getUseBatch() {
        return useBatch;
    }

    public void setUseBatch(Boolean useBatch) {
        this.useBatch = useBatch;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Boolean getCloseConnection() {
        return closeConnection;
    }

    public void setCloseConnection(Boolean closeConnection) {
        this.closeConnection = closeConnection;
    }

    public Boolean getPropagateQueryResultSet() {
        return propagateQueryResultSet;
    }

    public void setPropagateQueryResultSet(Boolean propagateQueryResultSet) {
        this.propagateQueryResultSet = propagateQueryResultSet;
    }

    public String getUseColumn() {
        return useColumn;
    }

    public void setUseColumn(String useColumn) {
        this.useColumn = useColumn;
    }

    public Boolean getUsePreparedStatement() {
        return usePreparedStatement;
    }

    public void setUsePreparedStatement(Boolean usePreparedStatement) {
        this.usePreparedStatement = usePreparedStatement;
    }

    public List<Integer> getIndexs() {
        return emptyListIfNull(indexs);
    }

    public void setIndexs(Object indexs) {
        this.indexs = wrap(indexs);
    }

    public List<String> getTypes() {
        return emptyListIfNull(types);
    }

    public void setTypes(Object types) {
        this.types = wrap(types);
    }

    public List<Object> getValues() {
        return emptyListIfNull(values);
    }

    public void setValues(Object values) {
        this.values = wrap(values);
    }

    public String getReferencedComponentId() {
        return referencedComponentId;
    }

    public void setReferencedComponentId(String referencedComponentId) {
        this.referencedComponentId = referencedComponentId;
    }

    public ComponentProperties getReferencedComponentProperties() {
        return referencedComponentProperties;
    }

    public void setReferencedComponentProperties(ComponentProperties referencedComponentProperties) {
        this.referencedComponentProperties = referencedComponentProperties;
    }

    @Override
    public boolean trim() {
        return trimStringOrCharColumns;
    }

    // TODO this is a temp fix, need to remove it after the CommonUtils.getMainSchemaFromOutputConnector can work for datastore
    // and dataset. Better to find the schema by the connector, not this.
    private Schema schema;

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Boolean isReadOnly() {
        return readOnly;
    }

    private String spName;

    private boolean isFunction;

    private String returnResultIn;

    private List<String> schemaColumns;

    private List<SPParameterTable.ParameterType> parameterTypes;

    public String getSpName() {
        return spName;
    }

    public void setSpName(String spName) {
        this.spName = spName;
    }

    public String getReturnResultIn() {
        return returnResultIn;
    }

    public void setReturnResultIn(String returnResultIn) {
        this.returnResultIn = returnResultIn;
    }

    public List<String> getSchemaColumns() {
        return emptyListIfNull(schemaColumns);
    }

    public void setSchemaColumns(Object schemaColumns) {
        this.schemaColumns = wrap(schemaColumns);
    }

    public List<SPParameterTable.ParameterType> getParameterTypes() {
        return emptyListIfNull(parameterTypes);
    }

    public void setParameterTypes(Object parameterTypes) {
        this.parameterTypes = wrap(parameterTypes);
    }

    public boolean isFunction() {
        return isFunction;
    }

    public void setIsFunction(boolean isFunction) {
        this.isFunction = isFunction;
    }

    private Boolean shareConnection;

    private String sharedConnectionName;

    private Boolean useDataSource;

    private String dataSource;

    public Boolean getShareConnection() {
        return shareConnection;
    }

    public void setShareConnection(Boolean shareConnection) {
        this.shareConnection = shareConnection;
    }

    public String getSharedConnectionName() {
        return sharedConnectionName;
    }

    public void setSharedConnectionName(String sharedConnectionName) {
        this.sharedConnectionName = sharedConnectionName;
    }

    public Boolean getUseDataSource() {
        return useDataSource;
    }

    public void setUseDataSource(Boolean useDataSource) {
        this.useDataSource = useDataSource;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    private Boolean enableDBMapping;

    private DBTypes dbMapping;

    public Boolean getEnableDBMapping() {
        return enableDBMapping;
    }

    public void setEnableDBMapping(Boolean enableDBMapping) {
        this.enableDBMapping = enableDBMapping;
    }

    public DBTypes getDbMapping() {
        return dbMapping;
    }

    public void setDbMapping(DBTypes dbMapping) {
        this.dbMapping = dbMapping;
    }

    private Map<Integer, Boolean> trimMap;

    public void setTrims(Object trims) {
        List<Boolean> ts = wrap(trims);
        int index = 0;
        for (Boolean trim : ts) {
            Map<Integer, Boolean> trimMap = new HashMap<>();
            trimMap.put(++index, trim);
        }
    }

    @Override
    public boolean isTrim(int index) {
        if (trimMap == null || trimMap.isEmpty()) {
            return false;
        }

        return trimMap.get(index);
    }

    private List<String> newDBColumnNames4AdditionalParameters;

    private List<String> sqlExpressions4AdditionalParameters;

    private List<AdditionalColumnsTable.Position> positions4AdditionalParameters;

    private List<String> referenceColumns4AdditionalParameters;

    private Boolean enableFieldOptions;

    private List<String> schemaColumns4FieldOption;

    private List<Boolean> updateKey4FieldOption;

    private List<Boolean> deletionKey4FieldOption;

    private List<Boolean> updatable4FieldOption;

    private List<Boolean> insertable4FieldOption;

    public List<String> getNewDBColumnNames4AdditionalParameters() {
        return emptyListIfNull(newDBColumnNames4AdditionalParameters);
    }

    public void setNewDBColumnNames4AdditionalParameters(List<String> newDBColumnNames4AdditionalParameters) {
        this.newDBColumnNames4AdditionalParameters = wrap(newDBColumnNames4AdditionalParameters);
    }

    public List<String> getSqlExpressions4AdditionalParameters() {
        return emptyListIfNull(sqlExpressions4AdditionalParameters);
    }

    public void setSqlExpressions4AdditionalParameters(List<String> sqlExpressions4AdditionalParameters) {
        this.sqlExpressions4AdditionalParameters = wrap(sqlExpressions4AdditionalParameters);
    }

    public List<AdditionalColumnsTable.Position> getPositions4AdditionalParameters() {
        return emptyListIfNull(positions4AdditionalParameters);
    }

    public void setPositions4AdditionalParameters(List<AdditionalColumnsTable.Position> positions4AdditionalParameters) {
        this.positions4AdditionalParameters = wrap(positions4AdditionalParameters);
    }

    public List<String> getReferenceColumns4AdditionalParameters() {
        return emptyListIfNull(referenceColumns4AdditionalParameters);
    }

    public void setReferenceColumns4AdditionalParameters(List<String> referenceColumns4AdditionalParameters) {
        this.referenceColumns4AdditionalParameters = wrap(referenceColumns4AdditionalParameters);
    }

    public List<String> getSchemaColumns4FieldOption() {
        return emptyListIfNull(schemaColumns4FieldOption);
    }

    public void setSchemaColumns4FieldOption(List<String> schemaColumns4FieldOption) {
        this.schemaColumns4FieldOption = wrap(schemaColumns4FieldOption);
    }

    public List<Boolean> getUpdateKey4FieldOption() {
        return emptyListIfNull(updateKey4FieldOption);
    }

    public void setUpdateKey4FieldOption(List<Boolean> updateKey4FieldOption) {
        this.updateKey4FieldOption = wrap(updateKey4FieldOption);
    }

    public List<Boolean> getDeletionKey4FieldOption() {
        return emptyListIfNull(deletionKey4FieldOption);
    }

    public void setDeletionKey4FieldOption(List<Boolean> deletionKey4FieldOption) {
        this.deletionKey4FieldOption = wrap(deletionKey4FieldOption);
    }

    public List<Boolean> getUpdatable4FieldOption() {
        return emptyListIfNull(updatable4FieldOption);
    }

    public void setUpdatable4FieldOption(List<Boolean> updatable4FieldOption) {
        this.updatable4FieldOption = wrap(updatable4FieldOption);
    }

    public List<Boolean> getInsertable4FieldOption() {
        return emptyListIfNull(insertable4FieldOption);
    }

    public void setInsertable4FieldOption(List<Boolean> insertable4FieldOption) {
        this.insertable4FieldOption = wrap(insertable4FieldOption);
    }

    public boolean getEnableFieldOptions() {
        return enableFieldOptions != null && enableFieldOptions;
    }

    public void setEnableFieldOptions(Boolean enableFieldOptions) {
        this.enableFieldOptions = enableFieldOptions;
    }

}
