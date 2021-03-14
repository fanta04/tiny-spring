package com.fanta.aop;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JdkDynamicAopProxy extends AbstractAopProxy implements InvocationHandler {

    //构造方法：初始化advisedSupport
    public JdkDynamicAopProxy(AdvisedSupport advisedSupport) {
        super(advisedSupport);
    }

    //获取代理对象
    @Override
    public Object getProxy() {
        return Proxy.newProxyInstance(getClass().getClassLoader(),advisedSupport.getTargetSource().getInterfaces(),this);
    }

    //InvocationHandler 接口中的 invoke 方法具体实现，封装了具体的代理逻辑
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodMatcher methodMatcher = advisedSupport.getMethodMatcher();

        //使用methodMatcher测试被代理对象中的method是否符合匹配规则
        if(methodMatcher!=null && methodMatcher.matchers(method,advisedSupport.getTargetSource().getTargetClass())){
            //如果符合，将被代理类的method封装成MethodInvocation的实现类对象（也就是ReflectiveMethodInvocation）
            //将生成的对象传给MethodInterceptor，执行通知逻辑（日志逻辑）
            MethodInterceptor methodInterceptor = advisedSupport.getMethodInterceptor();
            return methodInterceptor.invoke(
                    new ReflectiveMethodInvocation(advisedSupport.getTargetSource().getTarget(),method,args
            ));
        }
        //如果不匹配，则直接执行原方法
        else {
            return method.invoke(advisedSupport.getTargetSource().getTarget(),args);
        }
    }
}
