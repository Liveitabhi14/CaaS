package com.blibli.caas.serviceImpl;

import com.blibli.caas.service.ExecuteCommandOnRemoteMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ExecuteCommandOnRemoteMachineServiceImpl implements
    ExecuteCommandOnRemoteMachineService {

  @Override
  public String executeCommandOnRemoteMachine(String command, String targetHost, String targetPort) {
    return "";
  }
}
