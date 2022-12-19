package com.blibli.caas.service;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

import java.util.List;

public interface ClusterService {

  String addNewNodeToCLuster(String newRedisHost, String newRedisPort, String clusterHost,
      String clusterPort, boolean isSlave, String masterId, Boolean isNeedRebalance,
      String username, String password);

  String clusterRebalance(String clusterHost, String clusterPort, Boolean isEmptySlotReBalance,
      String username, String password);

  String reshardHashSlotsBetweenNodes(String clusterHost, String clusterPort, String sourceNodeId,
      String targetNodeId, int noOfSlots, String username, String password);

  String deleteNodeFromCluster(String clusterHost, String clusterPort, String deleteNodeHost,
      Integer deleteNodePort, String username, String password);

  String getClusterInfo(String clusterHost, Integer clusterPort);

  String getNodeIdInCluster(String nodeHost, Integer nodePort);

  Integer countSlotsInNode(String clusterHost, Integer clusterPort, String nodeId);

  String clusterResetHard(String clusterHost, int clusterPort);

  List<RedisClusterNode> getClusterNode();

  String flushDb(String clusterHost, int clusterPort);
}
