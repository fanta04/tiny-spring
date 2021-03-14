package com.fanta.ioc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class XmlBeanDefinitionReader implements BeanDefinitionReader{

    private Map<String,BeanDefinition> registry = new HashMap<>();

    @Override
    //读取xml文件中的根节点，进一步解析
    public void loadBeanDefinitions(String location) throws IOException, ParserConfigurationException, SAXException {
        InputStream inputStream = new FileInputStream(location);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        Document doc = documentBuilder.parse(inputStream);
        Element root = doc.getDocumentElement();

        parseBeanDefinitions(root);
    }

    //读取根节点下的子节点，进一步解析
    private void parseBeanDefinitions(Element root) {
        NodeList nodes = root.getChildNodes();
        for(int i = 0;i<nodes.getLength();i++){
            Node node = nodes.item(i);
            if(node instanceof Element){
                Element ele = (Element) node;
                parseBeanDefinition(ele);
            }
        }
    }

    //读取每一个<bean>标签的内容，创建与之对应的BeanDefinition对象，填充其内容，注册到registry中
    private void parseBeanDefinition(Element ele) {
        String id = ele.getAttribute("id");
        String className = ele.getAttribute("class");
        BeanDefinition beanDefinition = new BeanDefinition();
        //设置全类名时就已经反射生成了Class类
        beanDefinition.setBeanClassName(className);
        processProperty(ele,beanDefinition);
        registry.put(id,beanDefinition);
    }

    //解析<bean>标签下的每一个属性，填充到beanDefinition中
    private void processProperty(Element ele, BeanDefinition beanDefinition) {
        NodeList propertyNodes = ele.getElementsByTagName("property");
//        System.out.println("该元素的属性数量为："+propertyNodes.getLength());
        for(int i = 0;i<propertyNodes.getLength();i++){
            Node propertyNode = propertyNodes.item(i);
            if(propertyNode instanceof Element){
                Element propertyElement = (Element) propertyNode;
                String name = propertyElement.getAttribute("name");
                //属性值万一不是String类型怎么办？
                String value = propertyElement.getAttribute("value");

//                System.out.println(name+" "+value);

                //填充一个value属性
                if(value != null && value.length()>0){
                    beanDefinition.getPropertyValues().add(new PropertyValue(name,value));
                }
                //填充一个ref属性
                else {
                    String ref = propertyElement.getAttribute("ref");
                    if(ref != null && ref.length()>0){
                        //要设置BeanReference的id名，这样才能找到ref是哪个bean
                        BeanReference beanReference = new BeanReference(ref);
                        beanDefinition.getPropertyValues().add(new PropertyValue(name,beanReference));
                    }else throw new IllegalArgumentException("ref config error!");
                }
            }
        }
    }

    public Map<String,BeanDefinition> getRegistry(){return this.registry;}
}
