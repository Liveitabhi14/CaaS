package com.blibli.caas.constant;

public class CommandsKeyword {

  public static final String REDIS_CLI = "redis-cli";
  public static final String CLUSTER = "--cluster";
  public static final String REDIS_CLI_CLUSTER = REDIS_CLI + " " + CLUSTER;
  public static final String ADD_NODE = "add-node";
  public static final String CLUSTER_SLAVE = "--cluster-slave";
  public static final String CLUSTER_MASTER_ID = "--cluster-master-id";

  public static final String REBALANCE = "rebalance";

  public static final String USE_EMPTY_MASTER="--cluster-use-empty-masters";

  public static final String RESHARD = "reshard";
  public static final String CLUSTER_FROM = "--cluster-from";
  public static final String CLUSTER_TO = "--cluster-to";
  public static final String CLUSTER_SLOTS = "--cluster-slots";
  public static final String CLUSTER_YES = "--cluster-yes";

  public static final String DELETE_NODE = "del-node";
}
