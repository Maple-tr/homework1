package com.library.util;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JDBCTool {

    /**
     * 方法1：ResultSet 转 List
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            // 使用 JDK 8+ 推荐的反射实例化方式
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (int i = 1; i <= columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);

                // 根据列名找对应的实体类字段（忽略大小写）
                Field field = getFieldByColumnName(clazz, columnLabel);
                if (field != null && value != null) {
                    field.setAccessible(true);
                    field.set(obj, convertValue(value, field.getType()));
                }
            }
            list.add(obj);
        }
        return list;
    }

    /**
     * 方法2：保存数据
     */
    public static <T> int save(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);

        // 1. 自动生成 UUID 主键
        Field idField = getIdField(clazz);
        if (idField != null) {
            idField.setAccessible(true);
            if (idField.get(obj) == null || idField.get(obj).toString().isEmpty()) {
                idField.set(obj, UUID.randomUUID().toString().replace("-", ""));
            }
        }

        // 2. 自动填充时间
        autoFillTime(obj, "createTime");
        autoFillTime(obj, "updateTime");

        List<Field> fields = new ArrayList<>();
        for (Field field : getAllFields(clazz)) {
            // 跳过 List 集合等非数据库字段，跳过 static 字段
            if (field.getType() == List.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            // 如果是复杂对象（比如嵌套的 User 或 Book），跳过（由业务层单独存外键）
            if (field.getType() == Object.class || field.getType().getName().startsWith("com.library.entity")) {
                continue;
            }
            field.setAccessible(true);
            fields.add(field);
        }

        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder placeholders = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            sql.append(fields.get(i).getName());
            placeholders.append("?");
            if (i < fields.size() - 1) {
                sql.append(", ");
                placeholders.append(", ");
            }
        }
        sql.append(") VALUES (").append(placeholders).append(")");

        PreparedStatement ps = connection.prepareStatement(sql.toString());
        for (int i = 0; i < fields.size(); i++) {
            ps.setObject(i + 1, fields.get(i).get(obj));
        }
        int rows = ps.executeUpdate();
        ps.close();
        return rows;
    }

    /**
     * 方法3：更新数据
     */
    public static <T> int update(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getIdField(clazz);
        if (idField == null) throw new RuntimeException("未找到主键字段");

        idField.setAccessible(true);
        Object idValue = idField.get(obj);
        if (idValue == null) throw new RuntimeException("主键值为空，无法更新");

        // 自动更新 updateTime
        autoFillTime(obj, "updateTime");

        List<Field> fields = new ArrayList<>();
        for (Field field : getAllFields(clazz)) {
            if (field.equals(idField)) continue;
            if (field.getType() == List.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType().getName().startsWith("com.library.entity")) continue;
            field.setAccessible(true);
            fields.add(field);
        }

        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
        for (int i = 0; i < fields.size(); i++) {
            sql.append(fields.get(i).getName()).append(" = ?");
            if (i < fields.size() - 1) sql.append(", ");
        }
        sql.append(" WHERE ").append(idField.getName()).append(" = ?");

        PreparedStatement ps = connection.prepareStatement(sql.toString());
        int index = 1;
        for (Field field : fields) {
            ps.setObject(index++, field.get(obj));
        }
        ps.setObject(index, idValue);

        int rows = ps.executeUpdate();
        ps.close();
        return rows;
    }

    /**
     * 方法4：删除数据
     */
    public static <T> int delete(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getIdField(clazz);
        if (idField == null) throw new RuntimeException("未找到主键字段");

        idField.setAccessible(true);
        Object idValue = idField.get(obj);
        if (idValue == null) throw new RuntimeException("主键值为空，无法删除");

        String sql = "DELETE FROM " + tableName + " WHERE " + idField.getName() + " = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setObject(1, idValue);
        int rows = ps.executeUpdate();
        ps.close();
        return rows;
    }

    /**
     * 方法5：根据 ID 查询单个对象
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) throws Exception {
        String tableName = getTableName(clazz);
        Field idField = getIdField(clazz);
        if (idField == null) throw new RuntimeException("未找到主键字段");

        String sql = "SELECT * FROM " + tableName + " WHERE " + idField.getName() + " = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setObject(1, id);
        ResultSet rs = ps.executeQuery();

        List<T> list = resultSetToList(rs, clazz);
        rs.close();
        ps.close();
        return list.isEmpty() ? null : list.get(0);
    }

    // ================= 以下为内部辅助方法 =================

    private static void autoFillTime(Object obj, String fieldName) throws Exception {
        try {
            Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
            if (field.getType() == LocalDateTime.class) {
                field.setAccessible(true);
                // 如果是更新操作（updateTime），或者是插入操作但时间为空
                if (fieldName.equals("updateTime") || field.get(obj) == null) {
                    field.set(obj, LocalDateTime.now());
                }
            }
        } catch (NoSuchFieldException ignored) {}
    }

    private static Field getFieldByColumnName(Class<?> clazz, String columnName) {
        for (Field field : getAllFields(clazz)) {
            // MySQL 默认列名忽略大小写，这里直接使用 equalsIgnoreCase
            if (field.getName().equalsIgnoreCase(columnName)) {
                return field;
            }
        }
        return null;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private static Field getIdField(Class<?> clazz) {
        for (Field field : getAllFields(clazz)) {
            if (field.getName().equalsIgnoreCase("id")) {
                return field;
            }
        }
        return null;
    }

    private static String getTableName(Class<?> clazz) {
        // 根据你的数据库截图，类名 User 对应的表名是 user，BookCopy 对应 bookcopy
        // 直接返回类名全小写即可
        return clazz.getSimpleName().toLowerCase();
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;

        if (targetType == String.class) return value.toString();

        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) return ((Number) value).intValue();
            return Integer.parseInt(value.toString());
        }

        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            return Double.parseDouble(value.toString());
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Boolean) return value;
            if (value instanceof byte[]) return ((byte[]) value)[0] != 0; // 兼容 MySQL bit(1) 返回 byte[]
            if (value instanceof Number) return ((Number) value).intValue() != 0;
            return Boolean.parseBoolean(value.toString());
        }

        // 处理 JDK 8+ 的时间类型
        if (targetType == LocalDateTime.class) {
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toLocalDateTime();
            }
        }

        if (targetType == LocalDate.class) {
            if (value instanceof Date) {
                return ((Date) value).toLocalDate();
            }
        }

        return value;
    }
}