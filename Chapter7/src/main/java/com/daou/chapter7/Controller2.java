package com.daou.chapter7;

import org.junit.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.LongStream;

@RestController
public class Controller2 {
    @GetMapping("/list")
    public List<String> list() {
        List<String> listToReturn = new ArrayList<>();

        List<Long> list = new ArrayList<>();
        LongStream.rangeClosed(1, 10000).parallel().forEach(list::add);
        listToReturn.add("ArrayList size: " + list.size());

        List<Long> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        LongStream.rangeClosed(1, 10000).parallel().forEach(copyOnWriteArrayList::add);
        listToReturn.add("CopyOnWriteArrayList size: " + copyOnWriteArrayList.size());

        return listToReturn;
    }
}
