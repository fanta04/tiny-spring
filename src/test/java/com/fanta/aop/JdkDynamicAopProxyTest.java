package com.fanta.aop;

import com.fanta.HelloService;
import com.fanta.HelloServiceImpl;
import org.junit.Test;

import java.lang.reflect.Method;

public class JdkDynamicAopProxyTest {

    @Test
    public void test(){
        System.out.println("----Without AOP----");
        //创建AdvisedSupport对象：包含被代理对象，方法拦截器（方法增强），方法匹配器
        AdvisedSupport advisedSupport = new AdvisedSupport();
        HelloService helloService = new HelloServiceImpl();
        helloService.sayHelloWorld();

        System.out.println("------With AOP-----");
        //方法拦截器的增强功能：日志功能
        advisedSupport.setMethodInterceptor(new LogInterceptor());
        //创建被代理对象源：包含被代理对象，被代理对象实现类的类对象，被代理对象实现类所实现的接口们
        TargetSource targetSource = new TargetSource(helloService,HelloServiceImpl.class,HelloServiceImpl.class.getInterfaces());
        advisedSupport.setTargetSource(targetSource);
        //设置方法匹配器：所有的方法都能匹配上->所有的方法都能被拦截，并发挥增强功能（日志）
        advisedSupport.setMethodMatcher((Method method,Class beanClass) -> true);
        //创建代理对象
        helloService = (HelloService) new JdkDynamicAopProxy(advisedSupport).getProxy();
        //调用代理对象方法
        helloService.sayHelloWorld();
    }
}
