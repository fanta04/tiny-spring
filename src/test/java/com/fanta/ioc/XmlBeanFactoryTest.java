package com.fanta.ioc;

import com.fanta.HelloService;
import com.fanta.entity.Car;
import com.fanta.entity.Wheel;
import org.junit.Test;

public class XmlBeanFactoryTest {

    @Test
    public void getBean()throws Exception{
        System.out.println("--------- IOC test ----------");
        String location = XmlBeanFactory.class.getClassLoader().getResource("spring-ioc.xml").getFile();
        XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(location);
        Wheel wheel = (Wheel) xmlBeanFactory.getBean("wheel");
        Car car = (Car) xmlBeanFactory.getBean("car");
        System.out.println(wheel.toString());
        System.out.println(car.toString());

    }

    @Test
    public void testAOPwithIOC() throws Exception{
        System.out.println("--------- AOP test ----------");
        String location = getClass().getClassLoader().getResource("spring-ioc.xml").getFile();
        XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(location);
        HelloService helloService = (HelloService) xmlBeanFactory.getBean("helloService");

        helloService.sayHelloWorld();
        helloService.sayHello();
    }
}

/**
 * 输出：
 * hello world!
 * sayHellomethod start!
 * hello!
 * sayHellomethod end!
 * */