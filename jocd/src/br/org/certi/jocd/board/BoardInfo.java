/*
 * Copyright 2018 Fundação CERTI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package br.org.certi.jocd.board;

import br.org.certi.jocd.target.TargetFactory;

public class BoardInfo {

    // Logging
    private static final String TAG = "BoardInfo";

    public final String name;
    public final TargetFactory.targetEnum target;
    public final String binary;

    /*
     * Constructor.
     */
    public BoardInfo(String boardName, TargetFactory.targetEnum target, String testBinary) {
        this.name = boardName;
        this.target = target;
        this.binary = testBinary;
    }

    public static BoardInfo getBoardInfo(String boardId) {
        switch (boardId) {
            // Note: please keep board list sorted by ID!
            //
            // Board ID                                  Board Name                       Target                                      Test Binary
            case "0200": return new BoardInfo("FRDM-KL25Z",                     TargetFactory.targetEnum.kl25z,        "l1_kl25z.bin"           );
            case "0201": return new BoardInfo("FRDM-KW41Z",                     TargetFactory.targetEnum.kw41z4,       "l1_kw41z4.bin"          );
            case "0202": return new BoardInfo("USB-KW41Z",                      TargetFactory.targetEnum.kw41z4,       "l1_kw41z4.bin"          );
            case "0203": return new BoardInfo("TWR-KL28Z72M",                   TargetFactory.targetEnum.kl28z,        "l1_kl28z.bin"           );
            case "0204": return new BoardInfo("FRDM-KL02Z",                     TargetFactory.targetEnum.kl02z,        "l1_kl02z.bin"           );
            case "0205": return new BoardInfo("FRDM-KL28Z",                     TargetFactory.targetEnum.kl28z,        "l1_kl28z.bin"           );
            case "0215": return new BoardInfo("FRDM-KL28ZEM",                   TargetFactory.targetEnum.kl28z,        "l1_kl28z.bin"           );
            case "0206": return new BoardInfo("TWR-KE18F",                      TargetFactory.targetEnum.ke18f16,      "l1_ke18f16.bin"         );
            case "0210": return new BoardInfo("FRDM-KL05Z",                     TargetFactory.targetEnum.kl05z,        "l1_kl05z.bin"           );
            case "0213": return new BoardInfo("FRDM-KE15Z",                     TargetFactory.targetEnum.ke15z7,       "l1_ke15z7.bin"          );
            case "0214": return new BoardInfo("Hexiwear",                       TargetFactory.targetEnum.k64f,         "l1_k64f.bin"            );
            case "0216": return new BoardInfo("HVP-KE18F",                      TargetFactory.targetEnum.ke18f16,      "l1_ke18f16.bin"         );
            case "0217": return new BoardInfo("FRDM-K82F",                      TargetFactory.targetEnum.k82f25615,    "l1_k82f.bin"            );
            case "0218": return new BoardInfo("FRDM-KL82Z",                     TargetFactory.targetEnum.kl82z7,       "l1_kl82z.bin"           );
            case "0220": return new BoardInfo("FRDM-KL46Z",                     TargetFactory.targetEnum.kl46z,        "l1_kl46z.bin"           );
            case "0224": return new BoardInfo("FRDM-K28F",                      TargetFactory.targetEnum.k28f15,       "l1_k28f.bin"            );
            case "0230": return new BoardInfo("FRDM-K20D50M",                   TargetFactory.targetEnum.k20d50m,      "l1_k20d50m.bin"         );
            case "0231": return new BoardInfo("FRDM-K22F",                      TargetFactory.targetEnum.k22f,         "l1_k22f.bin"            );
            case "0240": return new BoardInfo("FRDM-K64F",                      TargetFactory.targetEnum.k64f,         "l1_k64f.bin"            );
            case "0260": return new BoardInfo("FRDM-KL26Z",                     TargetFactory.targetEnum.kl26z,        "l1_kl26z.bin"           );
            case "0261": return new BoardInfo("FRDM-KL27Z",                     TargetFactory.targetEnum.kl27z4,       "l1_kl27z.bin"           );
            case "0262": return new BoardInfo("FRDM-KL43Z",                     TargetFactory.targetEnum.kl43z4,       "l1_kl26z.bin"           );
            case "0290": return new BoardInfo("FRDM-KW40Z",                     TargetFactory.targetEnum.kw40z4,       "l1_kw40z.bin"           );
            case "0298": return new BoardInfo("FRDM-KV10Z",                     TargetFactory.targetEnum.kv10z7,       "l1_kl25z.bin"           );
            case "0300": return new BoardInfo("TWR-KV11Z75M",                   TargetFactory.targetEnum.kv11z7,       "l1_kl25z.bin"           );
            case "0311": return new BoardInfo("FRDM-K66F",                      TargetFactory.targetEnum.k66f18,       "l1_k66f.bin"            );
            case "0320": return new BoardInfo("FRDM-KW01Z9032",                 TargetFactory.targetEnum.kw01z4,       "l1_kl26z.bin"           );
            case "0321": return new BoardInfo("USB-KW01Z",                      TargetFactory.targetEnum.kw01z4,       "l1_kl25z.bin"           );
            case "0324": return new BoardInfo("USB-KW40Z",                      TargetFactory.targetEnum.kw40z4,       "l1_kl25z.bin"           );
            case "0400": return new BoardInfo("maxwsnenv",                      TargetFactory.targetEnum.maxwsnenv,    "l1_maxwsnenv.bin"       );
            case "0405": return new BoardInfo("max32600mbed",                   TargetFactory.targetEnum.max32600mbed, "l1_max32600mbed.bin"    );
            case "0824": return new BoardInfo("LPCXpresso824-MAX",              TargetFactory.targetEnum.lpc824,       "l1_lpc824.bin"          );
            case "1054": return new BoardInfo("LPCXpresso54114-MAX",            TargetFactory.targetEnum.lpc54114,     "l1_lpc54114.bin"        );
            case "1010": return new BoardInfo("mbed NXP LPC1768",               TargetFactory.targetEnum.lpc1768,      "l1_lpc1768.bin"         );
            case "1017": return new BoardInfo("mbed HRM1017",                   TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            case "1018": return new BoardInfo("Switch-Science-mbed-LPC824",     TargetFactory.targetEnum.lpc824,       "l1_lpc824.bin"          );
            case "1019": return new BoardInfo("mbed TY51822r3",                 TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            case "1040": return new BoardInfo("mbed NXP LPC11U24",              TargetFactory.targetEnum.lpc11u24,     "l1_lpc11u24.bin"        );
            case "1050": return new BoardInfo("NXP LPC800-MAX",                 TargetFactory.targetEnum.lpc800,       "l1_lpc800.bin"          );
            case "1060": return new BoardInfo("EA-LPC4088",                     TargetFactory.targetEnum.lpc4088qsb,   "l1_lpc4088qsb.bin"      );
            case "1062": return new BoardInfo("EA-LPC4088-Display-Module",      TargetFactory.targetEnum.lpc4088dm,    "l1_lpc4088dm.bin"       );
            case "1070": return new BoardInfo("nRF51822-mKIT",                  TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            case "1080": return new BoardInfo("DT01 + MB2001",                  TargetFactory.targetEnum.stm32f103rc,  "l1_stm32f103rc.bin"     );
            // TODO: we have to board with the same ID 1090. Check each one is right.
            // case "1090": return new BoardInfo("DT01 + MB00xx",                  TargetFactory.targetEnum.stm32f051,    "l1_stm32f051.bin"       );
            // case "1090": return new BoardInfo("RedBearLab-nRF51822",            TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            case "1095": return new BoardInfo("RedBearLab-BLE-Nano",            TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            case "1100": return new BoardInfo("nRF51-DK",                       TargetFactory.targetEnum.nrf51,        "l1_nrf51-dk.bin"        );
            case "1101": return new BoardInfo("nRF52-DK",                       TargetFactory.targetEnum.nrf52,        "l1_nrf52-dk.bin"        );
            case "1200": return new BoardInfo("NCS36510-EVK",                   TargetFactory.targetEnum.ncs36510,     "l1_ncs36510-evk.bin"    );
            case "1114": return new BoardInfo("mbed LPC1114FN28",               TargetFactory.targetEnum.lpc11xx_32,   "l1_mbed_LPC1114FN28.bin");
            case "1120": return new BoardInfo("nRF51-Dongle",                   TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            case "1234": return new BoardInfo("u-blox-C027",                    TargetFactory.targetEnum.lpc1768,      "l1_lpc1768.bin"         );
            case "1600": return new BoardInfo("Bambino 210",                    TargetFactory.targetEnum.lpc4330,      "l1_lpc4330.bin"         );
            case "1605": return new BoardInfo("Bambino 210E",                   TargetFactory.targetEnum.lpc4330,      "l1_lpc4330.bin"         );
            case "2201": return new BoardInfo("WIZwik_W7500",                   TargetFactory.targetEnum.w7500,        "l1_w7500mbed.bin"       );
            case "4600": return new BoardInfo("Realtek RTL8195AM",              TargetFactory.targetEnum.rtl8195am,    "l1_rtl8195am.bin"       );
            case "7402": return new BoardInfo("mbed 6LoWPAN Border Router HAT", TargetFactory.targetEnum.k64f,         "l1_k64f.bin"            );
            case "9004": return new BoardInfo("Arch Pro",                       TargetFactory.targetEnum.lpc1768,      "l1_lpc1768.bin"         );
            case "9009": return new BoardInfo("Arch BLE",                       TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            case "9012": return new BoardInfo("Seeed Tiny BLE",                 TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            case "9900": return new BoardInfo("Microbit",                       TargetFactory.targetEnum.nrf51,        "l1_microbit.bin"        );
            case "C004": return new BoardInfo("tinyK20",                        TargetFactory.targetEnum.k20d50m,      "l1_k20d50m.bin"         );
            case "C006": return new BoardInfo("VBLUno51",                       TargetFactory.targetEnum.nrf51,        "l1_nrf51.bin"           );
            default: return null;
        }
    }
}
