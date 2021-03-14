package com.fanta.aop;

import com.fanta.ioc.BeanFactory;
import com.fanta.ioc.BeanFactoryAware;
import com.fanta.ioc.BeanPostProcessor;
import com.fanta.ioc.XmlBeanFactory;
import org.aopalliance.intercept.MethodInterceptor;

import java.util.List;

public class AspectJAwareAdvisorAutoProxyCreator implements BeanPostProcessor, BeanFactoryAware {

    private XmlBeanFactory xmlBeanFactory;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
        if(bean instanceof AspectJExpressionPointCutAdvisor){
            return bean;
        }
        if(bean instanceof MethodInterceptor){
            return bean;
        }

        //1.获取xmlBeanFactory中 AspectJExpressionPointCutAdvisor 类型的对象
        List<AspectJExpressionPointCutAdvisor> advisors = xmlBeanFactory.getBeansForType(AspectJExpressionPointCutAdvisor.class);
        for(AspectJExpressionPointCutAdvisor advisor : advisors){
            //2.使用其中的 PointCut 匹配当前bean对象
            if(advisor.getPointCut().getClassFilter().matchers(bean.getClass())){
                ProxyFactory advisedSupport = new ProxyFactory();
                advisedSupport.setMethodInterceptor((MethodInterceptor) advisor.getAdvice());
                advisedSupport.setMethodMatcher(advisor.getPointCut().getMethodMatcher());

                TargetSource targetSource = new TargetSource(bean,bean.getClass(),bean.getClass().getInterfaces());
                advisedSupport.setTargetSource(targetSource);
                //3.生成代理对象并返回
                return advisedSupport.getProxy();
            }
        }

        //4.匹配失败，返回原来的bean
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws Exception {
        this.xmlBeanFactory = (XmlBeanFactory) beanFactory;
    }
}
