package com.fanta.ioc;

public interface BeanFactory {
    Object getBean(String beanId) throws Exception;
}

