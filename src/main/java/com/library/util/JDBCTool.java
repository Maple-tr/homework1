package com.library.util;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JDBCTool {

    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            T obj = clazz.getDeclaredConstructor().newInstance();
            for (int i = 1; i <= columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
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

    public static <T> int save(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);

        Field idField = getIdField(clazz);
        if (idField != null) {
            idField.setAccessible(true);
            if (idField.get(obj) == null || idField.get(obj).toString().isEmpty()) {
                idField.set(obj, UUID.randomUUID().toString().replace("-", ""));
            }
        }

        // 自动填充创建时间和更新时间
        autoFillTime(obj, "createTime");
        autoFillTime(obj, "updateTime");

        List<Field> fields = new ArrayList<>();
        for (Field field : getAllFields(clazz)) {
            if (field.getType() == List.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType() == Object.class || field.getType().getName().startsWith("com.library.entity")) continue;
            field.setAccessible(true);
            fields.add(field);
        }

        StringBuilder sql = new StringBuilder("INSERT INTO `" + tableName + "` (");
        StringBuilder placeholders = new StringBuilder();

        for (int i = 0; i < fields.size(); i++) {
            sql.append("`").append(fields.get(i).getName()).append("`");
            placeholders.append("?");
            if (i < fields.size() - 1) {
                sql.append(", ");
                placeholders.append(", ");
            }
        }
        sql.append(") VALUES (").append(placeholders).append(")");

        System.out.println("执行的SQL: " + sql.toString());

        PreparedStatement ps = connection.prepareStatement(sql.toString());
        for (int i = 0; i < fields.size(); i++) {
            ps.setObject(i + 1, fields.get(i).get(obj));
        }
        int rows = ps.executeUpdate();
        ps.close();
        return rows;
    }

    public static <T> int update(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getIdField(clazz);
        if (idField == null) throw new RuntimeException("未找到主键字段");

        idField.setAccessible(true);
        Object idValue = idField.get(obj);
        if (idValue == null) throw new RuntimeException("主键值为空，无法更新");

        autoFillTime(obj, "updateTime");

        List<Field> fields = new ArrayList<>();
        for (Field field : getAllFields(clazz)) {
            if (field.equals(idField)) continue;
            if (field.getType() == List.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType().getName().startsWith("com.library.entity")) continue;
            field.setAccessible(true);
            fields.add(field);
        }

        StringBuilder sql = new StringBuilder("UPDATE `" + tableName + "` SET ");
        for (int i = 0; i < fields.size(); i++) {
            sql.append("`").append(fields.get(i).getName()).append("` = ?");
            if (i < fields.size() - 1) sql.append(", ");
        }
        sql.append(" WHERE `").append(idField.getName()).append("` = ?");

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

    public static <T> int delete(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        String tableName = getTableName(clazz);
        Field idField = getIdField(clazz);
        if (idField == null) throw new RuntimeException("未找到主键字段");

        idField.setAccessible(true);
        Object idValue = idField.get(obj);

        String sql = "DELETE FROM `" + tableName + "` WHERE `" + idField.getName() + "` = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setObject(1, idValue);
        int rows = ps.executeUpdate();
        ps.close();
        return rows;
    }

    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) throws Exception {
        String tableName = getTableName(clazz);
        Field idField = getIdField(clazz);
        if (idField == null) throw new RuntimeException("未找到主键字段");

        String sql = "SELECT * FROM `" + tableName + "` WHERE `" + idField.getName() + "` = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setObject(1, id);
        ResultSet rs = ps.executeQuery();

        List<T> list = resultSetToList(rs, clazz);
        rs.close();
        ps.close();
        return list.isEmpty() ? null : list.get(0);
    }

    // === 修复：正确查找 Base 父类里的时间字段 ===
    private static void autoFillTime(Object obj, String fieldName) throws Exception {
        Field field = getFieldByColumnName(obj.getClass(), fieldName);
        if (field != null && field.getType() == LocalDateTime.class) {
            field.setAccessible(true);
            if (fieldName.equals("updateTime") || field.get(obj) == null) {
                field.set(obj, LocalDateTime.now());
            }
        }
    }

    private static Field getFieldByColumnName(Class<?> clazz, String columnName) {
        for (Field field : getAllFields(clazz)) {
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
        String className = clazz.getSimpleName();
        if ("Admin".equals(className) || "Operator".equals(className) ||
                "Student".equals(className) || "Teacher".equals(className)) {
            return "user";
        }
        return className.toLowerCase();
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
            if (value instanceof byte[]) return ((byte[]) value)[0] != 0;
            if (value instanceof Number) return ((Number) value).intValue() != 0;
            return Boolean.parseBoolean(value.toString());
        }
        if (targetType == LocalDateTime.class) {
            if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        }
        if (targetType == LocalDate.class) {
            if (value instanceof Date) return ((Date) value).toLocalDate();
        }
        return value;
    }
}