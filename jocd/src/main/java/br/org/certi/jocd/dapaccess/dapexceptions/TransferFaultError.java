/*
 * Copyright 2018 Fundação CERTI
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package br.org.certi.jocd.dapaccess.dapexceptions;


/*
 * A SWD Fault occurred
 */
public class TransferFaultError extends TransferError {

  private Long address;

  public TransferFaultError() {
    this("The host debugger reported failure for the given command", null);
  }

  public TransferFaultError(String message, Long faultAddress) {
    super(message);
    this.address = faultAddress;
  }

  public Long getFaultAddress() {
    return this.address;
  }

  public void setFaultAddress(long address) {
    this.address = address;
  }

  public String toString() {
    String desc = "SWD/JTAG Transfer Fault";
    if (this.address != null) {
      desc += " @ " + String.format("0x%08x", address);
    }
    return desc;
  }

}
