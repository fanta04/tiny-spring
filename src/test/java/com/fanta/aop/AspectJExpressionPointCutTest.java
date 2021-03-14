package com.fanta.aop;

import com.fanta.HelloService;
import com.fanta.HelloServiceImpl;
import org.junit.Test;

public class AspectJExpressionPointCutTest {

    @Test
    public void testClassFilter() throws Exception{
        String expression = "execution(* com.fanta.*.*(..))";
        AspectJExpressionPointCut aspectJExpressionPointCut = new AspectJExpressionPointCut();
        aspectJExpressionPointCut.setExpression(expression);
        Boolean matchers = aspectJExpressionPointCut.matchers(HelloService.class);
        System.out.println(matchers);
    }

    @Test
    public void testMethodMatcher() throws Exception{
        String expression = "execution(* com.fanta.*.sayHelloWorld(..))";
        AspectJExpressionPointCut aspectJExpressionPointCut = new AspectJExpressionPointCut();
        aspectJExpressionPointCut.setExpression(expression);
        Boolean matchers = aspectJExpressionPointCut.matchers
                (HelloServiceImpl.class.getDeclaredMethod("hello"), HelloServiceImpl.class);
        System.out.println(matchers);

    }
}
