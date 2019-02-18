## Lambda - Variable Test

~~~java
public class Main {

    private int instanceVar; //인스턴스 변수

    private static int classVar; //클래스 변수

    public static void main(String[] args) {
        method(0);
    }

    public static void method(int parameter) {
        int localVar; //지역 변수
        Runnable r = (() -> {
            localVar = 1;
            instanceVar = 1;
            classVar = 1;
            parameter = 1;
        });
        r.run();
    }
}
~~~
위의 코드 테스트 결과, 지역변수와, 파라미터의 경우  
"Variable used in lambda expression should be final or effectively final" 오류 발생  
인스턴스 변수의 경우, "Non-static field 'instanceVar' cannot be referenced from a static context" 오류 발생

~~~java
public class Main {

    private String instanceVar; //인스턴스 변수

    private static String classVar; //클래스 변수

    public static void main(String[] args) {
        method("Hi");
    }

    public static void method(String parameter) {
        String localVar; //지역 변수
        Runnable r = (() -> {
            localVar = "Hello";
            instanceVar = "Hello";
            classVar = "Hello";
            parameter = "Hello";
        });
        r.run();
    }
}
~~~
String으로 테스트 했을 경우, int형과 동일하게 오류 발생

~~~java
public class Main {

    private List<String> instanceVar; //인스턴스 변수

    private static List<String> classVar; //클래스 변수

    public static void main(String[] args) {
        method(Arrays.asList("a", "b", "c"));
    }

    public static void method(List<String> parameter) {
        List<String> localVar; //지역 변수
        Runnable r = (() -> {
            System.out.println(Thread.currentThread().getName());
            localVar.add("d");
            instanceVar.add("d");
            classVar.add("d");
            parameter.add("d");
        });
        r.run();
    }
}
~~~
List로 하였을 경우, 지역변수는 "Variable 'localVar' might not have been initialized" 오류 발생   
인스턴스 변수는 "Non-static field 'instanceVar' cannot be referenced from a static context" 오류 발생  
  
### 결  론
지역변수와 매개변수의 경우 람다 내에서는 final로 간주되어 수정 시 오류 발생,
단 ReferenceType의 경우 initialized 된 값이라면 수정 가능, 실제 value는 Heap영역에 저장되기 때문

_테스트 결과를 토대로 추론한 내용이기 때문에, 의견이나 이의 사항 있으시다면 수정 부탁드립니다_