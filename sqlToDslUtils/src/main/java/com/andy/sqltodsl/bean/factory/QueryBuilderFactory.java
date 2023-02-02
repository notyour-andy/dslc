package com.andy.sqltodsl.bean.factory;

import com.andy.sqltodsl.bean.enums.QueryType;
import com.andy.sqltodsl.bean.models.TreeNode;
import com.andy.sqltodsl.utils.ElasticSearchUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * QueryBuilder工厂
 *
 * @author Andy
 * @date 2022-11-29
 */
public class QueryBuilderFactory {


    /**
    *生成相应的QueryBuilder
    *@author Andy
    *@param type：0：=整数 1：=字符 2: 范围查询 之前没有 3:范围查询 之前有
    *@date 2022/11/29
    */
    public static QueryBuilder generateQueryBuilder(TreeNode node, QueryType type, List<RangeQueryBuilder> rangeList){
        if (Objects.equals(type, QueryType.EQ_INTEGER)){
            //整数支持in查询
            int[] valueList =  Arrays.stream(node.getValue().split(",")).mapToInt(Integer::parseInt).toArray();
            return QueryBuilders.termsQuery(node.getField(), valueList);
        }else if (Objects.equals(type, QueryType.EQ_STR)){
            //字符串查询
            return QueryBuilders.matchPhraseQuery(node.getField(), node.getValue());
        }else if (Objects.equals(type, QueryType.EXIST_RANGE) || Objects.equals(type, QueryType.NOT_EXIST_RANGE)) {
            //range查询
            RangeQueryBuilder builder = null;
            if (Objects.equals(type, QueryType.NOT_EXIST_RANGE)) {
                builder = QueryBuilders.rangeQuery(node.getField());
                rangeList.add(builder);
            } else if (Objects.equals(type, QueryType.EXIST_RANGE)) {
                Optional<RangeQueryBuilder> optional = rangeList.stream()
                        .filter(ele -> Objects.equals(ele.fieldName(), node.getField()))
                        .findAny();
                if (optional.isPresent()) {
                    builder = optional.get();

                } else {
                    throw new RuntimeException("type有误");
                }
            }
            ElasticSearchUtils.setRangeQueryBuilder(builder, node.getOperator(), Objects.equals(node.getValType(), 0) ? Integer.parseInt(node.getValue()) : node.getValue());
            return builder;
        }else{
            throw new IllegalArgumentException(String.format("不存在的类型: %s", type));
        }
    }
}
