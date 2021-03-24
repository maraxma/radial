package org.maraxma.radial.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * 可切换数据源事务管理器。
 *
 * @author mm92
 * @since 1.1.8 2019-03-09
 */
public class SwitchableDataSourceTransactionManager extends DataSourceTransactionManager {

    private static final long serialVersionUID = 2345464149650165126L;
    private static final Log LOGGER = LogFactory.getLog(SwitchableDataSourceTransactionManager.class);

    public SwitchableDataSourceTransactionManager(DataSource dataSource) {
        super(dataSource);
        LOGGER.info("Transaction Manager for SwitchableDataSource is initialized");
    }

    @Override
    protected Object doGetTransaction() {
        // 在我们的实现中不使用这个方法，但是这个方法的执行结果会被很多地方引用
        // 有的地方甚至用这个方法的结果判定是否存在事务，因此这个方法虽然不用，但不能直接返回null
        return new Object();
    }

    @Override
    protected boolean isExistingTransaction(Object transaction) {
        return SwitchableDataSourceConnectionContextHolder.isTransactional();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        // 在这儿做一些准备工作
        // 与原有实现不相同的是，在这个方法里我们不做获取Connection的操作
        // 否则可能在初始化的时候获取到错误的连接
        SwitchableDataSourceConnectionContextHolder.setTransactionMetaData(new SwitchableDataSourceTransactionMetaData(System.currentTimeMillis()));
        SwitchableDataSourceConnectionContextHolder.setTransactional(true);
        SwitchableDataSourceConnectionContextHolder.setTransactionDefinition(definition);
        // 这儿无法对连接进行预设定因为这里还无法获得连接，获得连接的操作是运行时决定的
        // 连接的一些设定被移到doCommit方法中，因为在那里获得的连接才是整个事务块得到的连接
    }

    @Override
    protected Object doSuspend(Object transaction) {
        throw new TransactionSuspensionNotSupportedException("SwitchableDataSource transaction does not support suspend");
    }

    @Override
    protected void doResume(Object transaction, Object suspendedResources) {
        throw new TransactionSuspensionNotSupportedException("SwitchableDataSource transaction does not support resume");
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
        List<SwitchableDataSourceConnectionHolder> stxObjects = SwitchableDataSourceConnectionContextHolder.getTransactionalConnectionsInternal();
        TransactionDefinition transactionDefinition = SwitchableDataSourceConnectionContextHolder.getTransactionDefinition();
        List<Exception> exceptions = new ArrayList<>();
        LOGGER.debug("Rolling back, number of connection(s): " + stxObjects.size());
        for (SwitchableDataSourceConnectionHolder stxObject : stxObjects) {
            try {
                // Check if the connection is closed
                if (stxObject.getConnection().isClosed()) {
                    LOGGER.warn("This connection is closed while SwitchableDataSourceTransactionManager is not rollback yet: " + stxObject.getDataSourceName() + " --> " + stxObject.getConnection());
                    continue;
                }
                prepareTransaction(stxObject, transactionDefinition);
                stxObject.getConnection().rollback();
            } catch (Exception e) {
                LOGGER.error("Cannot rollback one of the connections, will rollback others", e);
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            throw new TransactionSystemException("One or more exception(s) occurred when rolling back: " + exceptions.size(), exceptions.get(0));
        }
        SwitchableDataSourceConnectionContextHolder.getInternalTransactionMetadata().setEndTime(System.currentTimeMillis());
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
        List<SwitchableDataSourceConnectionHolder> stxObjects = SwitchableDataSourceConnectionContextHolder.getTransactionalConnectionsInternal();
        TransactionDefinition transactionDefinition = SwitchableDataSourceConnectionContextHolder.getTransactionDefinition();
        LOGGER.debug("Committing to database, number of connection(s): " + stxObjects.size());
        try {
            for (SwitchableDataSourceConnectionHolder stxObject : stxObjects) {

                // Check if the connection is closed
                if (stxObject.getConnection().isClosed()) {
                    LOGGER.warn("This connection is closed while SwitchableDataSourceTransactionManager is not commit yet: " + stxObject.getDataSourceName() + " --> " + stxObject.getConnection());
                    continue;
                }
                prepareTransaction(stxObject, transactionDefinition);
                stxObject.getConnection().commit();
            }
        } catch (Exception e) {
            LOGGER.error("Cannot commit one of the connections, rollback all", e);
            doRollback(status);
        }
        SwitchableDataSourceConnectionContextHolder.getInternalTransactionMetadata().setEndTime(System.currentTimeMillis());
    }

    @Override
    protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        List<SwitchableDataSourceConnectionHolder> stxObjects = SwitchableDataSourceConnectionContextHolder.getTransactionalConnectionsInternal();
        for (SwitchableDataSourceConnectionHolder stxObject : stxObjects) {
            stxObject.setRollbackOnly();
        }
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        // 完成后做一些清理工作（清理资源、关闭连接、恢复readOnly标记、恢复Isolation、恢复自动提交设定）
        List<SwitchableDataSourceConnectionHolder> stxObjects = SwitchableDataSourceConnectionContextHolder.getTransactionalConnectionsInternal();
        for (SwitchableDataSourceConnectionHolder stxObject : stxObjects) {
            Connection conn = stxObject.getConnection();
            try {
                if (stxObject.isMustRestoreAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                DataSourceUtils.resetConnectionAfterTransaction(conn, stxObject.getPreviousIsolationLevel(), conn.isReadOnly());
            } catch (Throwable ex) {
                logger.debug("Could not reset JDBC Connection after transaction", ex);
            }
            DataSourceUtils.releaseConnection(conn, obtainDataSource());
        }
        LOGGER.debug("Closing connection(s): " + stxObjects.size());
        // 清除已经保存的连接
        SwitchableDataSourceConnectionContextHolder.getTransactionalConnectionsInternal().clear();
        // 关闭上下文中的事务标记
        // 这里必须这样做，因为若发生事务块和非事务块在一个方法里的时候，全局的标记会影响后面非事务块的提交操作，导致不能提交
        SwitchableDataSourceConnectionContextHolder.setTransactional(false);
        // 可以不删除事务附加信息，因为这部分信息会在同一线程的下一次事务中被重写
        // 不删除的好处是在事务已经结束的时候还可以获得事务的一些信息
        // SwitchableDataSourceConnectionContextHolder.setTransactionMetadata(null);
    }

    private void prepareTransaction(SwitchableDataSourceConnectionHolder stxObject, TransactionDefinition transactionDefinition) throws SQLException {
        if (!stxObject.isSettingsReady()) {
            // 准备一系列的设定
            // 保存先前的隔离级别
            Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(stxObject.getConnection(), transactionDefinition);
            stxObject.setPreviousIsolationLevel(previousIsolationLevel);

            // 设定自动提交并保存修改标记，这个标记决定了后面需不需要将自动提交复原
            if (stxObject.getConnection().getAutoCommit()) {
                stxObject.getConnection().setAutoCommit(false);
                stxObject.setMustRestoreAutoCommit(true);
            }

            // 设置ReadOnly
            if (isEnforceReadOnly() && transactionDefinition.isReadOnly()) {
                try (Statement stmt = stxObject.getConnection().createStatement()) {
                    stmt.executeUpdate("SET TRANSACTION READ ONLY");
                }
            }

            // 设定Read Only
            prepareTransactionalConnection(stxObject.getConnection(), transactionDefinition);

            // 设定超时
            int timeout = determineTimeout(transactionDefinition);
            if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
                stxObject.setTimeoutInSeconds(timeout);
            }
        }
    }
}
