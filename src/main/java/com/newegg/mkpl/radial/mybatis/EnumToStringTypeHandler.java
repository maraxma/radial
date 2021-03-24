package com.newegg.mkpl.radial.mybatis;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.ibatis.type.EnumTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * 枚举toString类型处理器。
 * <p>此处理器用于替代MyBatis自带的处理器，自带的处理器会将枚举转换为枚举值的字面形式(调用name方法)，
 * 而这个处理器会调用枚举实例的toString()方法,
 * 这样做的好处是用户可以自定义枚举的toString()方法以控制其最终存入数据库的形式。</p>
 *
 * @param <E> 枚举类型
 * @author mm92 Mara.X.Ma
 * @since v19.2.063.0 2019-09-26
 */
public class EnumToStringTypeHandler<E extends Enum<E>> extends EnumTypeHandler<E> {

    public EnumToStringTypeHandler(Class<E> type) {
        super(type);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        if (jdbcType == null) {
            ps.setString(i, parameter.toString());
        } else {
            ps.setObject(i, parameter.toString(), jdbcType.TYPE_CODE);
        }
    }
}
