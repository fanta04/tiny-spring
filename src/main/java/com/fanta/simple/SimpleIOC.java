package com.fanta.simple;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class SimpleIOC {

    //用来存放bean对象的名字和实例
    //相当于beanFactory
    private Map<String,Object> beanMap = new HashMap<>();

    public SimpleIOC(String location)throws Exception{
        loadBeans(location);
    }

    //IOC核心方法getBean
    public Object getBean(String name){
        Object bean = beanMap.get(name);
        if(bean == null){
            throw new IllegalArgumentException("there is no bean with name "+ name);
        }
        return bean;
    }

    //读取xml文件，注册到beanMap中
    private void loadBeans(String locations) throws Exception{
        //把要解析的 XML 文档转化为输入流，以便 DOM 解析器解析它
        InputStream inputStream = new FileInputStream(locations);
        //创建 DOM 解析器的工厂
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        //调用newDocumentBuilder()得到 DOM 解析器对象
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(inputStream);
        //得到根节点，以及根下面的<bean>标签
        Element root = doc.getDocumentElement();
        NodeList nodes = root.getChildNodes();

        System.out.println("子元素数量为:"+nodes.getLength());

        //遍历<bean></bean>标签
        for(int i = 0;i<nodes.getLength();i++){
            Node node = nodes.item(i);
            if(node instanceof Element){
                Element element = (Element)node;
                String id = element.getAttribute("id");
                String className = element.getAttribute("class");
                Class beanClass = null;
                try{
                    //利用反射创建对象
                    beanClass = Class.forName(className);
                }catch (ClassNotFoundException e){
                    e.printStackTrace();
                    return ;
                }

                Object bean = beanClass.newInstance();

                //给bean对象注入property属性
                NodeList propertyNodes = element.getElementsByTagName("property");
                System.out.println("该元素的属性数量为："+propertyNodes.getLength());
                for(int j = 0;j<propertyNodes.getLength();j++) {
                    Node propertyNode = propertyNodes.item(j);
                    if (propertyNode instanceof Element) {
                        Element propertyElement = (Element) propertyNode;
                        String name = propertyElement.getAttribute("name");
                        //属性值万一不是String类型怎么办？
                        String value = propertyElement.getAttribute("value");

                        Field declaredField = bean.getClass().getDeclaredField(name);
                        declaredField.setAccessible(true);

                        if (value != null && value.length() > 0) {
                            declaredField.set(bean, value);
                        } else {
                            String ref = propertyElement.getAttribute("ref");
                            if (ref == null || ref.length() == 0) {
                                throw new IllegalArgumentException("ref config error!");
                            }
                            declaredField.set(bean, this.getBean(ref));
                        }
                        this.regist(id,bean);
                    }
                }
            }
        }
    }

    //把bean注册到beanMap中
    private void regist(String name,Object bean){
        beanMap.put(name,bean);
    }
}
