package com.quantum.axe;

import java.util.List;

public class AxeTestResultContainer {
  public String url;
  public String timestamp;
  public List<AxeTestResult> passes;
  public List<AxeTestResult> violations;
  public List<AxeTestResult> inapplicable;
  public List<AxeTestResult> incomplete;
}

