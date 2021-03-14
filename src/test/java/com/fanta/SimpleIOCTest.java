package com.fanta;

import com.fanta.entity.Car;
import com.fanta.entity.Wheel;
import com.fanta.simple.SimpleIOC;
import org.junit.Test;

public class SimpleIOCTest {

    @Test
    public void getBean() throws Exception{
        String location = SimpleIOC.class.getClassLoader().getResource("spring-ioc.xml").getFile();
        SimpleIOC ioc = new SimpleIOC(location);
        Wheel wheel = (Wheel) ioc.getBean("wheel");
        Car car = (Car) ioc.getBean("car");
        System.out.println(car.toString());
        System.out.println(wheel.toString());
    }
}
