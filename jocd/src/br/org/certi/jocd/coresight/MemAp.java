package br.org.certi.jocd.coresight;

public class MemAp extends AccessPort {

  public MemAp(DebugPort dp, int apNum) {
    super(dp, apNum);
  }

  @Override
  public void init(Boolean busAccessible) {
    super.init(busAccessible);
  }
}
