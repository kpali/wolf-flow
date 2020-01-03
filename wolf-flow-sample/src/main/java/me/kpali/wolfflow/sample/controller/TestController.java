package me.kpali.wolfflow.sample.controller;

import me.kpali.wolfflow.sample.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/test")
@RestController
public class TestController {

    @Autowired
    TestService testService;

    @GetMapping
    public void test() {
        testService.test();
    }

}
