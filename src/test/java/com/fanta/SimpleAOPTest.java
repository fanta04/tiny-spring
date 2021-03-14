package com.fanta;

import com.fanta.simple.Advice;
import com.fanta.simple.BeforeAdvice;
import com.fanta.simple.MethodInvocation;
import com.fanta.simple.SimpleAOP;
import org.junit.Test;

public class SimpleAOPTest {

    @Test
    public void testAOP(){
        //1.创建一个MethodInvocation实现类（方法增强：日志功能）
        MethodInvocation methodInvocation = () -> System.out.println("log start!");

        //2.创建一个Advice
        HelloService helloService = new HelloServiceImpl();
        Advice beforeAdvice = new BeforeAdvice(helloService,methodInvocation);

        //3.创建代理对象（好像只能是接口类型的，HelloServiceImpl就会报错？）
        HelloService helloWorldProxy = (HelloService) SimpleAOP.getProxy(helloService, beforeAdvice);

        helloWorldProxy.sayHelloWorld();
    }
}
