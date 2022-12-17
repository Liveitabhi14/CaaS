package com.blibli.caas.service;

public interface ExecuteCommandOnRemoteMachineService {

  String executeCommandOnRemoteMachine(String command, String targetHost, String targetPort,
      String username, String password);
}
