package com.blibli.caas.service;

public interface ClusterService {
  String addNewNodeToCLuster(String newRedisHost, String newRedisPort, String clusterHost,
      String clusterPort, boolean isSlave, String masterId, Boolean isNeedRebalance);

  String clusterRebalance(String clusterHost, String clusterPort, Boolean isEmptySlotReBalance);
}
