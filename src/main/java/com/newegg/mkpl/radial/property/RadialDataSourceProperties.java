package com.newegg.mkpl.radial.property;

import java.util.Map;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

/**
 * 单一数据源配置承载器
 *
 * @author mm92
 * @since 1.0.0 2018-11-09
 */
public class RadialDataSourceProperties extends DataSourceProperties {

    private Boolean active;
    private Map<String, Object> hikari;
    private Map<String, Object> druid;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Map<String, Object> getHikari() {
        return hikari;
    }

    public void setHikari(Map<String, Object> hikari) {
        this.hikari = hikari;
    }

    public Map<String, Object> getDruid() {
        return druid;
    }

    public void setDruid(Map<String, Object> druid) {
        this.druid = druid;
    }

}
