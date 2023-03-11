package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class HelloWorldController {
	
	@GetMapping("/wish")
	public String wish() {
		log.info("within wish method");
		return "Hello World";
	}
	

}
