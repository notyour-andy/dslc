package utils;

import java.util.List;
import java.util.Set;

public class CommonUtils {

    public static void main(String[] args) {
        String sql = "select * from z_test where a = '1' and b = '2' or (c = '3' or (d = '4' and e = '5'))";
    }



    private String getOperatorList(String expr, List<String> conditionList){
        
    }

    /**
     * 将中缀表达式转换为后缀表达式
     * @author Andy
     * @date 2022/11/14 22:42
     * @param expr 表达式
     * @return 后缀表达式
     **/
    public static String transInfixToSuffixExpr(String expr){
        String[] s = expr.split(" ");


    }
}
