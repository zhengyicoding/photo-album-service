package com.example.photoalbum.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlbumController {
  @GetMapping("/")
  public String hello() {
    return "Photo Album Service is running";
  }
}
