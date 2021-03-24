package com.newegg.mkpl.radial.datasource;

import java.beans.ConstructorProperties;

/**
 * 池化数据源信息。
 *
 * @author mm92
 * @since 1.2.0 2019-03-19
 */
public class PooledDataSourceInfo {
    private final String type;
    private final boolean running;
    private final int openedConnections;
    private final int maxConnections;

    @ConstructorProperties({"type", "running", "openedConnections", "maxConnections"})
    public PooledDataSourceInfo(String type, boolean running, int openedConnections, int maxConnections) {
        super();
        this.type = type;
        this.running = running;
        this.openedConnections = openedConnections;
        this.maxConnections = maxConnections;
    }

    public String getType() {
        return type;
    }

    public boolean isRunning() {
        return running;
    }

    public int getOpenedConnections() {
        return openedConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

}
