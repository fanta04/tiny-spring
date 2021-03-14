package com.fanta.ioc;

import com.fanta.ioc.BeanFactory;

public interface BeanFactoryAware {

    void setBeanFactory(BeanFactory beanFactory) throws Exception;
}