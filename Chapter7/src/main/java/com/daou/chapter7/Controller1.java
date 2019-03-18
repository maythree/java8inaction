package com.daou.chapter7;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

@RestController
public class Controller1 {
    @GetMapping("/atomic")
    public List<String> test0() {
        return Arrays.asList(test1(), test2(), test3(), test4(), test5());
    }

    @GetMapping("/atomic1")
    public String test1() {
        // AtomicLong
        Accumulator accumulator = new Accumulator();
        LongStream.rangeClosed(1, 50000).parallel().forEach(accumulator::add);
        return "AtomicLong: " + accumulator.total;
    }

    @GetMapping("/atomic2")
    public String test2() {
        // volatile long with serial
        Accumulator2 accumulator2 = new Accumulator2();
        LongStream.rangeClosed(1, 50000).forEach(accumulator2::add);
        return "volatile long with serial: " + accumulator2.total;
    }

    @GetMapping("/atomic3")
    public String test3() {
        Accumulator2 accumulator21 = new Accumulator2();
        LongStream.rangeClosed(1, 50000).parallel().forEach(accumulator21::add);
        return "volatile long with parallel: " + accumulator21.total;
    }

    @GetMapping("/atomi4")
    public String test4() {
        // static long
        Accumulator3 accumulator3 = new Accumulator3();
        LongStream.rangeClosed(1, 50000).parallel().forEach(accumulator3::add);
        return "static long: " + accumulator3.total;
    }

    @GetMapping("/atomic5")
    public String test5() {
        // static volatile long
        Accumulator4 accumulator4 = new Accumulator4();
        LongStream.rangeClosed(1, 50000).parallel().forEach(accumulator4::add);
        return "static volatile long: " + accumulator4.total;
    }
}
