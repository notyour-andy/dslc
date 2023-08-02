package com.andy.sqltodsl.utils;

import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.andy.sqltodsl.bean.models.TreeNode;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Andy
 * @date 2023-02-02
 */
public class TreeUtils {

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

}
