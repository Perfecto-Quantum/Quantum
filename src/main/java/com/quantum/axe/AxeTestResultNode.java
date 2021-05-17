package com.quantum.axe;

import java.util.List;

public class AxeTestResultNode {
  public String html;
  public String impact;
  public List<String> target;
  public String failureSummary;
  public List<AxeTestResultCheckDetail> any;
  public List<AxeTestResultCheckDetail> all;
  public List<AxeTestResultCheckDetail> none;
}

