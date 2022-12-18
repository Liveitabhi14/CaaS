package com.blibli.caas.service;

import org.springframework.stereotype.Service;

public interface MetricService {
   void checkNodeMemory(String userName, String password);
   void scheduleNodeCheck() throws InterruptedException;
}
