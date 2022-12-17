package com.blibli.caas.service;

import redis.clients.jedis.HostAndPort;

public interface ExecuteCommandOnRemoteMachineService {

  String executeCommandOnRemoteMachine(String command, String targetHost, String targetPort);
}
