package com.fanta.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class LogInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        System.out.println(methodInvocation.getMethod().getName() + "method start!");
        Object obj = methodInvocation.proceed();
        System.out.println(methodInvocation.getMethod().getName() + "method end!");
        return obj;
    }
}
