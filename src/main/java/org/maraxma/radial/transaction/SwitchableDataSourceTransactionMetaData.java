package org.maraxma.radial.transaction;

import java.util.UUID;

/**
 * 包裹了一个事务执行时候记录的一些数据。
 *
 * @author mm92
 * @since 1.2.0 2019-07-17
 */
public class SwitchableDataSourceTransactionMetaData {

    private String transactionId = UUID.randomUUID().toString().replace("-", "");
    private long startTime;
    private long endTime;

    public SwitchableDataSourceTransactionMetaData(long startTime) {
        this.startTime = startTime;
    }

    public SwitchableDataSourceTransactionMetaData(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public SwitchableDataSourceTransactionMetaData() {
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

}
