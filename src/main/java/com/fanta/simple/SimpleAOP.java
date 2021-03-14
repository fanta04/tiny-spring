package com.fanta.simple;

import java.lang.reflect.Proxy;

public class SimpleAOP {

    //返回代理对象
    public static Object getProxy(Object bean,Advice advice){
        return Proxy.newProxyInstance(SimpleAOP.class.getClassLoader(),bean.getClass().getInterfaces(),advice);
    }
}
