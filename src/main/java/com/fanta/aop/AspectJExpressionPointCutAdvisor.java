package com.fanta.aop;

import org.aopalliance.aop.Advice;

public class AspectJExpressionPointCutAdvisor implements PointCutAdvisor{

    private AspectJExpressionPointCut pointCut = new AspectJExpressionPointCut();
    private Advice advice;

    public void setExpression(String expression){
        pointCut.setExpression(expression);
    }

    public void setAdvice(Advice advice){
        this.advice = advice;
    }

    @Override
    public PointCut getPointCut() {
        return pointCut;
    }

    public Advice getAdvice(){
        return advice;
    }
}
