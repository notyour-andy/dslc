package com.andy.sqltodsl.utils;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.expr.SQLQueryExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.andy.sqltodsl.bean.models.TreeNode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
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


    /**
    *获得where语句表达式树
    *@author Andy
    *@date 2023/2/20
    */
    public static TreeNode getExprTreeByExpr(SQLBinaryOpExpr sqlExpr){
        TreeNode treeNode = new TreeNode();
        if (!Objects.isNull(sqlExpr)){
            //非空, 逻辑符号
            boolean logical = sqlExpr.getOperator().isLogical();
            if (logical){
                //逻辑符号型
                treeNode.setType(1);
                treeNode.setValue(sqlExpr.getOperator().getName());
                treeNode.setValType(1);
                treeNode.setLeft(getExprTreeByExpr((SQLBinaryOpExpr)sqlExpr.getLeft()));
                treeNode.setRight(getExprTreeByExpr((SQLBinaryOpExpr)sqlExpr.getRight()));
            }else{
                //数值型节点, 不用遍历
                //字段
                treeNode.setType(0);
                treeNode.setField(sqlExpr.getLeft().toString());
                treeNode.setOperator(sqlExpr.getOperator().getName());
                treeNode.setValue(sqlExpr.getRight().toString());
                treeNode.setValType(sqlExpr.getRight().toString().startsWith("'") ? 1 : 0);
            }
        }
        return treeNode;
    }

    public static List<String> getCondByWhere(SQLExpr sqlExpr){
        List<String> resultList = new ArrayList<>();
        if (!Objects.isNull(sqlExpr)){
            if (sqlExpr instanceof SQLBinaryOpExpr){
                SQLBinaryOpExpr opExpr = (SQLBinaryOpExpr) sqlExpr;
                SQLExpr left = opExpr.getLeft();
                SQLExpr right = opExpr.getRight();
                boolean logical = opExpr.getOperator().isLogical();
                if (logical){
                    //逻辑符号型
                    resultList.addAll(getCondByWhere(left));
                    resultList.addAll(getCondByWhere(right));
                }else{
                    resultList.add(opExpr.getRight().toString());
                }
            }else if (sqlExpr instanceof SQLInSubQueryExpr){
                MySqlSelectQueryBlock query = (MySqlSelectQueryBlock) ((SQLInSubQueryExpr) sqlExpr).getSubQuery().getQuery();
                resultList.addAll(getWhereValue(query));
            }
        }
        return resultList;
    }


    public static List<String> getCondByFrom(SQLTableSource tbSrc) {
        List<String> resultList = new ArrayList<>();
        if (!Objects.isNull(tbSrc)){
            if (tbSrc instanceof SQLSubqueryTableSource){
                MySqlSelectQueryBlock query = (MySqlSelectQueryBlock) ((SQLSubqueryTableSource) tbSrc).getSelect().getQuery();
//                //处理where
//                resultList.addAll(getCondByWhere(query.getWhere()));
//                //判断是否存在子查询
//                resultList.addAll(getCondByFrom(query.getFrom()));
            }
        }
        return resultList;
    }

    public static List<String> getCondBySelect(List<SQLSelectItem> selectList) {
        List<String> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(selectList)){
            for (SQLSelectItem sqlSelectItem : selectList) {
                    SQLExpr expr = sqlSelectItem.getExpr();
                    if (expr instanceof SQLQueryExpr){
                        MySqlSelectQueryBlock subQuery = (MySqlSelectQueryBlock) ((SQLQueryExpr) expr).subQuery.getQueryBlock();

                    }
                }
            }
        return resultList;
    }


    public static List<String> getWhereValue(MySqlSelectQueryBlock queryBlock){
        List<String> resultList = new ArrayList<>();
        //遍历where
        resultList.addAll(TreeUtils.getCondByWhere(queryBlock.getWhere()));
        //遍历from
        resultList.addAll(TreeUtils.getCondByFrom(queryBlock.getFrom()));
    }
}
