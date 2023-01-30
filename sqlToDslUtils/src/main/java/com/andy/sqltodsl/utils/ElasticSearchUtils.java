package com.andy.sqltodsl.utils;

import com.andy.sqltodsl.bean.enums.Operator;
import com.andy.sqltodsl.bean.factory.QueryBuilderFactory;
import com.andy.sqltodsl.bean.models.OrderColumnModel;
import com.andy.sqltodsl.bean.models.TreeNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;

/**
 * @author Andy
 * @date 2022-11-15
 */
public class ElasticSearchUtils {

    public static void main(String[] args) {
        String sql = "select * from text where id = 123 and time > 1 and time < 3 or state = 0 group by time, state, c limit 10";
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //解析where语句
        parseWhere(sql, searchSourceBuilder);
        //order
        parseOrderBy(sql, searchSourceBuilder);
        //limit
        parseLimit(sql, searchSourceBuilder);
        //group by
        parseGroupBy(sql, searchSourceBuilder);
        System.out.println(searchSourceBuilder);
    }

    /**
     * 解析group by语句
     * @author Andy
     * @date 2023/1/19 22:03
     * @param sql sql语句
     * @param searchSourceBuilder dsl语句
     **/
    private static void parseGroupBy(String sql, SearchSourceBuilder searchSourceBuilder) {
        //获取group的字段
        List<String> fieldList = SqlUtils.getGroupByFieldList(sql);
        if (CollectionUtils.isNotEmpty(fieldList)){
            TermsAggregationBuilder last = null;
            TermsAggregationBuilder first = null;
            for (String field : fieldList) {
                TermsAggregationBuilder aggBuilder = AggregationBuilders.terms("groupBy" + field).field(field);
                if (!Objects.isNull(last)){
                    //如果last非空, 则说明是子聚合
                    last.subAggregation(aggBuilder);
                }
                if (fieldList.indexOf(field) == 0){
                    //如果是列表第一个字段, 则是最顶层聚合
                    first = aggBuilder;
                }
                last = aggBuilder;
            }
            searchSourceBuilder.aggregation(first);
        }
    }

    /**
     * 解析limit语句
     * @author Andy
     * @date 2023/1/19 21:45
     * @param sql 语句
     * @param searchSourceBuilder dsl语句
     **/
    private static void parseLimit(String sql, SearchSourceBuilder searchSourceBuilder) {
        Map<String, Object> argMap = SqlUtils.getLimitArgMap(sql);
        if (MapUtils.isNotEmpty(argMap)){
            searchSourceBuilder.from(MapUtils.getInteger(argMap, "from"));
            searchSourceBuilder.size(MapUtils.getInteger(argMap, "size"));
        }
    }


    /**
     * 解析order by语句
     * @author Andy
     * @date 2023/1/19 21:32
     * @param sql sql语句
     * @param searchSourceBuilder 查询语句builder
     **/
    private static void parseOrderBy(String sql, SearchSourceBuilder searchSourceBuilder) {
        List<OrderColumnModel> orderColumnList = SqlUtils.getOrderColumnList(sql);
        if (CollectionUtils.isNotEmpty(orderColumnList)){
            for (OrderColumnModel model : orderColumnList) {
                searchSourceBuilder.sort(model.getName(), SortOrder.fromString(model.getOrderType()));
            }
        }
    }


    /**
     * 解析where语句
     * @author Andy
     * @date 2023/1/19 22:06
     * @param sql sql语句
     * @param searchSourceBuilder dsl语句
     **/
    public static void parseWhere(String sql, SearchSourceBuilder searchSourceBuilder){
        searchSourceBuilder.query(parseQuerySqlToDsl(sql));
    }

    /**
     * 将where语句转换成dsl语句
     * @author Andy
     * @date 2023/1/19 22:15
     * @param sql sql语句
     **/
    public static BoolQueryBuilder parseQuerySqlToDsl(String sql){
        String expr = SqlUtils.getWhereStatement(sql).replace(" ", "");
        List<String> condList = SqlUtils.parseQueryConditions(sql);
        List<Map<String, Object>> mapList = SqlUtils.parseQueryConditionsToMapList(sql);
        String pattern = CommonUtils.getPattens(expr, condList);
        TreeNode tree = CommonUtils.makeExprTree(CommonUtils.transInfixToSuffixExpr(pattern), mapList);
        return transTreeToDsl(tree, QueryBuilders.boolQuery(), tree.getValue(), new ArrayList<>());
    }


    /**
     * 解析where语句
     * @author Andy
     * @date 2023/1/19 22:16
     * @param treeNode 树节点
     * @param boolQueryBuilder bool语句
     * @param operator 操作符
     * @param rangeQueryList range语句
     **/
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
                //修改
                int type = parseVal(treeNode, rangeQueryList);
                //如果为或
                QueryBuilder queryBuilder = QueryBuilderFactory.generateQueryBuilder(treeNode, type, rangeQueryList);
                if (type != 3){
                    builderList.add(queryBuilder);
                }
            }
        }
        return boolQueryBuilder;
    }

    /**
    *设置rangeQueryBuilder参数
    *@author Andy
    *@date 2022/11/29
    */
    public static void setRangeQueryBuilder(RangeQueryBuilder builder, String operator, Object value){
        if (Objects.equals(operator, ">=")) {
            builder.gte(value);
        } else if (Objects.equals(operator, ">")) {
            builder.gt(value);
        } else if (Objects.equals(operator, "<=")) {
            builder.lte(value);
        } else if (Objects.equals(operator, "<")) {
            builder.lt(value);
        }
    }

    /**
    *校验参数类型，0:整型 1:字符串
    *@author Andy
    *@param treeNode 树节点
    *@date 2022/11/29
    */
    private static Integer parseVal(TreeNode treeNode, List<RangeQueryBuilder> rangeQueryList){
        if (Objects.equals(treeNode.getOperator(), "=")) {
            return treeNode.getValType();
        }else{
            //范围查询
            Optional<RangeQueryBuilder> optional = rangeQueryList.stream().filter(ele -> Objects.equals(ele.fieldName(), treeNode.getField())).findAny();
            return optional.isPresent() ? 3 : 2;
        }
    }


    /**
     * 判断是否是新的操作符
     * @author Andy
     * @date `2023/1/19 21:18
     * @param initOp 初始操作符
     * @param currentOp 当前操作符
     **/
    private static boolean isNew(String initOp, String currentOp){
        //如果操作符部位null, 并且前后操作符不等，则使用新的bool查询
        return !Objects.isNull(initOp) && !Objects.equals(initOp, currentOp);
    }
}
