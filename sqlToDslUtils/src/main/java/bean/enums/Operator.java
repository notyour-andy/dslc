package bean.enums;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum Operator {

    LEFT("(", 3),

    RIGHT(")", 0),

    OR("or", 2),

    AND("and", 1);

    private final String mark;

    private final Integer priority;

}
