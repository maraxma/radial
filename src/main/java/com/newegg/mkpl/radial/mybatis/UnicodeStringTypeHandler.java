package com.newegg.mkpl.radial.mybatis;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.StringTypeHandler;

/**
 * 对Unicode编码的字符串处理器。
 * <p>当你的数据库里字段类型为NCHAR、NVARCHAR或者是unicode字符串编码的类型的时候你需要使用此处理器，
 * 否则存入的字符串可能出现乱码。</p>
 *
 * @author mm92
 * @since 1.2.4 2019-06-05
 */
public class UnicodeStringTypeHandler extends StringTypeHandler {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setNString(i, parameter);
    }
}
