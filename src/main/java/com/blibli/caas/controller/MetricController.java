package com.blibli.caas.controller;

import com.blibli.caas.service.MetricService;
import javafx.collections.ObservableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;

@RestController
@RequestMapping(value = "metric")
@Slf4j
public class MetricController {

  @Autowired
  private MetricService metricService;


  @GetMapping(value = "/stats")
  public String getStats() {
    try {
      return metricService.getStatsResponse();
    } catch (Exception e) {
      log.info("something", e);
    }
    return "true";
  }

}
