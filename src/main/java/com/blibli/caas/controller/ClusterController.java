package com.blibli.caas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/cluster")
public class ClusterController {

  @GetMapping(value = "/addNewNode")
  public boolean addNewNodeToRedisController(@RequestParam String newRedisHost,
      @RequestParam String newRedisPort) {
    return true;
  }
}
