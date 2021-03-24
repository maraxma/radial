package org.maraxma.radial.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;

/**
 * 可切换数据源事务工厂。
 *
 * @author mm92
 * @since 1.1.8 2019-03-09
 */
public class SwitchableDataSourceTransactionFactory extends SpringManagedTransactionFactory {

    @Override
    public void setProperties(Properties props) {

    }

    @Override
    public Transaction newTransaction(Connection conn) {
        throw new UnsupportedOperationException("Switchable transactions require a DataSource");
    }

    @Override
    public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        return new SwitchableDataSourceTransaction(dataSource);
    }

}
