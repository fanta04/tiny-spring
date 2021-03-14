package com.fanta;

public class HelloServiceImpl implements HelloService{

    @Override
    public void sayHelloWorld() {
        System.out.println("hello world!");
    }

    @Override
    public void sayHello() {
        System.out.println("hello!");
    }
}
