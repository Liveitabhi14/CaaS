package com.blibli.caas.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeStats implements Serializable {
  private String host;
  private String port;
  private String nodeId;
  private double usedMemory;
  private double totalMemory;
  private double usedCPU;
  private double totalCPU;
  private boolean isSlave;
  private String masterHost;
  private String masterPort;
  private double instantaneousOpsPerSec;
  private double instantaneousInputKbps;
  private double instantaneousOutputKbps;
  private double keyspaceMisses;
  private double keyspaceHits;
  private int slots;
}
