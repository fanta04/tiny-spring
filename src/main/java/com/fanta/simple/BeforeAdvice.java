package com.fanta.simple;


import java.lang.reflect.Method;

public class BeforeAdvice implements Advice{

    private Object bean; //被代理对象
    private MethodInvocation methodInvocation; //方法增强

    public BeforeAdvice(Object bean,MethodInvocation methodInvocation){
        this.bean = bean;
        this.methodInvocation = methodInvocation;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //先调用方法增强（日志之类的功能）
        methodInvocation.invoke();
        //再执行原来的方法
        return method.invoke(bean,args);
    }
}
