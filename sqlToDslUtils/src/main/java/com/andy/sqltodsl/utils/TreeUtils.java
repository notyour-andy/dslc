package com.andy.sqltodsl.utils;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Andy
 * @date 2023-02-02
 */
public class TreeUtils {

    private static final String FIELD = "field";

    private static final String VALUE = "value";

    private static final String OPERATOR = "operator";

    /**
     *构建指定pid下的节点树
     *@author Andy
     *@param pid  父节点id
     *@param array 全部实体数据
     *@date 2022/9/28
     */
    public static JSONArray getChildrenList(String pid, JSONArray array, String pidName, String idName, String childName) {
        List<JSONObject> childrenList = array.stream().filter(m -> Objects.equals(((JSONObject)m).getString(pidName), pid)).map(
                (m) -> {
                    ((JSONObject) m).put(childName, getChildrenList(((JSONObject) m).getString(idName), array, pidName, idName, childName));
                    return (JSONObject) m;
                }
        ).collect(Collectors.toList());
        JSONArray jsonArray = new JSONArray();
        if (CollectionUtils.isNotEmpty(childrenList)) {
            jsonArray.addAll(childrenList);
        }
        return jsonArray;
    }



    public static void getExprByTree(SQLBinaryOpExpr sqlExpr, List<String> resultList) {
        if (!Objects.isNull(sqlExpr)) {
            //判断操作符是否是逻辑符号
            boolean logical = sqlExpr.getOperator().isLogical();
            if (logical) {
                //是逻辑符号说明还不是表达式
                getExprByTree((SQLBinaryOpExpr) sqlExpr.getLeft(), resultList);
                getExprByTree((SQLBinaryOpExpr) sqlExpr.getRight(), resultList);
            } else {
                if (!Objects.isNull(sqlExpr.getOperator())) {
                    resultList.add(sqlExpr.toString().replace(" ", ""));
                }
            }
        }
    }

    public static void getExprMapByTree(SQLBinaryOpExpr sqlExpr, List<Map<String, Object>> resultList) {
        if (!Objects.isNull(sqlExpr)) {
            //判断操作符是否是逻辑符号
            boolean logical = sqlExpr.getOperator().isLogical();
            if (logical) {
                //是逻辑符号说明还不是表达式
                getExprMapByTree((SQLBinaryOpExpr) sqlExpr.getLeft(), resultList);
                getExprMapByTree((SQLBinaryOpExpr) sqlExpr.getRight(), resultList);
            } else {
                if (!Objects.isNull(sqlExpr.getOperator())) {
                    //为表达式
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put(FIELD, sqlExpr.getLeft().toString().trim());
                    resultMap.put(VALUE, sqlExpr.getRight().toString().trim());
                    resultMap.put(OPERATOR, sqlExpr.getOperator().getName());
                    resultList.add(resultMap);
                }
            }
        }
    }
}
