package org.maraxma.radial.transaction;

import java.sql.Connection;

import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.ConnectionHolder;

/**
 * 代表一个可切换数据源的连接保持器，它提供了一些关于连接的状态管理。
 *
 * @author mm92
 * @since 1.1.8 2019-03-13
 */
public class SwitchableDataSourceConnectionHolder extends ConnectionHolder {

    private String dataSourceName;
    private Integer previousIsolationLevel;
    private boolean settingsReady = false;
    private boolean mustRestoreAutoCommit;
    private long acquireConnectionTimeCosts;

    public SwitchableDataSourceConnectionHolder(String dataSourceName, Connection connection, boolean transactionActive) {
        super(connection, transactionActive);
        this.dataSourceName = dataSourceName;
    }

    public SwitchableDataSourceConnectionHolder(String dataSourceName, Connection connection) {
        super(connection);
        this.dataSourceName = dataSourceName;
    }

    public SwitchableDataSourceConnectionHolder(String dataSourceName, Connection connection, long acquireConnectionTimeCosts) {
        super(connection);
        this.dataSourceName = dataSourceName;
        this.acquireConnectionTimeCosts = acquireConnectionTimeCosts;
    }

    public SwitchableDataSourceConnectionHolder(String dataSourceName, ConnectionHandle connectionHandle) {
        super(connectionHandle);
        this.dataSourceName = dataSourceName;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public boolean isSettingsReady() {
        return settingsReady;
    }

    public void setSettingsReady(boolean settingsReady) {
        this.settingsReady = settingsReady;
    }

    public boolean isMustRestoreAutoCommit() {
        return mustRestoreAutoCommit;
    }

    public void setMustRestoreAutoCommit(boolean mustRestoreAutoCommit) {
        this.mustRestoreAutoCommit = mustRestoreAutoCommit;
    }

    public Integer getPreviousIsolationLevel() {
        return previousIsolationLevel;
    }

    public void setPreviousIsolationLevel(Integer previousIsolationLevel) {
        this.previousIsolationLevel = previousIsolationLevel;
    }

    public long getAcquireConnectionTimeCosts() {
        return acquireConnectionTimeCosts;
    }

    public void setAcquireConnectionTimeCosts(long acquireConnectionTimeCosts) {
        this.acquireConnectionTimeCosts = acquireConnectionTimeCosts;
    }

}
