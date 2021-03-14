package com.fanta.aop;

public class ProxyFactory extends AdvisedSupport implements AopProxy{
    @Override
    public Object getProxy() {
        return new JdkDynamicAopProxy(this).getProxy();
    }

}
