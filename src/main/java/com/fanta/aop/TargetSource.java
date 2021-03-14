package com.fanta.aop;

public class TargetSource {
    private Class<?> targetClass; //被代理类的Class
    private Class<?>[] interfaces; //被代理类实现的接口
    private Object target; //被代理类

    public TargetSource( Object target, Class<?> targetClass,Class<?>... interfaces){
        this.targetClass = targetClass;
        this.interfaces = interfaces;
        this.target = target;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }


    public Class<?>[] getInterfaces() {
        return interfaces;
    }

    public Object getTarget() {
        return target;
    }

}
