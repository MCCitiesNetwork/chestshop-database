package io.github.md5sha256.chestshopdatabase.database.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConditionBuilder {

    private static final String OPEN = "(";
    private static final String CLOSE = ")";

    private static final String AND = " AND ";
    private static final String OR = " OR ";

    private final List<String> text = new ArrayList<>();

    public ConditionBuilder applyIf(boolean value, Consumer<ConditionBuilder> consumer) {
        if (value) {
            consumer.accept(this);
        }
        return this;
    }

    public ConditionBuilder and(String... exprs) {
        for (String expr : exprs) {
            text.add(AND);
            text.add(expr);
        }
        return this;
    }

    public String newAnd(String... exprs) {
        return new ConditionBuilder().and(exprs).toString();
    }

    public String newOr(String ... exprs) {
        return new ConditionBuilder().or(exprs).toString();
    }

    public ConditionBuilder or(String... exprs) {
        for (String expr : exprs) {
            text.add(OR);
            text.add(expr);
        }
        return this;
    }

    public String toString() {
        if (text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(OPEN);
        for (int i = 1; i < text.size(); i++) {
            builder.append(text.get(i));
        }
        builder.append(CLOSE);
        return builder.toString();
    }

}
