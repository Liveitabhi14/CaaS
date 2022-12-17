package com.blibli.caas.controller;

import com.blibli.caas.service.SshCommandExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ssh")
@Slf4j
public class SshConnectionTestController {

  public static final String TEST_CONNECTION = "/testConnection";

  @Autowired
  private SshCommandExecutorService sshCommandExecutorService;

  @GetMapping(TEST_CONNECTION + "/executeCommand")
  public String testSshConnectionWithCommand(@RequestParam String host, @RequestParam Integer port,
      @RequestParam String username, @RequestParam String password, @RequestParam String command)
      throws Exception {

    return sshCommandExecutorService.executeCommandOnRemoteMachineViaSSH(host, port, username,
        password, command);
  }
}
