package com.andy.sqltodsl.utils;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.google.inject.internal.util.Preconditions;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class SqlUtils {

    private static final String FIELD = "field";

    private static final String VALUE = "value";

    private static final String OPERATOR = "operator";

    public static void main(String[] args) {
    }


    public static MySqlSchemaStatVisitor getVisitor(String sql){
        //获取visitor
        MySqlStatementParser parser = new MySqlStatementParser(sql);
        SQLStatement sqlStatement = parser.parseStatement();
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        sqlStatement.accept(visitor);
        return visitor;
    }


    /**
     * 将sql的查询语句去除空格存入set
     * @author Andy
     * @date 2022/11/14 22:31
     * @param sql 待解析sql
     * @return conditions set
     **/
    public static List<String> parseQueryConditions(String sql){
        List<String> resultSet = new ArrayList<>();
        MySqlSchemaStatVisitor visitor = getVisitor(sql);
        List<String> tableNameList = getTableNameList(visitor);
        for (TableStat.Condition con : visitor.getConditions()) {
            Class<?> aClass = con.getValues().get(0).getClass();
            String fullName = getFieldName(tableNameList, con);
            if (Objects.equals(aClass.getName(), "java.lang.String")) {
                resultSet.add(fullName+ con.getOperator() +  "'" + con.getValues().get(0) + "'");
            }else{
                resultSet.add(fullName + con.getOperator() + con.getValues().get(0));
            }
        }
        return resultSet;
    }

    private static String getFieldName(List<String> tableNameList, TableStat.Condition con) {
        //单表查询, 普通字段:tableName.fieldName nested字段:field_name.field_name
        String fullName = con.getColumn().getFullName();
        String[] nameArray = fullName.split("\\.");
        if (Objects.equals(nameArray[0], tableNameList.get(0)) && nameArray.length > 1){
            //等于表名, 则去除
            fullName = nameArray[1];
        }
        return fullName;
    }


    /**
    *解析查询条件为MapList
    *@author Andy
    *@param sql 查询语句
    *@return mapList
    *@date 2022/11/15
    */
    public static List<Map<String, Object>> parseQueryConditionsToMapList(String sql){
        List<Map<String, Object>> resultList = new ArrayList<>();
        Map<String, Object> dataMap = null;
        MySqlSchemaStatVisitor visitor = getVisitor(sql);
        List<String> tableNameList = getTableNameList(visitor);
        for (TableStat.Condition con : visitor.getConditions()) {
            Class<?> aClass = con.getValues().get(0).getClass();
            dataMap = new HashMap<>();
            dataMap.put(FIELD, getFieldName(tableNameList, con));
            dataMap.put(VALUE, Objects.equals(aClass.getName(), "java.lang.String") ? "'" + con.getValues().get(0) + "'" : con.getValues().get(0));
            dataMap.put(OPERATOR, con.getOperator());
            resultList.add(dataMap);
        }
        return resultList;
    }



    /**
    *获得sql中的查询表名
    *@author Andy
    *@param visitor 查询器
    *@return 表名List
    *@date 2022/11/15
    */
    public static List<String> getTableNameList(MySqlSchemaStatVisitor visitor){
        List<String> resultList = new ArrayList<>();
        for (TableStat.Name key : visitor.getTables().keySet()) {
            resultList.add(key.getName());
        }
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(resultList), "未解析到表名");
        return resultList;
    }


    /**
    *获取where完整语句, 只考虑单表单where
    *@author Andy
    *@param sql sql语句
    *@return 完整的where语句
    *@date 2022/11/15
    */
    public static String getWhereStatement(String sql){
        int index = sql.toUpperCase(Locale.ROOT).indexOf("WHERE");
        return sql.substring(index + "WHERE".length());
    }
}