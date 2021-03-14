package com.fanta.ioc;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlBeanFactory implements BeanFactory{

    private Map<String,BeanDefinition> beanDefinitionMap = new HashMap<>();
    private List<String> beanDefinitionNames = new ArrayList<>();
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    private XmlBeanDefinitionReader beanDefinitionReader;

    //构造方法
    public XmlBeanFactory(String location) throws Exception {
        beanDefinitionReader = new XmlBeanDefinitionReader();
        loadBeanDefinition(location);
    }

    private void loadBeanDefinition(String location) throws Exception {
        beanDefinitionReader.loadBeanDefinitions(location);
        registerBeanDefinition();
        registerBeanPostProcessor();
    }

    //将XmlBeanDefinitionReader中的registry注册到本类的beanDefinitionMap中
    private void registerBeanDefinition() {
        for(Map.Entry<String,BeanDefinition> entry : beanDefinitionReader.getRegistry().entrySet()){
            String key = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            beanDefinitionMap.put(key,beanDefinition);
            beanDefinitionNames.add(key);
        }
    }

    //BeanFactory核心方法
    //由BeanDefinition填充反射生成的bean的属性值
    public Object getBean(String name) throws Exception {
        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        if(beanDefinition == null){
            throw new IllegalArgumentException("no this bean with name "+name);
        }
        Object bean = beanDefinition.getBean();
        if(bean == null){
            //创建bean实例对象，并填充属性
            bean = createBean(beanDefinition);
            //将beanPostProcessor的前置和后置操作织入bean的实例化过程中
            //返回的是bean的代理对象
            bean = initializaBean(bean,name);
            beanDefinition.setBean(bean);
        }
        return bean;
    }

    //反射生成bean
    private Object createBean(BeanDefinition beanDefinition) throws Exception {
        Object bean = beanDefinition.getBeanClass().newInstance();
        applyPropertyValues(bean,beanDefinition);

        return bean;
    }

    //利用beanDefinition填充属性值
    private void applyPropertyValues(Object bean, BeanDefinition beanDefinition) throws Exception {
        if(bean instanceof BeanFactoryAware){
            ((BeanFactoryAware) bean).setBeanFactory((BeanFactory) this);
        }
        //取beanDefinition中的PropertyValues
        for(PropertyValue propertyValue : beanDefinition.getPropertyValues().getPropertyValueList()){
            Object value = propertyValue.getValue();
            if(value instanceof BeanReference){
                BeanReference beanReference = (BeanReference) value;
                value = getBean(beanReference.getName());
            }
            //如果属性是普通类型的话：
            try {
                //使用set方法设置属性
                Method declaredMethod = bean.getClass().getDeclaredMethod(
                        "set" + propertyValue.getName().substring(0, 1).toUpperCase()
                                + propertyValue.getName().substring(1), value.getClass());
                declaredMethod.setAccessible(true);

                declaredMethod.invoke(bean, value);
            }
            //使用field直接设置属性
            catch (Exception e) {
                Field field = bean.getClass().getDeclaredField(propertyValue.getName());
                field.setAccessible(true);
                field.set(bean,value);
            }
        }
    }

    //
    private Object initializaBean(Object bean, String name) throws Exception {
        for(BeanPostProcessor beanPostProcessor : beanPostProcessors){
            bean = beanPostProcessor.postProcessBeforeInitialization(bean,name);
        }
        for(BeanPostProcessor beanPostProcessor : beanPostProcessors){
            bean = beanPostProcessor.postProcessAfterInitialization(bean,name);
        }
        return bean;
    }

    public void registerBeanPostProcessor() throws Exception{
        List beans = getBeansForType(BeanPostProcessor.class);
        for(Object bean : beans){
            addBeanPostProcessor((BeanPostProcessor) bean);
        }
    }

    public List getBeansForType(Class type) throws Exception{
        List beans = new ArrayList<>();
        for(String beanDefinitionName : beanDefinitionNames){
            if(type.isAssignableFrom(beanDefinitionMap.get(beanDefinitionName).getBeanClass())){
                beans.add(getBean(beanDefinitionName));
            }
        }
        return beans;
    }

    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor){beanPostProcessors.add(beanPostProcessor);}
}
