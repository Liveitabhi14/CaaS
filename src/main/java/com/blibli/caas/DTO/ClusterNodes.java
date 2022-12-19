package com.blibli.caas.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.BitSet;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClusterNodes implements Serializable {

  private String nodeHostPort;
  private String nodeId;
  private long slots;
  private Double memoryUsage;
  private double usedMemory;
  private double totalMemory;
  private String masterNodeHostPort;
  private boolean isSlave;
  private String role;
  private long currentTime;

}
