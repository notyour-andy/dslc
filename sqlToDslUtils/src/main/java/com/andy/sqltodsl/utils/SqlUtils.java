package com.andy.sqltodsl.utils;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.internal.util.ImmutableMap;
import com.google.inject.internal.util.Preconditions;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

public class SqlUtils {


    private static JSONObject TYPETEXT = JSONObject.parseObject("{\"type\":\"text\",\"fielddata\":true}");


    private final static Map<String,Object> SORT = ImmutableMap.of("type","keyword");

    private final static Map<String,Object> fields = ImmutableMap.of("sort",SORT);

//    private final static Map<String,Object> TYPEMAPWITHORDER = ImmutableMap.of("type","text","fields",fields,"fielddata",true);

    //    private final static Map<String,String> TYPELONGMAP = ImmutableMap.of("type","long");
    private static Map<String,String> TYPELONGMAP =
            new HashMap<String, String>() {
                {
                    put("type", "long");
                }
            };

    private static final String FIELD = "field";

    private static final String VALUE = "value";

    private static final String OPERATOR = "operator";

    public static void main(String[] args) {
        JSONArray jsonArray1 = JSONArray.parseArray("[{\"name\":\"标题\",\"key\":\"G_title\",\"type\":\"text\"},{\"name\":\"是否提交\",\"key\":\"G_isSubmit\",\"type\":\"text\"},{\"name\":\"基本信息_名字\",\"type\":\"text\",\"key\":\"P_getBaseInfo_name\"},{\"name\":\"基本信息_年龄\",\"type\":\"text\",\"key\":\"P_getBaseInfo_age\"},{\"name\":\"基本信息_出生年份\",\"type\":\"number\",\"key\":\"P_getBaseInfo_year\"},{\"name\":\"基本信息_多选\",\"key\":\"P_getBaseInfo_duoxuan\",\"type\":\"text\",\"children\":[{\"name\":\"值\",\"key\":\"value\",\"type\":\"text\"},{\"name\":\"标签\",\"key\":\"label\",\"type\":\"text\"}]},{\"name\":\"基本信息_范围\",\"type\":\"text\",\"key\":\"P_getBaseInfo_fanwei\"},{\"name\":\"基本信息_序号\",\"type\":\"text\",\"key\":\"P_getBaseInfo_xuhao\"},{\"name\":\"基本信息_上传pdf\",\"key\":\"P_getBaseInfo_pdf\",\"type\":\"text\",\"children\":[{\"name\":\"文件id\",\"key\":\"id\",\"type\":\"text\"},{\"name\":\"文件名称\",\"key\":\"name\",\"type\":\"text\"}]}]");
        String sql = "select * from z_test where a = '1' and b.d = '2' or (c = '3' or ( d = '4' and e = '5'))";
        List<String> dataSet = parseQueryConditions(sql);
//        System.out.println("dd");
//        String whereStatement = getWhereStatement(sql);
        System.out.println(dataSet);
        char c = '1';
        System.out.println((int) c);
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
            dataMap = new HashMap<>();
            Class<?> aClass = con.getValues().get(0).getClass();
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

    public static Map<String, Object> initMapNew(JSONArray array, Map<String,Object> propertiesMap){
        for (int i = 0; i < array.size(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            String key = jsonObject.getString("key");
            if (!jsonObject.containsKey("children")){
                //如果不含有children属性, 则为text类型
                String typeName = jsonObject.getString("type");
                if(Objects.equals(typeName,"text")){
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "text");
                    map.put("fielddata", true);
//                    BeanUtils.copyProperties(TYPETEXT, map);
//                    System.out.println(TYPETEXT);
//                    System.out.println(map);
                    propertiesMap.put(key, TYPETEXT);
                }else {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", "long");
                    propertiesMap.put(key, TYPELONGMAP);
                }
            }else{
                JSONArray children = jsonObject.getJSONArray("children");
//                JSONArray newChildren = JSONArray.parseArray(JSONObject.toJSONString(jsonObject.getJSONArray("children")));
                String typeName = jsonObject.getString("type");
                if (CollectionUtils.isNotEmpty(children)){
                    JSONObject json = new JSONObject();
                    json.put("type", "nested");
                    json.put("properties", initMapNew(children, new HashMap<>()));
                    propertiesMap.put(key, json);
                }else{
                    if(Objects.equals(typeName,"text")){
                        Map<String, Object> map = new HashMap<>();
                        map.put("type", "text");
                        map.put("fielddata", true);
                        propertiesMap.put(key, map);
                    }else {
                        Map<String, Object> map = new HashMap<>();
                        map.put("type", "long");
                        propertiesMap.put(key, map);
                    }
                }


            }
        }
        return propertiesMap;
    }
}
