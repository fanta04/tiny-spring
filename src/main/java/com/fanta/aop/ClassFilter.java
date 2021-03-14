package com.fanta.aop;

public interface ClassFilter {
    Boolean matchers(Class beanClass) throws Exception;
}
