## Chapter11 CompletableFuture:조합할 수 있는 비동기 프로그래밍 

### 이 장의 내용
- 비동기 작업을 만들고 결과 얻기
- 비블록 동작으로 생산성 높이기
- 비동기 API 설계와 구현 
- 동기 API를 비동기적으로 소비하기
- 두 개 이상의 비동기 연산을 파이프라인으로 만들고 합치기 
- 비동기 작업 완료에 대응하기 
  
### 11.1 Future 
: 자바 5부터 미래의 어느 시점에 결과를 얻는 모델에 활용할 수 있도록 future 인터페이스를 제공하고 있다. 
: 비동기 계산을 모델링 한는데 future를 이용할 수 있으며 future계산이 끝났을때 결과에 접근할 수 있는 레퍼런스를 제공한다. 

* 동기 API와 비동기 API 
: 동기 API => 메서드를 호출한 다음에 완료때 까지 기다렸다가 메스드를 반환 호출자는 피호출자의 동작 완료를 기다린다. 이런 상황을 블록호출 이라 한다. 
: 비동기 API => 메서드가 즉시 반환되며 끝내지 못한 작업을 호출자 스데드와 동기적으로 실행 될 수 있도록 다른 스레드에 할당한다. 이런 상황을 비블록 호출 이라 한다. 
 
* 예제 코드
~~~ java
    public class Shop {

        private final String name;
        private final Random random;

        public Shop(String name) {
            this.name = name;
            random = new Random(name.charAt(0) * name.charAt(1) * name.charAt(2));
        }

        public double getPrice(String product) {
            return calculatePrice(product);
        }

        private double calculatePrice(String product) {
            delay();
            return random.nextDouble() * product.charAt(0) + product.charAt(1);
        }

        public Future<Double> getPriceAsync(String product) {
            CompletableFuture<Double> futurePrice = new CompletableFuture<>();
            new Thread( () -> {
                        double price = calculatePrice(product);
                        futurePrice.complete(price);
            }).start();
            return futurePrice;
        }

        public String getName() {
            return name;
        }

    }
~~~


### 11.2 비동기 API 구현 

* 비동기 메서드 예제 코드 
~~~ java
    public class ShopMain {
      public static void main(String[] args) {
        Shop shop = new Shop("BestShop");
        long start = System.nanoTime();
        Future<Double> futurePrice = shop.getPriceAsync("my favorite product");
        long invocationTime = ((System.nanoTime() - start) / 1_000_000);
        System.out.println("Invocation returned after " + invocationTime 
                                                        + " msecs");
        // Do some more tasks, like querying other shops
        doSomethingElse();
        // while the price of the product is being calculated
        try {
            double price = futurePrice.get();
            System.out.printf("Price is %.2f%n", price);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        long retrievalTime = ((System.nanoTime() - start) / 1_000_000);
        System.out.println("Price returned after " + retrievalTime + " msecs");
      }

      private static void doSomethingElse() {
          System.out.println("Doing something else...");
      }

    }
~~~

* 에러 처리 방법 
~~~ java
    CompletableFuture<Double> futurePrice = new CompletableFuture<>();
    new Thread( () -> {
                try {
                    double price = calculatePrice(product);
                    futurePrice.complete(price);
                } catch (Exception ex) {
                    futurePrice.completeExceptionally(ex);
                }
    }).start();
    return futurePrice;
~~~


### 11.3 비블록 코드 만들기 

* 상점 리스트 
~~~ java
    public class BestPriceFinder {

        private final List<Shop> shops = Arrays.asList(new Shop("BestPrice"),
                                                       new Shop("LetsSaveBig"),
                                                       new Shop("MyFavoriteShop"),
                                                       new Shop("BuyItAll")/*,
                                                       new Shop("ShopEasy")*/);

        private final Executor executor = Executors.newFixedThreadPool(shops.size(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

        public List<String> findPricesSequential(String product) {
            return shops.stream()
                    .map(shop -> shop.getName() + " price is " + shop.getPrice(product))
                    .collect(Collectors.toList()); //4초
        }

        public List<String> findPricesParallel(String product) {
            return shops.parallelStream()
                    .map(shop -> shop.getName() + " price is " + shop.getPrice(product))
                    .collect(Collectors.toList()); //1초 
        }

        public List<String> findPricesFuture(String product) {
            List<CompletableFuture<String>> priceFutures =
                    shops.stream()
                    .map(shop -> CompletableFuture.supplyAsync(() -> shop.getName() + " price is "
                            + shop.getPrice(product), executor))
                    .collect(Collectors.toList());

            List<String> prices = priceFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            return prices; //2초 
        }




    }
~~~

: 상점 개수에 따라 처리시간이 달라짐
: 상점 수가 많으면 Future 가 더 빠름 

: I/O가 포람 되지 않는 계산 중심의 동작을 실행 할 때는 스트림 인터페이스가 효율적 
: I/O를 기다리는 작업을 병렬로 실행할 때는 CompletableFuture가 더 많은 유연성을 제공하며 대기/계산 의 비율에 적합한 스레드 수를 설정할 수 있다. 


### 11.4 비동기 작업 파이프라인 만들기 
> 할인 코드 추가 하기 

~~~ java
    public class Quote {

        private final String shopName;
        private final double price;
        private final Discount.Code discountCode;

        public Quote(String shopName, double price, Discount.Code discountCode) {
            this.shopName = shopName;
            this.price = price;
            this.discountCode = discountCode;
        }

        public static Quote parse(String s) {
            String[] split = s.split(":");
            String shopName = split[0];
            double price = Double.parseDouble(split[1]);
            Discount.Code discountCode = Discount.Code.valueOf(split[2]);
            return new Quote(shopName, price, discountCode);
        }

        public String getShopName() {
            return shopName;
        }

        public double getPrice() {
            return price;
        }

        public Discount.Code getDiscountCode() {
            return discountCode;
        }
    }
~~~ 

~~~ java
    public class Discount {

        public enum Code {
            NONE(0), SILVER(5), GOLD(10), PLATINUM(15), DIAMOND(20);

            private final int percentage;

            Code(int percentage) {
                this.percentage = percentage;
            }
        }

        public static String applyDiscount(Quote quote) {
            return quote.getShopName() + " price is " + Discount.apply(quote.getPrice(), quote.getDiscountCode());
        }
        private static double apply(double price, Code code) {
            delay();
            return format(price * (100 - code.percentage) / 100);
        }
    }
~~~ 

~~~ java
    public List<String> findPricesSequential(String product) {
        return shops.stream()
                .map(shop -> shop.getPrice(product))
                .map(Quote::parse)
                .map(Discount::applyDiscount)
                .collect(Collectors.toList());
    }
~~~ 


: 병렬 스트림을 이용하면 성능을 쉽게 개선할 수 있음  하지만, 스트림이 사용하는 스레드 풀의 크기가 고정되어 있어서 상점 수가 늘어났을때 처럼 검색 대상이 확장 되었을 때 유연하게 대응할 수 없음 
: 따라서 CompletableFutture에서 수행하는 태스크를 설정할 수 있는 커스텀 Executor를 정의함으로서 우리의 Cpu 사용을 극대화 할수 있음 

~~~ java
    public List<String> findPricesFuture(String product) {
        List<CompletableFuture<String>> priceFutures = findPricesStream(product)
                .collect(Collectors.<CompletableFuture<String>>toList());

        return priceFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    public Stream<CompletableFuture<String>> findPricesStream(String product) {
        return shops.stream()
                .map(shop -> CompletableFuture.supplyAsync(() -> shop.getPrice(product), executor))
                .map(future -> future.thenApply(Quote::parse))
                .map(future -> future.thenCompose(quote -> CompletableFuture.supplyAsync(() -> Discount.applyDiscount(quote), executor)));
    }
~~~ 


: thenApply 메서드는 CompletableFuture가 끝날 때까지 블록하지 않음 
: thenCompose 는 두 비동기 연상을 파이프라인으로 만들수 있도록 한다. 첫번째 연산읜 결과를 두번째 연산으로 전달 한다. 
: thenCombine 첫번째 CompletableFuture의 동작 완료와 관계없이 두번째 CompletableFuture 를 생할하여 결과를 합침 
  CompletableFuture의 결과가 생성되고 BiFunction으로 합쳐진 다음에 세번째에 CompletableFuturefmf djedmftn dlTdma 
: thenAccept 연산 결과를 소비하는 Consumer를 인수로 받는다. CompletableFuture가 생성한 결과를 어떻게 소비할지 미리 지정했으므로 CompletableFuture<Void>를 반환 한다. 



* 독릭적인 두개의 CompletableFuture 합치기
~~~ java
    public List<String> findPricesInUSD(String product) {
        List<CompletableFuture<Double>> priceFutures = new ArrayList<>();
        for (Shop shop : shops) {
            // Start of Listing 10.20.
            // Only the type of futurePriceInUSD has been changed to
            // CompletableFuture so that it is compatible with the
            // CompletableFuture::join operation below.
            CompletableFuture<Double> futurePriceInUSD = 
                CompletableFuture.supplyAsync(() -> shop.getPrice(product))
                .thenCombine(
                    CompletableFuture.supplyAsync(
                        () ->  ExchangeService.getRate(Money.EUR, Money.USD)),
                    (price, rate) -> price * rate
                );  //345 page 참고 
            priceFutures.add(futurePriceInUSD);
        }
        // Drawback: The shop is not accessible anymore outside the loop,
        // so the getName() call below has been commented out.
        List<String> prices = priceFutures
                .stream()
                .map(CompletableFuture::join)
                .map(price -> /*shop.getName() +*/ " price is " + price)
                .collect(Collectors.toList());
        return prices;
    }
~~~ 


* 자바 7로 두 Future 합치기 
~~~ java
    public List<String> findPricesInUSDJava7(String product) {
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<Double>> priceFutures = new ArrayList<>();
        for (Shop shop : shops) {
            final Future<Double> futureRate = executor.submit(new Callable<Double>() { 
                public Double call() {
                    return ExchangeService.getRate(Money.EUR, Money.USD);
                }
            });
            Future<Double> futurePriceInUSD = executor.submit(new Callable<Double>() { 
                public Double call() {
                    try {
                        double priceInEUR = shop.getPrice(product);
                        return priceInEUR * futureRate.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            });
            priceFutures.add(futurePriceInUSD);
        }
        List<String> prices = new ArrayList<>();
        for (Future<Double> priceFuture : priceFutures) {
            try {
                prices.add(/*shop.getName() +*/ " price is " + priceFuture.get());
            }
            catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return prices;
    }
    }
~~~ 


### 11.5 CompletableFuture의 종료에 대응하는 방법 
* CompletableFuture 종료에 반응하기 
~~~ java
    public void printPricesStream(String product) {
        long start = System.nanoTime();
        CompletableFuture[] futures = findPricesStream(product)
                .map(f -> f.thenAccept(s -> System.out.println(s + " (done in " + ((System.nanoTime() - start) / 1_000_000) + " msecs)")))
                .toArray(size -> new CompletableFuture[size]);
        CompletableFuture.allOf(futures).join();
        System.out.println("All shops have now responded in " + ((System.nanoTime() - start) / 1_000_000) + " msecs");
    }
~~~ 

> allOf 
: CompletableFuture 배열을 입력으로 받아 CompletableFuture<Void> 를 반환한다.
: CompletableFuture가 완료되어야 CompletableFuture<Void> 가  완료 된다. 

> anyOf 
: CompletableFuture 배열을 입력으로 받아 CompletableFuture<Object> 를 반환한다.
: CompletableFuture<Object>는 처음으로 완료한 CompletableFuture 값으로 동작을 완료한다. 

    





















### 10.3 Optional 적용 패턴
* Optional 객체 만들기  
1. 빈 Optional
: 정적 팩토리 메서드 **Optional.empty()** 로 빈 Optional 객체를 얻을 수 있다.
2. null이 아닌 값으로 Optional 만들기
: **Optional.of()** 로 null이 아닌 값을 포함하는 Optional을 만들 수 있다.
~~~java
Optional<Car> optCar = Optional.of(car);
여기서 car가 null이라면 즉시 NullPointerException이 발생한다.
~~~
3. null 값으로 Optional 만들기
: **Optional.ofNullable()** 로 null값을 저장할 수 있는 Optional을 만들 수 있다.
  
* 맵으로 Optional의 값을 추출하고 변환하기  
map 메서드를 사용해 객체의 정보를 추출할 때 null인지 확인한다.
~~~ java
Optional<Insurance> optInsurance = Optional.ofNullable(insurance);
Optional<String> name = optInsurance.map(Insurance::getName);
~~~
: Optional이 값을 포함하면 map의 인수로 제공된 함수가 값을 바꾼다.  
: Optional이 비어있으면 아무 일도 일어나지 않는다.
  
* flatMap으로 Optional 객체 연결  
**flatMap** 메서드는 이차원 스트림을 일차원 스트림으로 변환한다.  
- 잘못된 예제
~~~ java
Optional<Person> optPerson = Optional.of(person);
Optoinal<String> name =
    optPerson.map(Person::getCar)
             .map(Car::getinsurance)
             .map(Insurance::getName);
~~~
: map(Person::getCar) 연산의 결과는 Optional<Optional<Car>> 형식의 객체다.  
: 이와 같이 함수를 인수로 받아 다른 스트림으로 반환하는 경우 스트림과 같이 flatMap을 사용한다.
  
- 정상적인 예제  
~~~ java
public String getCarInsuranceName(Optional<Person> person) {
    return person.flatMap(Person::getCar)
                 .flatMap(Car::getInsurance)
                 .map(Insurance::getName)
                 .orElse("Unknown");
}
~~~
: flatMap 연산 수행 시 빈 Optional에 호출하면 아무 일도 일어나지 않는다.  
: Optional이 Person을 감싸고 있다면 flatMap에 적용된 Function이 Person에 적용된다.
  
* 디폴트 액션과 Optional 언랩  
1. get()
: 값이 있으면 값을 반환하고 없으면 NullPointerException을 발생시킨다. 값에 확신이 없다면 사용하지 않는 것을 추천
2. orElse(T other)
: Optional이 값을 포함하지 않을 때 디폴트값을 제공한다.
3. orElseGet(Supplier<? extends T> other)
: Optional에 값이 없을때만 Supplier가 실행된다. 디폴트 메서드를 만드는데 시간이 걸리거나 비어있을 때만 생성하고 싶으면 사용한다.
4. orElseThrow(Supplier<? extends X> exceptionSupplier)
: Optional이 비어있을 때 예외를 발생시키며, 발생시킬 예외의 종류를 선택할 수 있다.
5. ifPresent(Consumer<? super T> consumer)
: 값이 존재할 때 인수로 넘겨준 동작을 실행할 수 있다. 없으면 아무 일도 일어나지 않는다.
  
* 두 Optional 합치기  
- isPresent를 이용한 null-safe Method  
~~~ java
if(person.isPresent() && car.isPresent()){
    return Optional.of(findCheapestInsurance(preson.get()), car.get()));
} else {
    return Optional.empty();
}
~~~
  
- 조건문 없이 nullSafeFindCheapestInsurance() 구현하기  
~~~ java
public Optional<Insurance> nullSafeFindCheapestInsurance(
                                Optinoal<Person> person, Optional<Car> car) {
    return person.flatMap(p -> car.map(c -> findCheapestInsurance(p,c)));
}
~~~
  
* 필터로 특정 값 거르기
**filter** 메서드는
Predicate를 인수로 받아 일치하면 그 값을, 그렇지 않으면 빈 Optional 객체를 반환한다.  
Optional에 값이 있으면 그 값에 Predicate를 적용한다. 결과가 true면 변화가 없지만 false면 빈 상태가 된다.
  
**p329 Optional 클래스의 메서드 참고**   
  
### 10.4 Optional을 사용한 실용 예제
* 잠재적으로 null이 될 수 있는 대상을 Optional로 감싸기  
~~~ java
Object value = map.get("key");    //key에 해당하는 값이 없으면 null 반환  
Optional<Object> value = Optional.ofNullable(map.get("key")); //Optional 객체로 감싸기
~~~
  
* 예외와 Optional
자바 API는 값을 제공할 수 없을 때 null 반환 대신 예외를 발생시킨다.  
대표적인 예로 문자열을 정수로 바꾸지 못하면 NumberFormatException을 발생시킨다.  
이 경우 문자열을 정수 Optional로 반환하는 유틸리티 메서드를 구현하여 사용할 수 있다.  
  
* 기본형 Optional과 이를 사용하지 말아야 하는 이유  
Optinoal도 기본형으로 특화된 OptionalInt, OptionalLong, OptionalDouble을 제공한다.  
단, 이 경우 1)Optional 클래스의 map, flatMap, filter 등을 지원하지 않고, 2)일반 Optional과 혼용할 수 없으므로 권장하지 않는다.
