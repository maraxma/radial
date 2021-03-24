package org.maraxma.radial.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;

import org.maraxma.radial.datasource.DataSourceContextHolder;
import org.maraxma.radial.datasource.SwitchableDataSource;

/**
 * 可切换数据源事务。
 * <p>注意：这个事务仅可使用在可切换数据源环境下。</p>
 * <p>可切换数据源事务负责在这个类中管理打开的所有连接。</p>
 *
 * @author mm92
 * @since 1.1.8 2019-03-09
 */
public class SwitchableDataSourceTransaction implements Transaction {

    private static final Log LOGGER = LogFactory.getLog(SwitchableDataSourceTransaction.class);

    private final DataSource dataSource;
    private volatile SwitchableDataSourceConnectionHolder connectionHolder;

    public SwitchableDataSourceTransaction(DataSource dataSource) {
        if (dataSource instanceof SwitchableDataSource) {
            this.dataSource = dataSource;
        } else {
            throw new IllegalArgumentException("The datasource is not SwitchableDataSource: " + dataSource);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        // 每一次获取连接将连接放入保持器中，若一个事务块中有相同的实例连接请求，则合并为一个连接返回，不再重开连接
        String currentDataSourceName = null;
        try {
            // 这里要同步，在执行下面语句的时候不允许在中途切换数据源
            synchronized (DataSourceContextHolder.class) {
                currentDataSourceName = DataSourceContextHolder.getDataSourceName();
                currentDataSourceName = currentDataSourceName == null ? String.valueOf(((SwitchableDataSource) dataSource).getDefaultDataSourceName()) : currentDataSourceName;
                // 若在一个事务块里执行的操作均是针对一个数据库实例的包含相同的一行或多行数据操作的，
                // 位于前面的操作未提交之前，后面的操作会一直等待，这在事务中是无法忍受的
                // 这个的作用是将相同的操作合并为一个事务防止一个操作未完成导致另一个操作死锁
                // 合并相同的DataSource下取得的连接，因为他们可以将各个操作合并在一个Connection中提交，这样就由可以使用单一的连接完美实现事务
                if (SwitchableDataSourceConnectionContextHolder.isTransactional()) {
                    // 当前上下文是事务性的那么需要对相同目标的连接进行合并
                    List<SwitchableDataSourceConnectionHolder> conns = SwitchableDataSourceConnectionContextHolder.getTransactionalConnectionsInternal();
                    
                    if (!prepareConnection(conns, currentDataSourceName)) {
                        // 准备连接并判定连接是否存在
                        // 如果连接不存在
                        // 将连接保存至上下文
                        // 稍后会在事务结束的时候挨个提交这批连接
                        SwitchableDataSourceConnectionContextHolder.addTransactionalConnections(connectionHolder);
                    } else {
                        // 如果存在连接则不管，connectionHolder已经在prepareConnection()方法中被赋值了
                    }
                    // 添加更多的信息
                    TransactionDefinition td = SwitchableDataSourceConnectionContextHolder.getTransactionDefinition();
                    if (td != null) {
                        if (td.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
                            // 当不为默认timeout的时候才设置过期时间
                            connectionHolder.setTimeoutInSeconds(td.getTimeout());
                        }
                        connectionHolder.setSynchronizedWithTransaction(true);
                    }
                } else {
                    // 若当前上下文并非事务性的，那么各获取各的连接，各提交各的
                    // 这时直接返回连接即可，这个连接会被Transaction实例管理，在合适的时机实现提交
                    // 2019-07-20新的需求需要记录非事务状态下的连接以供hydra使用
                    // 2020-03-04 Fix: Connection leak when use PageHelper(One session execute 2 or more SQLs)
                    if (!prepareConnection(SwitchableDataSourceConnectionContextHolder.getNonTransactionalConnectionsInternal(), currentDataSourceName)) {
                        SwitchableDataSourceConnectionContextHolder.addNonTransactionalConnections(connectionHolder);
                    }
                }
                return connectionHolder.getConnection();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot acquire connection from dataSource [" + dataSource + "] with name [" + currentDataSourceName + "]", e);
        }
    }

    @Override
    public void commit() throws SQLException {
        if (canCommitOrRollback(connectionHolder)) {
            try {
                connectionHolder.getConnection().commit();
            } catch (Exception e) {
                // 为异常附加当前使用的数据源名称
                throw new SQLException("Something wrong with your DataSource [" + connectionHolder.getDataSourceName() + "]", e);
            }
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (canCommitOrRollback(connectionHolder)) {
            try {
                connectionHolder.getConnection().rollback();
            } catch (Exception e) {
                // 为异常附加当前使用的数据源名称
                throw new SQLException("Something wrong with your DataSource [" + connectionHolder.getDataSourceName() + "]", e);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        // 当当前上下文事务不是事务性的才允许关闭连接，否则关闭连接的操作应该交与事务管理器来处理
        try {
            if (connectionHolder != null) { // fix: NPE，有些情况下MyBatis在未解析成功Mapper的时候也会调用close方法来确保连接被关闭
                if (!SwitchableDataSourceConnectionContextHolder.isTransactional()) {
                    // 释放连接
                    DataSourceUtils.doCloseConnection(connectionHolder.getConnection(), dataSource);
                    // 将连接从列表中移除
                    SwitchableDataSourceConnectionContextHolder.getNonTransactionalConnectionsInternal().remove(connectionHolder);
                }
            }
        } catch (Exception e) {
            throw new SQLException("Something wrong with your DataSource [" + connectionHolder.getDataSourceName() + "]", e);
        }
    }

    @Override
    public Integer getTimeout() throws SQLException {
        if (connectionHolder != null && connectionHolder.hasTimeout()) {
            return connectionHolder.getTimeToLiveInSeconds();
        }
        return null;
    }

    /**
     * 获知一个连接是够可以在这里提交或者回滚。
     * <p>这个方法主要是取当前上下文的事务状态来决定是否应该将提交和回滚交与其他事务管理器处理。</p>
     *
     * @param switchableDataSourceConnectionHolder 连接保持器
     * @return 若可以在这里提交或者回滚，那么返回true，否则返回false
     */
    protected boolean canCommitOrRollback(SwitchableDataSourceConnectionHolder switchableDataSourceConnectionHolder) {
        // Objects.requireNonNull(switchableDataSourceConnectionHolder, "Unexpected exception, the switchableDataSourceConnectionHolder is null, please contact the framework's administrator");
        if (switchableDataSourceConnectionHolder == null) {
            return false;
        }
        try {
            return switchableDataSourceConnectionHolder.getConnection() != null && // 连接不为null
                    !switchableDataSourceConnectionHolder.getConnection().getAutoCommit() && // 自动提交已关闭
                    !SwitchableDataSourceConnectionContextHolder.isTransactional(); // 当前上下文不是事务性的
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot acquire the connection's status: " + switchableDataSourceConnectionHolder.getConnection(), e);
        }
    }

    /**
     * 获得连接并就此创建一个新的保持器。
     * @param dataSourceName 数据源名称
     */
    private SwitchableDataSourceConnectionHolder newConnectionHolder(String dataSourceName) {
        long getConnectionStartTime = System.currentTimeMillis();
        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot acquire connection from dataSource [" + dataSource + "] with name [" + dataSourceName + "]", e);
        }
        long timeCosts = System.currentTimeMillis() - getConnectionStartTime;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Acquire connection from datasource '%s' spent %dms", dataSourceName, timeCosts));
        }
        return new SwitchableDataSourceConnectionHolder(dataSourceName, connection, timeCosts);
    }
    
    /**
     * 准备连接。这个方法会判定连接是否存在并打算重新使用连接，若连接不存在则新建一个连接。
     * <p>
     * @param existsConnections 已经存在的连接列表
     * @param dataSourceName 新建连接的数据源名称
     * @return 连接是否已经存在
     */
    private boolean prepareConnection(List<SwitchableDataSourceConnectionHolder> existsConnections, String dataSourceName) {    	
        for (SwitchableDataSourceConnectionHolder connInfo : existsConnections) {
            if (connInfo.getDataSourceName().equals(dataSourceName)) {
                connectionHolder = connInfo;
                Connection existingConn = connInfo.getConnection();
                try {
					if (existingConn.isClosed()) {
						throw new SQLException("Connection is closed");
					}
				} catch (SQLException e) {
					// 2020-10-21 Fix：如果连接出现问题，已被缓存的Connection得不到更新，导致程序无法从连接异常中恢复
					// 如果这步出现问题，那么证明连接已经坏掉了
					// 用新生成的ConnectionHolder替换原有ConnectionHolder
					try {
						// 尝试将废弃的连接关闭
						// 如果是池化的数据源实现，连接调用close后应该被回收或者销毁
						existingConn.close();
					} catch (Exception e2) {
						// ignore
					}
					SwitchableDataSourceConnectionHolder newConnHolder = newConnectionHolder(dataSourceName);
					existsConnections.add(existsConnections.indexOf(connInfo), newConnHolder);
					connectionHolder = newConnHolder;
				}
                return true;
            }
        }
        connectionHolder = newConnectionHolder(dataSourceName);
        return false;
    }
}
