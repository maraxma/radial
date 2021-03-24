package com.newegg.mkpl.radial.mybatis;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.SqlxmlTypeHandler;

/**
 * Unicode编码的XML类型处理器（用于向数据库服务器传送需要Unicode编码的XML数据）。
 *
 * @author mm92
 * @since 1.1.7 2019-03-06
 */
public class UnicodeSqlXmlTypeHandler extends SqlxmlTypeHandler {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setNString(i, parameter);
    }

}
