package br.org.certi.jocd.coresight;

public class AccessPort {

  public AccessPort(DebugPort dp, int apNum) {

  }

  public void init(Boolean busAccessible) {
    // Set default value if null.
    if (busAccessible == null) {
      busAccessible = true;
    }
  }

  public void writeMemory(long address, long value, Integer transferSize) {
    // TODO
  }

  public long readMemory(long address, Integer transferSize) {
    return readMemoryNow(address, transferSize);
  }

  public long readMemoryNow(long address, Integer transferSize) {
    // TODO
    return 0L;
  }

  public void readMemoryLater(long address, Integer transferSize) {
    // TODO
  }

  public byte[] readBlockMemoryUnaligned8(long address, long size) {
    // TODO
    return null;
  }

  public long[] readBlockMemoryAligned32(long address, int size) {
    // TODO
    return null;
  }

  public void writeBlockMemoryUnaligned8(long address, byte[] data) {
  }

  public void writeBlockMemoryAligned32(long address, long[] words) {
  }
}
