package com.andy.sqltodsl.utils;

import com.andy.sqltodsl.bean.enums.Operator;
import com.andy.sqltodsl.bean.models.OrderColumnModel;
import com.andy.sqltodsl.bean.models.TreeNode;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;

/**
 * @author Andy
 * @date 2022-11-15
 */
public class ElasticSearchUtils {

    public static void main(String[] args) {
        String sql = "select * from text where a = '1' and b = '2' and c = '4' and d <= 5 or d > 6 order by f desc, g";
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(parseQuerySqlToDsl(sql));
        List<OrderColumnModel> orderColumnList = SqlUtils.getOrderColumnList(sql);
        if (CollectionUtils.isNotEmpty(orderColumnList)){
            for (OrderColumnModel model : orderColumnList) {
                searchSourceBuilder.sort(model.getName(), SortOrder.fromString(model.getOrderType()));
            }
        }
        System.out.println(searchSourceBuilder.toString());
    }


    public static BoolQueryBuilder parseQuerySqlToDsl(String sql){
        String expr = SqlUtils.getWhereStatement(sql).replace(" ", "");
        List<String> condList = SqlUtils.parseQueryConditions(sql);
        List<Map<String, Object>> mapList = SqlUtils.parseQueryConditionsToMapList(sql);
        String pattern = CommonUtils.getPattens(expr, condList);
        TreeNode tree = CommonUtils.makeExprTree(CommonUtils.transInfixToSuffixExpr(pattern), mapList);
        return transTreeToDsl(tree, QueryBuilders.boolQuery(),tree.getValue(), new ArrayList<>());
    }


    private static BoolQueryBuilder transTreeToDsl(TreeNode treeNode, BoolQueryBuilder boolQueryBuilder, String operator, List<RangeQueryBuilder> rangeQueryList){
        if (treeNode != null){
            if (Objects.equals(treeNode.type, 1)){
                //逻辑符号
                boolean isNew = isNew(operator, treeNode.value);
                if(!isNew) {
                    //未发生改变
                    transTreeToDsl(treeNode.left, boolQueryBuilder, treeNode.value, rangeQueryList);
                    transTreeToDsl(treeNode.right, boolQueryBuilder, treeNode.value, rangeQueryList);
                }else{
                    //发生改变，则使用新的bool
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    List<RangeQueryBuilder> rangeList = new ArrayList<>();
                    transTreeToDsl(treeNode.left, boolQuery, treeNode.value, rangeList);
                    transTreeToDsl(treeNode.right, boolQuery, treeNode.value, rangeList);
                    List<QueryBuilder> builderList = Objects.equals(operator, Operator.OR.getMark()) ? boolQueryBuilder.should() : boolQueryBuilder.must();
                    builderList.add(boolQuery);
                }
            }else{
                //表达式节点
                List<QueryBuilder> builderList = Objects.equals(operator, Operator.OR.getMark()) ? boolQueryBuilder.should() : boolQueryBuilder.must();
                treeNode.setValue(treeNode.getValue().replace("'", ""));
                //如果为或
                if (Objects.equals(treeNode.getOperator(), "=")) {
                    builderList.add(QueryBuilders.matchPhraseQuery(treeNode.getField(), treeNode.getValue()));
                }else {
                    //查询是否已存在range
                    Optional<RangeQueryBuilder> any = rangeQueryList.stream().filter(ele -> Objects.equals(ele.fieldName(), treeNode.getField())).findAny();
                    RangeQueryBuilder rangeQueryBuilder = null;
                    if (any.isPresent()){
                        rangeQueryBuilder = any.get();
                    }else{
                        rangeQueryBuilder = new RangeQueryBuilder(treeNode.getField());
                        rangeQueryList.add(rangeQueryBuilder);
                        builderList.add(rangeQueryBuilder);
                    }
                    if (Objects.equals(treeNode.getOperator(), ">=")) {
                        rangeQueryBuilder.gte(treeNode.getValue());
                    } else if (Objects.equals(treeNode.getOperator(), ">")) {
                        rangeQueryBuilder.gt(treeNode.getValue());
                    } else if (Objects.equals(treeNode.getOperator(), "<=")) {
                        rangeQueryBuilder.lte(treeNode.getValue());
                    } else if (Objects.equals(treeNode.getOperator(), "<")) {
                        rangeQueryBuilder.lt(treeNode.getValue());
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
