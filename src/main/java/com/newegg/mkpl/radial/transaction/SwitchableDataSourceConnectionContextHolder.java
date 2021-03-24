package com.newegg.mkpl.radial.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.transaction.TransactionDefinition;

/**
 * 代表一个可切换数据源连接信息上下文保持器（ThreadLocal）。
 * <p>这个保持器里主要包含有连接、事务标记、事务的一些额外信息。</p>
 *
 * @author mm92
 * @since 1.1.8 2019-03-09
 */
public class SwitchableDataSourceConnectionContextHolder {
    private static final ThreadLocal<SwitchableDataSourceTransactionMetaData> TRANSACTION_METADATA = ThreadLocal.withInitial(() -> null);
    private static final ThreadLocal<List<SwitchableDataSourceConnectionHolder>> NON_TRANSACTIONAL_CONNECTION_HOLDER = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<SwitchableDataSourceConnectionHolder>> TRANSACTIONAL_CONNECTION_HOLDER = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Boolean> IS_TRANSACTIONAL = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<TransactionDefinition> TRANSACTION_DEFINITION = ThreadLocal.withInitial(() -> null);

    /**
     * 获得当前上下文中存在的连接信息（内部方法）。
     *
     * @return 连接信息列表，它们指代了当前上下文中的按顺序排列的连接列表（先建立的连接在最前面）。
     */
    static List<SwitchableDataSourceConnectionHolder> getTransactionalConnectionsInternal() {
        return TRANSACTIONAL_CONNECTION_HOLDER.get();
    }

    /**
     * 获得当前上下文中存在的连接信息。
     *
     * @return 连接信息列表，它们指代了当前上下文中的按顺序排列的连接列表（先建立的连接在最前面）。
     */
    public static List<SwitchableDataSourceConnectionHolder> getTransactionalConnections() {
        return Collections.unmodifiableList(TRANSACTIONAL_CONNECTION_HOLDER.get());
    }

    /**
     * 向当前上下文中添加一条连接。
     *
     * @param connectionHolders 连接保持器
     */
    static void addTransactionalConnections(SwitchableDataSourceConnectionHolder... connectionHolders) {
        for (SwitchableDataSourceConnectionHolder connectionHolder : connectionHolders) {
            TRANSACTIONAL_CONNECTION_HOLDER.get().add(connectionHolder);
        }
    }

    /**
     * 获得当前线程上下文中的某个连接。
     *
     * @param connectionIndex 连接在列表中的编号（从0开始）
     */
    static SwitchableDataSourceConnectionHolder getTransactionalConnection(int connectionIndex) {
        return TRANSACTIONAL_CONNECTION_HOLDER.get().get(connectionIndex);
    }

    static void setNonTransactionalConnections(List<SwitchableDataSourceConnectionHolder> connectionHolders) {
        NON_TRANSACTIONAL_CONNECTION_HOLDER.set(connectionHolders);
    }

    static void addNonTransactionalConnections(SwitchableDataSourceConnectionHolder... connectionHolders) {
        for (SwitchableDataSourceConnectionHolder connectionHolder : connectionHolders) {
            NON_TRANSACTIONAL_CONNECTION_HOLDER.get().add(connectionHolder);
        }
    }

    public static List<SwitchableDataSourceConnectionHolder> getNonTransactionalConnections() {
        return Collections.unmodifiableList(NON_TRANSACTIONAL_CONNECTION_HOLDER.get());
    }

    static List<SwitchableDataSourceConnectionHolder> getNonTransactionalConnectionsInternal() {
        return NON_TRANSACTIONAL_CONNECTION_HOLDER.get();
    }

    /**
     * 当前线程中的操作是否是事务性的。
     *
     * @return 若是则返回true，否则返回false
     */
    public static boolean isTransactional() {
        return IS_TRANSACTIONAL.get();
    }

    /**
     * 设定当前的事务性状态。
     *
     * @param isTransactional 是否是事务性的
     */
    static void setTransactional(boolean isTransactional) {
        IS_TRANSACTIONAL.set(isTransactional);
    }

    /**
     * 设置当前上下文中的事务定义。
     *
     * @param transactionDefinition 事务定义
     */
    static void setTransactionDefinition(TransactionDefinition transactionDefinition) {
        TRANSACTION_DEFINITION.set(transactionDefinition);
    }

    /**
     * 获得当前上下文中的事务定义。在没有事务的时候会返回null。
     *
     * @return 事务定义
     */
    public static TransactionDefinition getTransactionDefinition() {
        return TRANSACTION_DEFINITION.get();
    }

    /**
     * 获得可切换数据源事务附加数据。
     *
     * @return 附加数据
     */
    public static SwitchableDataSourceTransactionMetaData getTransactionMetadata() {
        if (TRANSACTION_METADATA.get() == null) {
            return null;
        } else {
            // return a copy to avoid user's modification
            SwitchableDataSourceTransactionMetaData metaData = new SwitchableDataSourceTransactionMetaData();
            BeanUtils.copyProperties(TRANSACTION_METADATA.get(), metaData);
            return metaData;
        }
    }

    /**
     * 设置可切换数据源事务附加数据。
     */
    static void setTransactionMetaData(SwitchableDataSourceTransactionMetaData metaData) {
        TRANSACTION_METADATA.set(metaData);
    }

    /**
     * 获得可切换数据源事务附加数据（内部使用，获得到的附加数据可修改）。
     *
     * @return 附加数据
     */
    static SwitchableDataSourceTransactionMetaData getInternalTransactionMetadata() {
        return TRANSACTION_METADATA.get();
    }
}