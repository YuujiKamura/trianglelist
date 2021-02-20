package com.jpaver.trianglelist;

//式のメタファー
interface Expression {
    Expression times(int multiplier);
    Expression plus(Expression addend);
    Money reduce(Bank bank, String to);
}
