# tiny-spring</br>

从去年十月份接触web框架，也稀里糊涂的用Spring全家桶写过几个demo。但一直对其中原理不甚理解，于是重新复习了一边Spring的IOC和AOP，并找到了[toy-spring](https://github.com/code4wt/toy-spring)这个项目，觉得自己手写一遍简易Spring框架是很有益处的。

这份README是我对照着代码梳理的简易Spring框架的笔记，是为了自己能清晰地回忆，也希望能帮助大家理解。

这个简易Spring的功能包括：

- 根据xml文件读取Bean信息注册到BeanFactory中，支持getBean()
- 基于JDK动态代理的AOP
- 将AOP和IOC整合，让AOP参与Bean的实例化过程

## 实现IOC

### BeanFactory的生命流程

1. BeanFactory加载xml文件，将xml文件中的Bean信息封装成BeanDefinition，并配置到BeanFactory自己的容器（Map）中暂存
2. 将BeanPostProcessor相关实现类配置到BeanFactory自己的容器（List）中暂存
3. BeanFactory准备就绪
4. 外部调用getBean(String beanName)方法，BeanFactory根据BeanDefinition实例化相关Bean对象

### Bean的相关类

BeanFactory的实例化对象过程需要用到几个辅助类来存放Bean的相关信息，比如BeanDefinition，BeanReference，PropertyValues，下面一个一个来介绍。

#### BeanDefinition

BeanDefinition字面意思为Bean的定义，这个类中记载了Bean的基本情况，包括它的ID名，全类名，Class类和属性。其中属性分为基本属性和引用属性（也就是在这个类中引用了其他类），引用属性需要用到BeanReference来表示

```java
public class BeanDefinition {

    private Object bean;

    private Class beanClass;

    private String beanClassName;

    private PropertyValues propertyValues = new PropertyValues();
}
```

#### BeanReference

BeanReference中保存的是xml文件中的ref值和对应的bean对象，在后续BeanFactory实例化Bean时，如果发现这个Bean依赖与其他Bean对象，就会根据BeanReference中的ref值先去实例化其依赖的对象

```java
public class BeanReference {

    private String name;

    private Object bean;
}
```

#### PropertyValues

PropertyValues用来存放Bean对象的属性，它还有一个辅助类是PropertyValue，用来保存name-value的键值对。我们可以对PropertyValues进行添加操作和取出操作。

```java
public class PropertyValues {

    private final List<PropertyValue> propertyValueList = new ArrayList<PropertyValue>();

    //添加属性值
    public void addPropertyValue(PropertyValue pv) {
        this.propertyValueList.add(pv);
    }

    //取出属性列表
    public List<PropertyValue> getPropertyValues() {
        return this.propertyValueList;
    }
}
```

### Xml文件的读取

BeanFactory工作的第一步就是从xml文件中读取Bean的相关信息，然后封装成BeanDefintion保存到自己的容器中。但是它自己并不执行这个工作，而是交由XmlBeanDefinitionReader（以下简称Reader）来做。Reader类中也有一个暂存BeanDefiniton的Map容器，BeanFactory会调用Reader的解析方法之后从它的Map中取出所有BeanDefiniton，保存到自己的Map容器中。Reader的工作流程如下：

1. 将xml文件读进来，获取根节点<beans>（我猜的，因为调试打印中获取到的好像并不是<beans>标签）
2. 遍历根节点下所有子节点<bean>，解析每个<bean>标签
   1. 获取Bean的id名和class全类名
   2. 创建BeanDefinition，将获取到的id名和class名（反射会用到）保存起来
   3. 将BeanDefiniton暂存到Map中，留作后用
   4. 遍历<bean>的属性值，保存每一个属性
      1. 获取<property>属性标签的name值和value值
      2. 如果是普通属性则创建PropertyValue(name,value)放入BeanDefiniton中
      3. 如果是引用属性则创建PropertyValue(name,new BeanReference(ref))放入BeanDefiniton中

至此xml文件就解析完毕，我们得到了一个保存了所有Bean信息的容器，可以供BeanFactory使用了。

### 注册BeanPostProcessor

BeanPostProcessor是Spring对外拓展的接口之一，可以让程序员插手Bean实例化的过程，AOP也是在这里织入横切逻辑的。BeanPostProcessor有两个方法：postProcessBeforeInitialization(Object bean, String beanName)和Object postProcessAfterInitialization(Object bean, String beanName)。在后一个方法中，我们可以返回一个代理对象而不是原来的对象，造成偷天换日的效果。

BeanFactory将<id,beanDefinition>键值对保存到自己的Map之后，立刻开始注册BeanPostProcessor相关实现类：

1. 根据BeanDefinition寻找实现了BeanPostProcessor接口的类，调用getBean()实例化这些类
2. 将实例化好的BeanPostProcessor实现类添加到List容器中

至此，BeanFactory中就有了BeanPostProcessor实现类的列表，可以在实例化Bean的过程中调用BeanPostProcessor的前置方法和后置方法。



## 实现AOP

本项目中aop包中的类比较繁杂，很容易犯迷糊，为了能够更清晰的理解，首先来复习AOP中的术语。

- `JoinPoint`：程序执行的每一行之间，都可以看作一个JoinPoint，可以在其中横插我们自己的逻辑
- `PointCut`：用来捕获JoinPoint，我们需要知道PointCut，才可以知道该往哪些JoinPoint上织入横切逻辑
- `Advice`：横切逻辑本体，常见的形式有日志，运行时间等
- `Aspect`：PointCut+Advice，一般叫做切面类，这个类中的成员包括切点和通知。在Spring AOP中一般叫做Advisor

AOP术语的工作场景：

![图片截自《Spring揭秘》](https://i.loli.net/2021/03/14/DTOzREqosJNAt3g.png)

### JDK对动态代理的支持

我们都知道AOP是基于动态代理的，关于动态代理的应用背景就不再赘述，来看看JDK中动态代理的使用方法。

动态代理中两个关键的类：

- `Proxy`：提供静态方法`newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h)`来返回目标对象的代理对象
- `InvocationHandler`：通过`invoke(Object proxy, Method method,Object[] args)`来实现代理对象中方法的调用，可以在这个方法中写入我们自己的横切逻辑

假设以下情景：

- HelloServiceImpl实现了HelloService接口，接口中有一个方法`viod hello()`
- 对象Handler是InvocationHandler的实现类，invoke方法里在调用目标对象的方法之前打印方法名

那么通过`newProxyInstance(HelloServiceImpl.getClassLoader(), HelloServiceImpl.getClass().getInterfaces(), Handler)`方法就可以返回一个HelloServiceImpl的代理对象proxy。调用`proxy.hello()`时，对象内部将委托给`Handler.invoke(proxy,hello,args)`处理。

因此，Handler的invoke()方法可以对目标方法进行拦截，相当于AOP中的通知Advice。还可以在invoke()加入判断，如果是，则执行代理的方法，否则执行原来的方法。

### 基于JDK动态代理的AOP实现

接下来分两个方面来实现AOP：第一个方面，直接用JDK提供的动态代理实现AOP，AOP不参与Bean的实例化过程。第二个方面，将AOP和IOC结合起来，在BeanFactory实例化对象的过程中做一点手脚，将目标对象替换成代理对象返回，这个代理对象在特定的方法执行的时候（用PointCut设定）可以执行相应的横切逻辑（Advice）。

首先来看第一个方面，我们需要实现的就是一个代理对象生成器，主要的功能写在了JdkDynamicAopProxy这个类中。这个类包含两个方法：getProxy()用于生成代理对象，invoke()方法是InvocationHandler的实现，包含了判断是否要拦截该方法和将通知加入原来方法中。

```java
/**
 * 基于 JDK 动态代理的代理对象生成器
 */
final public class JdkDynamicAopProxy extends AbstractAopProxy implements InvocationHandler {

    public JdkDynamicAopProxy(AdvisedSupport advised) {
        super(advised);
    }

    /**
     * 为目标 bean 生成代理对象
     *
     * @return bean 的代理对象
     */
    @Override
    public Object getProxy() {
        return Proxy.newProxyInstance(getClass().getClassLoader(), advised.getTargetSource().getInterfaces(), this);
    }

    /**
     * InvocationHandler 接口中的 invoke 方法具体实现，封装了具体的代理逻辑
     *
     * @param proxy
     * @param method
     * @param args
     * @return 代理方法或原方法的返回值
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodMatcher methodMatcher = advised.getMethodMatcher();

        // 使用方法匹配器 methodMatcher 测试 bean 中原始方法 method 是否符合匹配规则
        if (methodMatcher != null && methodMatcher.matchers(method, advised.getTargetSource().getTargetClass())) {

            // 获取 Advice。MethodInterceptor 的父接口继承了 Advice
            MethodInterceptor methodInterceptor = advised.getMethodInterceptor();

            // 将 bean 的原始 method 封装成 MethodInvocation 实现类对象，
            // 将生成的对象传给 Adivce 实现类对象，执行通知逻辑
            return methodInterceptor.invoke(
                    new ReflectiveMethodInvocation(advised.getTargetSource().getTarget(), method, args));
        } else {
            // 当前 method 不符合匹配规则，直接调用 bean 中的原始 method
            return method.invoke(advised.getTargetSource().getTarget(), args);
        }
    }
}

```

其中`MethodInterceptor`类是AOP联盟包中的标准接口，methodInterceptor.invoke()这个方法才是具体实现通知的方法，我们需要自己编写`MethodInterceptor`的实现类去实现这个invoke()方法，该方法接收一个MethodInvocation实现类作为参数，其中包含了正在被拦截的方法。`MethodInterceptor`实现类如下：

```java
public class LogInterceptor implements MethodInterceptor {
    @Override
    //实现一个日志功能
    public Object invoke(MethodInvocation invocation) throws Throwable {
        System.out.println(invocation.getMethod().getName() + " method start");
        Object obj= invocation.proceed();
        System.out.println(invocation.getMethod().getName() + " method end");
        return obj;
    }
}
```

#### 辅助类

JdkDynamicAopProxy有许多其他类来帮助它实现功能，下面从上到下地来看看到底有哪些类。

##### AbstractAopProxy

首先是JdkDynamicAopProxy继承的抽象父类，它包含一个AdvisedSupport类型的成员变量，构造方法是设定这个成员变量。没有什么特别的。

##### AdvisedSupport

AdvisedSupport包含的信息十分重要，它是生成代理对象的信息载体。根据JdkDynamicAopProxy的代码可以看出，它会根据AdvisedSupport的信息来生成相应的代理对象。AdvisedSupport的成员变量具体包括：TargetSource（目标对象），MethodInterceptor（方法拦截器），MethodMatcher（方法匹配器）

##### TargetSource

接下来是AdvisedSupport的成员变量。首先是TargetSource，它说明了目标对象，目标对象的Class类，以及目标对象所实现的接口们。它的作用体现在JdkDynamicAopProxy.getProxy()所需要的目标对象所有信息。

##### MethodInterceptor

方法拦截器不用多说，前面已经解释过，代理对象执行方法时会委托给它的实现类（上文的LogInterceptor）执行。

##### MethodMatcher

方法匹配器用来判断当前的方法是否应该被拦截，如果是，则交由MethodInterceptor执行方法，否则就调用原方法。

##### ReflectiveMethodInvocation

上文说到MethodInterceptor.invoke()方法需要接收一个MethodInvocation实现类作为参数。MethodInvocation也是AOP联盟包中的标准接口，MethodInvocation的顶级接口是JoinPoint，由此我们可以窥探出这个MethodInvocation要包含正在被拦截的方法，方法的对象，以及方法的参数：

```java
public class ReflectiveMethodInvocation implements MethodInvocation {

    protected Object target;

    protected Method method;

    protected Object[] arguments;

    public ReflectiveMethodInvocation(Object target, Method method, Object[] arguments) {
        this.target = target;
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    // 执行原方法
    public Object proceed() throws Throwable {
        return method.invoke(target, arguments);
    }

    @Override
    public Method getMethod() { return method;}

    @Override
    public Object[] getArguments() {return arguments;}

    @Override
    public Object getThis() {return target;}

    @Override
    public AccessibleObject getStaticPart() {return method;}
}
```

#### 测试

到这里，代理对象生成器的代码已经全部分析完毕。接下来我们可以做一个简单的测试，看看JdkDynamicAopProxy是否生成了相应的代理对象：

```java
public class JdkDynamicAopProxyTest {

    @Test
    public void getProxy() throws Exception {
        System.out.println("---------- no proxy ----------");
        HelloService helloService = new HelloServiceImpl();
        helloService.sayHelloWorld();

        System.out.println("\n----------- proxy -----------");
        //创建 JdkDynamicAopProxy 所需要的信息载体
        AdvisedSupport advisedSupport = new AdvisedSupport();
        //为 advisedSupport 设置方法拦截器
        advisedSupport.setMethodInterceptor(new LogInterceptor());
        //创建目标对象的信息载体
        TargetSource targetSource = new TargetSource(
                helloService, HelloServiceImpl.class, HelloServiceImpl.class.getInterfaces());
        //为 advisedSupport 设置targetSource
        advisedSupport.setTargetSource(targetSource);
        //为 advisedSupport 方法匹配器，令所有方法都匹配上
        advisedSupport.setMethodMatcher((Method method, Class beanClass) -> true);
        //创建代理对象
        helloService = (HelloService) new JdkDynamicAopProxy(advisedSupport).getProxy();

        helloService.sayHelloWorld();
        helloService.sayHello();
    }
}

/**
 * 输出为：
 *---------- no proxy ----------
 * hello world!
 * hello
 *
 * ----------- proxy -----------
 * sayHelloWorld method start
 * hello world!
 * sayHelloWorld method end
 * sayHello method start
 * hello
 * sayHello method end
 * */
```

### AOP与IOC合作

在代理对象生成器中，我们只是简单地走了一遍代理对象的生成过程，这个代理对象是孤立的，如何让代理对象的生成融入Bean实例化的过程就是我们接下来要做的事情，即AOP融入IOC。

AOP和IOC合作的关键类是`BeanPostProcessor`。还记得`BeanPostProcessor`有两个方法吗，即前置方法和后置方法，我们可以在Bean的实例化过程中，调用前置方法返回一个代理对象而不是实例化后的目标对象本身。其中具体的工作交由`BeanPostProcessor`的实现类`AspectJAwareAdvisorAutoProxyCreator`来完成。

AspectJAwareAdvisorAutoProxyCreator（下面简称ProxyCreator）工作前的准备工作：

BeanFactory在填充Bean的属性时，会检测这个Bean是否属于BeanFactoryAware类型，如果是则给往这个Bean的属性中注入自己的引用，即Bean.setBeanFactory(this)。而ProxyCreator实现了BeanFactoryAware接口，所以说这一步是将BeanFactory的引用注入到了ProxyCreator里，方便后续创建代理对象。

在外部调用getBean()方法时，BeanFactory在根据BeanDefinition为其注入属性之后，会调用自身BeanPostProcessor的方法（初始化BeanFactory时已经把BeanPostProcessor暂存到自己的List容器里了）。这时候BeanPostProcessor的实现类ProxyCreator就开始工作了，过程如下：

1. 从BeanFactory中查找实现了切面接口（下面具体分析）的类，切面中包含了切点和通知
2. 利用切点中的类匹配器判断当前类是否要拦截
   1. 如果匹配成功，创建AdviceSupport对象，为其设置方法匹配器，方法拦截器以及目标对象信息
   2. 将AdviceSupport作为JdkDynamicAopProxy的参数，让JdkDynamicAopProxy返回一个代理对象给BeanFacotry
3. 如果匹配不成功，则返回原对象

```java
public class AspectJAwareAdvisorAutoProxyCreator implements BeanPostProcessor, BeanFactoryAware {

    private XmlBeanFactory xmlBeanFactory;

    @Override
    //前置方法
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
    //后置方法
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws Exception {
        this.xmlBeanFactory = (XmlBeanFactory) beanFactory;
    }
}
```

这样一来，BeanFactory在实例化Bean的时候，就可以按照程序员编写的规则，返回相应的代理对象或者原对象了。

#### 辅助类

下面还是从上到下，讲解一下围绕AspectJAwareAdvisorAutoProxyCreator的辅助类。

##### AspectJExpressionPointCutAdvisor

AspectJExpressionPointCutAdvisor是一个切面类，类成员包含了一个AspectJExpressionPointCut类型的切点，和一个Advice类型的通知（此处的Advice类型是AOP联盟包里的顶级接口，其实我觉得换成MethodInterceptor也行）

##### AspectJExpressionPointCut

这个切点类实现了我们自己定义的Point，ClassFilter，MethodMatcher。大量的调用了AspectJ提供的方法用来匹配类和方法，再此不过多赘述，代码中有详细注释。

##### **Point，ClassFilter，MethodMatcher**

这三个是我们自己定义的接口，Point中有获取ClassFilter和MethodMatcher的方法。ClassFilter和MethodMatcher中各有一个返回Boolean值的方法，用来表示是否匹配上了类或者方法。

#### 测试

1. 首先在xml文件中配置好目标类，方法拦截器，ProxyCreator和切面类。这些都会注册到BeanFactory中

   ```xml
   <beans>
       <bean id="helloService" class="com.fanta.HelloServiceImpl"/>
       <!--通知为日志功能-->
       <bean id="logInterceptor" class="com.fanta.aop.LogInterceptor"/>
       <bean id="autoProxyCreator" class="com.fanta.aop.AspectJAwareAdvisorAutoProxyCreator"/>
       <!--切面类匹配HelloService下的sayHello()方法-->
       <bean id="helloServiceAspect" class="com.fanta.aop.AspectJExpressionPointCutAdvisor">
           <property name="advice" ref="logInterceptor"/>
           <property name="expression" value="execution(* com.fanta.HelloService.sayHello(..))"/>
       </bean>
   </beans>
   ```

2. 编写测试

```java
@Test
public void testAOPwithIOC() throws Exception{
    System.out.println("--------- AOP test ----------");
    String location = getClass().getClassLoader().getResource("spring-ioc.xml").getFile();
    XmlBeanFactory xmlBeanFactory = new XmlBeanFactory(location);
    HelloService helloService = (HelloService) xmlBeanFactory.getBean("helloService");
    helloService.sayHelloWorld();
}
/**
 * 输出：
 * hello world!
 * sayHellomethod start!
 * hello!
 * sayHellomethod end!
 * */
```

可以看到，此时helloService已经是一个符合要求的代理对象了，它仅在sayHello()方法加入了日志功能。



# 参考资料：

[toy-spring](https://github.com/code4wt/toy-spring)

[tiny-spring](https://github.com/code4craft/tiny-spring)

[Spring揭秘](https://pan.baidu.com/s/1gWyq52dVBv277DLIGxQNQg)提取码：c6qx
