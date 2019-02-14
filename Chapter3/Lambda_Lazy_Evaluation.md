## Lambda - Lazy Evaluation

자바는 매서드의 매개변수를 평가할 때 eager로 처리한다.
~~~java
public static boolean sleepMethod(int value) {
    System.out.println("sleepMethod Call " + value);
    try {
        Thread.sleep(value);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return value > 2000;
}

public static void lazyTest(boolean a, boolean b) {
    System.out.println("lazyTest call!!");
    if (a && b) {
       System.out.println("Finish");
    }
}
~~~ 
lazyTest(sleepMethod(2000), sleepMethod(3000));  
이와 같이 위의 메서드를 사용하여 실행 시, 매개변수는 eager로 처리되기 때문에
결과는 아래와 같이 약 5초의 시간이 걸리게 된다.
~~~
sleepMethod Call 2000
sleepMethod Call 3000
lazyTest call!!
Process Time :: 5007 ms
~~~

위와 같은 경우 실제로는 a의 결과만 확인한다면 약 2초만에 연산을 종료할 수 있다.  
위의 문제를 해결하기 위해 lambda를 활용해보자.
~~~java
public static boolean sleepMethod(int value) {
    System.out.println("sleepMethod Call " + value);
    try {
        Thread.sleep(value);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return value > 2000;
}

public static void lazyTest(Supplier<Boolean> a, Supplier<Boolean> b) {
    System.out.println("lazyTest call!!");
    if (a.get() && b.get()) {
       System.out.println("Finish");
    }
}
~~~
lazyTest(() -> sleepMethod(2000), () -> sleepMethod(3000));  
이처럼 lambda를 활용할 시, 불필요한 연산이 eager로 수행되지 않고, 실제 사용시점에 실행되게 된다.
결과적으로 아래와 같이 약 2초로 Process 시간을 줄일 수 있다.
~~~
lazyTest call!!
sleepMethod Call 2000
Process Time :: 2003 ms
~~~
다양한 사례에 적절하게 lambda의 lazy evaluation을 활용한다면  
성능 개선이 가능하다.