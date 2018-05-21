package br.org.certi.jocd.debug.breakpoints;

import br.org.certi.jocd.core.Target;
import java.util.ArrayList;
import java.util.List;

public class SoftwareBreakpointProvider extends BreakpointProvider {

  private Target core;
  private List<Breakpoint> breakpoints = new ArrayList<Breakpoint>();

  public SoftwareBreakpointProvider(Target core) {
    super();
    this.core = core;
  }

  @Override
  public void init() {
  }

  @Override
  public boolean doFilterMemory() {
    return true;
  }

  @Override
  public long filterMemory(long address, int size, long word) {
    for (Breakpoint bp : this.breakpoints) {
      switch (size) {
        case 16:
          if (bp.address == address) {
            word = bp.originalInstr;
          }
          break;

        case 32:
          if (bp.address == address) {
            word = (word & 0xFFFF0000L) | bp.originalInstr;
          } else if (bp.address == address + 2) {
            word = (word & 0xFFFFL) | (bp.originalInstr << 16);
          }
          break;
      }
    }
    return word;
  }

  @Override
  public byte filterMemory(long address, byte data) {
    for (Breakpoint bp : this.breakpoints) {
      if (bp.address == address) {
        data = (byte) (bp.originalInstr & 0xFFL);
      } else if (bp.address + 1 == address) {
        data = (byte) (bp.originalInstr >> 8);
      }
    }
    return data;
  }
}
