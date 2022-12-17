package com.blibli.caas.impl;

import com.blibli.caas.model.Root;
import com.blibli.caas.model.Stats;
import com.blibli.caas.service.MetricService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@Service
@Slf4j
public class MetricServiceImpl implements MetricService {


  @Value("${external.rest.api.host}") private String host;

  @Autowired private ObjectMapper objectMapper;

  @Override
  public String getStatsResponse() throws JsonProcessingException {
    RestTemplate restTemplate = new RestTemplate();

    ResponseEntity<String> response = restTemplate.getForEntity(
        host + "stats?filter=&format=json&type=Counters&histogram_buckets=none", String.class);

    Root root = this.objectMapper.readValue(response.getBody(), new TypeReference<>() {
    });

    for (Stats stat : root.getStats()) {
      log.info(stat.toString());
    }
    return "true";
  }
}
