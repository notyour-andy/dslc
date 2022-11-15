package com.andy.sqltodsl.utils;

import com.andy.sqltodsl.bean.enums.Operator;
import com.andy.sqltodsl.bean.models.TreeNode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Andy
 * @date 2022-11-15
 */
public class ElasticSearchUtils {


    public static String parseSqlToDsl(String sql){
        String expr = SqlUtils.getWhereStatement(sql).replace(" ", "");
        List<String> condList = SqlUtils.parseQueryConditions(sql);
        List<Map<String, Object>> mapList = SqlUtils.parseQueryConditionsToMapList(sql);
        String pattern = CommonUtils.getPattens(expr, condList);
        TreeNode tree = CommonUtils.makeExprTree(CommonUtils.transInfixToSuffixExpr(pattern), mapList);
        return transTreeToDsl(tree, QueryBuilders.boolQuery(),tree.getValue()).toString();
    }


    private static BoolQueryBuilder transTreeToDsl(TreeNode treeNode, BoolQueryBuilder boolQueryBuilder, String operator){
        if (treeNode != null){
            if (Objects.equals(treeNode.type, 1)){
                //逻辑符号
                boolean isNew = isNew(operator, treeNode.value);
                if(!isNew) {
                    //未发生改变
                    transTreeToDsl(treeNode.left, boolQueryBuilder, treeNode.value);
                    transTreeToDsl(treeNode.right, boolQueryBuilder, treeNode.value);
                }else{
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    if (Objects.equals(operator, Operator.OR.getMark())){
                        boolQueryBuilder.should().add(transTreeToDsl(treeNode.left, boolQuery, treeNode.value));
                        boolQueryBuilder.should().add(transTreeToDsl(treeNode.right, boolQuery, treeNode.value));
                    }else{
                        boolQueryBuilder.must().add(transTreeToDsl(treeNode.left, boolQuery, treeNode.value));
                        boolQueryBuilder.must().add(transTreeToDsl(treeNode.right, boolQuery, treeNode.value));
                    }
                }

            }else{
                //表达式节点
                if (Objects.equals(operator, Operator.OR.getMark())){
                    //如果为或
                    if (Objects.equals(treeNode.getOperator(), "and")) {
                        boolQueryBuilder.should().add(QueryBuilders.matchPhraseQuery(treeNode.getField(), treeNode.getValue().replace("'", "")));
                    }
                }else{
                    //为与
                    if (Objects.equals(treeNode.getOperator(), "=")) {
                        boolQueryBuilder.must().add(QueryBuilders.matchPhraseQuery(treeNode.getField(), treeNode.getValue().replace("'", "")));
                    }
                }
            }
        }
        return boolQueryBuilder;
    }

    private static boolean isNew(String initOp, String currentOp){
        //如果操作符部位null, 并且前后操作符不等，则使用新的bool查询
        return !Objects.isNull(initOp) && !Objects.equals(initOp, currentOp);
    }
}
