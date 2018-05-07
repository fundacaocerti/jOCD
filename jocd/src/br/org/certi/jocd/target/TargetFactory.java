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
package br.org.certi.jocd.target;

import android.util.Log;

import br.org.certi.jocd.core.Target;
import br.org.certi.jocd.target.nrf51822.Nrf51;

public class TargetFactory {

    // Logging
    static final String TAG = "TargetFactory";

    public static enum targetEnum {
        cortex_m,    // Not implemented.
        kinetis,     // Not implemented.
        ke15z7,      // Not implemented.
        ke18f16,     // Not implemented.
        kl02z,       // Not implemented.
        kl05z,       // Not implemented.
        kl25z,       // Not implemented.
        kl26z,       // Not implemented.
        kl27z4,      // Not implemented.
        kl28z,       // Not implemented.
        kl43z4,      // Not implemented.
        kl46z,       // Not implemented.
        kl82z7,      // Not implemented.
        kv10z7,      // Not implemented.
        kv11z7,      // Not implemented.
        kw01z4,      // Not implemented.
        kw40z4,      // Not implemented.
        kw41z4,      // Not implemented.
        k20d50m,     // Not implemented.
        k22fa12,     // Not implemented.
        k22f,        // Not implemented.
        k28f15,      // Not implemented.
        k64f,        // Not implemented.
        k66f18,      // Not implemented.
        k82f25615,   // Not implemented.
        lpc800,      // Not implemented.
        lpc11u24,    // Not implemented.
        lpc1768,     // Not implemented.
        lpc4330,     // Not implemented.
        nrf51,
        nrf52,       // Not implemented.
        stm32f103rc, // Not implemented.
        stm32f051,   // Not implemented.
        maxwsnenv,   // Not implemented.
        max32600mbed,// Not implemented.
        w7500,       // Not implemented.
        lpc11xx_32,  // Not implemented.
        lpc824,      // Not implemented.
        lpc54114,    // Not implemented.
        lpc4088,     // Not implemented.
        ncs36510,    // Not implemented.
        lpc4088qsb,  // Not implemented.
        lpc4088dm,   // Not implemented.
        rtl8195am    // Not implemented.
    }

    public static Target getTarget(targetEnum target) {
        switch (target) {
            case nrf51: return new Nrf51();
            default:
                Log.e(TAG,"Default case on switch GetTarget. "
                        + "Unexpected interface: " + target.toString());
        }
        return null;
    }
}
