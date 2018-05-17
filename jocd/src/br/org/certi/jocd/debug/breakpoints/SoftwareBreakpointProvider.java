package br.org.certi.jocd.debug.breakpoints;

import br.org.certi.jocd.core.Target;
import java.util.ArrayList;
import java.util.List;

public class SoftwareBreakpointProvider extends BreakpointProvider {

  private Target core;
  private List<Breakpoint> Breakpoints = new ArrayList<Breakpoint>();

  public SoftwareBreakpointProvider(Target core) {
    super();
    this.core = core;
  }

  @Override
  public void init() {
  }

  // TODO
}
