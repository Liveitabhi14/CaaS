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
  private long usedMemory;
  private long totalMemory;
  private double usedCPU;
  private double totalCPU;
  private boolean isSlave;
  private String masterHost;
  private String masterPort;
  private int slots;
}
