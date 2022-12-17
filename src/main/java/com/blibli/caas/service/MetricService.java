package com.blibli.caas.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.URISyntaxException;

public interface MetricService {
  String getStatsResponse() throws URISyntaxException, JsonProcessingException;
}
