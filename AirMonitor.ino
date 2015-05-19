#include <I2C.h>
#include <avr/wdt.h>
#include <avr/sleep.h>
#include <avr/power.h>
#include <SoftwareSerial.h>
#include "BGLib.h"

#define TEMP_HUMD_ADDRESS           0x28
#define I2C_POLLING_DELAY           1
#define VOC_MEASURE_ENABLE_PIN      7
#define MEASURE_WAKEUP_PIN          8
#define PM_LED_PIN                  9

#define BLE_STATE_STANDBY           0
#define BLE_STATE_SCANNING          1
#define BLE_STATE_ADVERTISING       2
#define BLE_STATE_CONNECTING        3
#define BLE_STATE_CONNECTED_MASTER  4
#define BLE_STATE_CONNECTED_SLAVE   5
#define GATT_HANDLE_C_RX_DATA       17  // 0x11, supports "write" operation
#define GATT_HANDLE_C_TX_DATA       20  // 0x14, supports "read" and "indicate" operations

// Toggle for debug serial output
// #define DEBUG

// Toggle for mocked sensor inputs
// #define MOCK

// BLE state/link status tracker
uint8_t ble_state = BLE_STATE_STANDBY;
uint8_t ble_encrypted = 0;  // 0 = not encrypted, otherwise = encrypted
uint8_t ble_bonding = 0xFF; // 0xFF = no bonding, otherwise = bonding handle

// use SoftwareSerial on pins D3/D4 for RX/TX (Arduino side)
SoftwareSerial bleSerialPort(3, 4);

// create BGLib object:
//  - use SoftwareSerial por for module comms
//  - use nothing for passthrough comms (0 = null pointer)
//  - enable packet mode on API protocol since flow control is unavailable
BGLib ble112((HardwareSerial *)&bleSerialPort, 0, 1);
#define BGAPI_GET_RESPONSE(v, dType) dType *v = (dType *)ble112.getLastRXPayload()

// Sensor data
unsigned int pm;
unsigned int voc;
unsigned int temp;
unsigned int humd;

// Bluetooth attributes
uint8 bluetooth_data[6] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

// State of Bluetooth connection
boolean bluetoothConnected;

// Watchdog timer ISR definition in interrupt vector table
ISR(WDT_vect)  {
  wdt_disable();  // Disable watchdog timer
}

void setup() {
    resetWatchdogTimer ();  // do this first in case WDT fires
    I2c.begin(); // Opens & joins the irc bus as master
    delay(100); // Waits to make sure everything is powered up before sending or receiving data
    I2c.timeOut(500); // Sets a timeout to ensure no locking up of sketch if I2C communication fails

    #ifndef MOCK
        // Enable VOC sensor
        pinMode(VOC_MEASURE_ENABLE_PIN, OUTPUT);
        digitalWrite(VOC_MEASURE_ENABLE_PIN, LOW);

        // Enable PM sensor
        pinMode(PM_LED_PIN, OUTPUT);
        digitalWrite(PM_LED_PIN, HIGH);
    #endif

    // Enable button-based wakeup
    pinMode(MEASURE_WAKEUP_PIN, INPUT_PULLUP);
    pciSetup(8);

    #ifdef DEBUG
        Serial.begin(38400);
        while (!Serial);
    #endif
    bluetoothSetup();
}

// Sets up pin change ISR for the specified pin number
void pciSetup(byte pin)  {
    *digitalPinToPCMSK(pin) |= bit (digitalPinToPCMSKbit(pin));  // enable pin
    PCIFR |= bit (digitalPinToPCICRbit(pin)); // clear any outstanding interrupt
    PCICR |= bit (digitalPinToPCICRbit(pin)); // enable interrupt for the group
}

void loop() {
    if (digitalRead(MEASURE_WAKEUP_PIN)) {
        #ifdef DEBUG
            Serial.print("Entering sleep...");
        #endif

        delay(100);
        enterSleep();

        #ifdef DEBUG
            Serial.println("Woke up!");
        #endif
    }

    // Keep polling for new data from BLE
    ble112.checkActivity();

    #ifdef MOCK
        pm   += 1;
        voc  += 1;
        temp += 1;
        humd += 1;
    #else
        // Acquire data from sensors
        printTempHumd();
        printVOC();
        printPM();
    #endif

    #ifdef DEBUG
        Serial.println(temp, BIN);
        Serial.println(humd, BIN);
        Serial.println(voc, BIN);
        Serial.println(pm, BIN);
        Serial.println();
    #endif

    formatBluetoothData();

    uint8_t result = ble112.ble_cmd_attributes_write(GATT_HANDLE_C_TX_DATA,
        0, 6, (const uint8*) bluetooth_data);

    delay(500);
}

/**
 * Stuffs the sensor data into 6-bytes:
 * 14-bits Temperature
 * 14-bits Humidity
 * 10-bits VOC
 * 10-bits PM
 */
void formatBluetoothData()  {
    bluetooth_data[5] = pm & 0xFF;
    bluetooth_data[4] = (pm >> 8) & B00000011;

    bluetooth_data[4] |= ((voc << 2) & 0xFF);
    bluetooth_data[3] = ((voc >> 6) & B0001111);

    bluetooth_data[3] |= ((humd << 4) & 0xFF);
    bluetooth_data[2] = (humd >> 4) & 0xFF;
    bluetooth_data[1] = (humd >> 12) & B00000011;

    bluetooth_data[1] |= ((temp << 2) & 0xFF);
    bluetooth_data[0] = (temp >> 6) & 0xFF;
}

void printPM()  {
    getPM();

    #ifdef DEBUG
        Serial.print("PM:   ");
        delay(3);
        Serial.print(pm / 10.23);
        delay(2);
        Serial.println("%");
        delay(1);
    #endif
}

void printTempHumd()  {
    getTempHumd();

    #ifdef DEBUG
        Serial.print("TEMP: ");
        delay(3);
        Serial.print(temp / (float)0x4000 * 165 - 40);
        delay(2);
        Serial.println(" C");
        delay(1);

        Serial.print("HUMD: ");
        delay(3);
        Serial.print(humd / (float)0x4000 * 100);
        delay(2);
        Serial.println("%");
        delay(1);
    #endif
}

void printVOC()  {
    getVOC();

    #ifdef DEBUG
        Serial.print("VOC:  ");
        delay(3);
        Serial.print(voc / 10.23);
        delay(2);
        Serial.println("%");
        delay(1);
    #endif
}

void getPM() {
    digitalWrite(PM_LED_PIN, LOW);
    delayMicroseconds(280);

    pm = analogRead(3);
    delayMicroseconds(40);

    digitalWrite(PM_LED_PIN, HIGH);
    delayMicroseconds(9680);
}

void getVOC()  {
    digitalWrite(VOC_MEASURE_ENABLE_PIN, HIGH);
    delay(50);

    voc = analogRead(1);
    digitalWrite(VOC_MEASURE_ENABLE_PIN, LOW);
}

int getTempHumd()  {
    byte data[4];
    uint8_t nackack = 100; // Setup variable to hold ACK/NACK resopnses

    // Measurement request
    nackack = I2c.write(TEMP_HUMD_ADDRESS, 0x01);
    delay(55); // Acquisition Time
    nackack = 100; // Setup variable to hold ACK/NACK resopnses

    // Data fetch
    nackack = I2c.read(TEMP_HUMD_ADDRESS, 4);
    for (int i = 3; i >= 0; i--)  {
        data[i] = I2c.receive();
    }

    humd = ((data[3] & B00111111) << 8) + data[2];
    temp = (data[1] << 6) + (data[0] >> 2);
}

void resetWatchdogTimer()  {
    // clear various "reset" flags
    MCUSR = 0;

    // allow changes, disable reset, clear existing interrupt
    WDTCSR = bit (WDCE) | bit (WDE) | bit (WDIF);

    // set interrupt mode and an interval (WDE must be changed from 1 to 0 here)
    WDTCSR = bit (WDIE) | bit (WDP3) | bit (WDP0); // set WDIE, and 8 seconds delay
    wdt_reset();
}

void enterSleep()  {
    set_sleep_mode(SLEEP_MODE_PWR_DOWN);   // sleep mode is set here

    byte old_ADCSRA = ADCSRA; // disable ADC to save power
    ADCSRA = 0;               // disable ADC to save power

    power_all_disable();      // power off ADC, Timer 0 and 1, serial interface
    noInterrupts();           // timed sequence coming up
    resetWatchdogTimer();     // get watchdog timer ready
    sleep_enable();           // enables the sleep bit in the mcucr register so sleep is possible. just a safety pin
    interrupts();             // interrupts are required now
    sleep_cpu();              // PROGRAM SLEEPS HERE!
    sleep_disable();          // cancel sleep as a precaution
    power_all_enable();       // power everything back on
    ADCSRA = old_ADCSRA;      // re-enable ADC conversion
}


/******************************************************************************/
/*                                 BLUETOOTH                                  */
/******************************************************************************/

void bluetoothSetup() {
    // set up internal status handlers (these are technically optional)
    ble112.onBusy = onBusy;
    ble112.onIdle = onIdle;
    ble112.onTimeout = onTimeout;

    // set up BGLib event handlers
    ble112.ble_evt_system_boot = my_ble_evt_system_boot;
    ble112.ble_evt_connection_status = my_ble_evt_connection_status;
    ble112.ble_evt_connection_disconnected = my_ble_evt_connection_disconnect;
    ble112.ble_evt_attributes_value = my_ble_evt_attributes_value;

    // open BLE software serial port
    bleSerialPort.begin(38400);

    my_ble_evt_system_boot(NULL);
}

// ================================================================
// INTERNAL BGLIB CLASS CALLBACK FUNCTIONS
// ================================================================

// called when the module begins sending a command
void onBusy() {
    // turn LED on when we're busy
    // digitalWrite(LED_PIN, HIGH);
}

// called when the module receives a complete response or "system_boot" event
void onIdle() {
    // turn LED off when we're no longer busy
    // digitalWrite(LED_PIN, LOW);
}

// called when the parser does not read the expected response in the specified time limit
void onTimeout() {}

// called immediately before beginning UART TX of a command
void onBeforeTXCommand() {
    // wait for "hardware_io_port_status" event to come through, and parse it (and otherwise ignore it)
    uint8_t *last;
    while (1) {
        ble112.checkActivity();
        last = ble112.getLastEvent();
        if (last[0] == 0x07 && last[1] == 0x00) break;
    }

    // give a bit of a gap between parsing the wake-up event and allowing the command to go out
    delayMicroseconds(1000);
}

// called immediately after finishing UART TX
void onTXCommandComplete() {
    // allow module to return to sleep (assuming here that digital pin 5 is connected to the BLE wake-up pin)
    // digitalWrite(BLE_WAKEUP_PIN, LOW);
}

// ================================================================
// APPLICATION EVENT HANDLER FUNCTIONS
// ================================================================

void my_ble_evt_system_boot(const ble_msg_system_boot_evt_t *msg) {
    #ifdef DEBUG
        Serial.print("###\tsystem_boot: { ");
        Serial.print("major: "); Serial.print(msg -> major, HEX);
        Serial.print(", minor: "); Serial.print(msg -> minor, HEX);
        Serial.print(", patch: "); Serial.print(msg -> patch, HEX);
        Serial.print(", build: "); Serial.print(msg -> build, HEX);
        Serial.print(", ll_version: "); Serial.print(msg -> ll_version, HEX);
        Serial.print(", protocol_version: "); Serial.print(msg -> protocol_version, HEX);
        Serial.print(", hw: "); Serial.print(msg -> hw, HEX);
        Serial.println(" }");
    #endif

    // system boot means module is in standby state
    // ble_state = BLE_STATE_STANDBY;
    // ^^^ skip above since we're going right back into advertising below

    // set advertisement interval to 200-300ms, use all advertisement channels
    // (note min/max parameters are in units of 625 uSec)
    ble112.ble_cmd_gap_set_adv_parameters(320, 480, 7);
    while (ble112.checkActivity(1000));

    // USE THE FOLLOWING TO LET THE BLE STACK HANDLE YOUR ADVERTISEMENT PACKETS
    // ========================================================================
    // start advertising general discoverable / undirected connectable
    // ble112.ble_cmd_gap_set_mode(BGLIB_GAP_GENERAL_DISCOVERABLE, BGLIB_GAP_UNDIRECTED_CONNECTABLE);
    // while (ble112.checkActivity(1000));

    // USE THE FOLLOWING TO HANDLE YOUR OWN CUSTOM ADVERTISEMENT PACKETS
    // =================================================================
#define BGLIB_GAP_AD_FLAG_GENERAL_DISCOVERABLE 0x02
#define BGLIB_GAP_AD_FLAG_BREDR_NOT_SUPPORTED 0x04
    // build custom advertisement data
    // default BLE stack value: 0201061107e4ba94c3c9b7cdb09b487a438ae55a19
    uint8 adv_data[] = {
        0x02, // field length
        BGLIB_GAP_AD_TYPE_FLAGS, // field type (0x01)
        BGLIB_GAP_AD_FLAG_GENERAL_DISCOVERABLE | BGLIB_GAP_AD_FLAG_BREDR_NOT_SUPPORTED, // 0x06
        0x11, // field length
        BGLIB_GAP_AD_TYPE_SERVICES_128BIT_ALL, // field type (0x07)
        0xe4, 0xba, 0x94, 0xc3, 0xc9, 0xb7, 0xcd, 0xb0, 0x9b, 0x48, 0x7a, 0x43, 0x8a, 0xe5, 0x5a, 0x19
    };

    // set custom advertisement data
    ble112.ble_cmd_gap_set_adv_data(0, 0x15, adv_data);
    while (ble112.checkActivity(1000));

    // build custom scan response data (i.e. the Device Name value)
    // default BLE stack value: 140942474c69622055314131502033382e344e4657
    uint8 sr_data[] = {
        0x14, // field length
        BGLIB_GAP_AD_TYPE_LOCALNAME_COMPLETE, // field type
        'A', 'i', 'r', 'M', 'o', 'n', 'i', 't', 'o', 'r', ' ', '0', '0', ':', '0', '0', ':', '0', '0'
    };

    // get BLE MAC address
    ble112.ble_cmd_system_address_get();
    while (ble112.checkActivity(1000));
    BGAPI_GET_RESPONSE(r0, ble_msg_system_address_get_rsp_t);

    // assign last three bytes of MAC address to ad packet friendly name (instead of 00:00:00 above)
    sr_data[13] = (r0 -> address.addr[2] / 0x10) + 48 + ((r0 -> address.addr[2] / 0x10) / 10 * 7); // MAC byte 4 10's digit
    sr_data[14] = (r0 -> address.addr[2] & 0xF)  + 48 + ((r0 -> address.addr[2] & 0xF ) / 10 * 7); // MAC byte 4 1's digit
    sr_data[16] = (r0 -> address.addr[1] / 0x10) + 48 + ((r0 -> address.addr[1] / 0x10) / 10 * 7); // MAC byte 5 10's digit
    sr_data[17] = (r0 -> address.addr[1] & 0xF)  + 48 + ((r0 -> address.addr[1] & 0xF ) / 10 * 7); // MAC byte 5 1's digit
    sr_data[19] = (r0 -> address.addr[0] / 0x10) + 48 + ((r0 -> address.addr[0] / 0x10) / 10 * 7); // MAC byte 6 10's digit
    sr_data[20] = (r0 -> address.addr[0] & 0xF)  + 48 + ((r0 -> address.addr[0] & 0xF ) / 10 * 7); // MAC byte 6 1's digit

    // set custom scan response data (i.e. the Device Name value)
    ble112.ble_cmd_gap_set_adv_data(1, 0x15, sr_data);
    while (ble112.checkActivity(1000));

    // put module into discoverable/connectable mode (with user-defined advertisement data)
    ble112.ble_cmd_gap_set_mode(BGLIB_GAP_USER_DATA, BGLIB_GAP_UNDIRECTED_CONNECTABLE);
    while (ble112.checkActivity(1000));
    // set state to ADVERTISING
    ble_state = BLE_STATE_ADVERTISING;
}

void my_ble_evt_connection_status(const ble_msg_connection_status_evt_t *msg) {
    #ifdef DEBUG
        Serial.print("###\tconnection_status: { ");
        Serial.print("connection: "); Serial.print(msg -> connection, HEX);
        Serial.print(", flags: "); Serial.print(msg -> flags, HEX);
        Serial.print(", address: ");
        // this is a "bd_addr" data type, which is a 6-byte uint8_t array
        for (uint8_t i = 0; i < 6; i++) {
            if (msg -> address.addr[i] < 16) Serial.write('0');
            Serial.print(msg -> address.addr[i], HEX);
        }
        Serial.print(", address_type: "); Serial.print(msg -> address_type, HEX);
        Serial.print(", conn_interval: "); Serial.print(msg -> conn_interval, HEX);
        Serial.print(", timeout: "); Serial.print(msg -> timeout, HEX);
        Serial.print(", latency: "); Serial.print(msg -> latency, HEX);
        Serial.print(", bonding: "); Serial.print(msg -> bonding, HEX);
        Serial.println(" }");
    #endif

    // "flags" bit description:
    //  - bit 0: connection_connected
    //           Indicates the connection exists to a remote device.
    //  - bit 1: connection_encrypted
    //           Indicates the connection is encrypted.
    //  - bit 2: connection_completed
    //           Indicates that a new connection has been created.
    //  - bit 3; connection_parameters_change
    //           Indicates that connection parameters have changed, and is set
    //           when parameters change due to a link layer operation.

    // check for new connection established
    if ((msg -> flags & 0x05) == 0x05) {
        // track state change based on last known state, since we can connect two ways
        if (ble_state == BLE_STATE_ADVERTISING) {
            ble_state = BLE_STATE_CONNECTED_SLAVE;
        } else {
            ble_state = BLE_STATE_CONNECTED_MASTER;
        }
    }

    bluetoothConnected = (msg -> flags & 0x01) == 0x01;

    // update "encrypted" status
    ble_encrypted = msg -> flags & 0x02;

    // update "bonded" status
    ble_bonding = msg -> bonding;
}

void my_ble_evt_connection_disconnect(const struct ble_msg_connection_disconnected_evt_t *msg) {
    #ifdef DEBUG
        Serial.print("###\tconnection_disconnect: { ");
        Serial.print("connection: "); Serial.print(msg -> connection, HEX);
        Serial.print(", reason: "); Serial.print(msg -> reason, HEX);
        Serial.println(" }");
    #endif

    // set state to DISCONNECTED
    // ble_state = BLE_STATE_DISCONNECTED;
    // ^^^ skip above since we're going right back into advertising below

    // after disconnection, resume advertising as discoverable/connectable
    // ble112.ble_cmd_gap_set_mode(BGLIB_GAP_GENERAL_DISCOVERABLE, BGLIB_GAP_UNDIRECTED_CONNECTABLE);
    // while (ble112.checkActivity(1000));

    // after disconnection, resume advertising as discoverable/connectable (with user-defined advertisement data)
    ble112.ble_cmd_gap_set_mode(BGLIB_GAP_USER_DATA, BGLIB_GAP_UNDIRECTED_CONNECTABLE);
    while (ble112.checkActivity(1000));

    // set state to ADVERTISING
    ble_state = BLE_STATE_ADVERTISING;

    // clear "encrypted" and "bonding" info
    ble_encrypted = 0;
    ble_bonding = 0xFF;
}

void my_ble_evt_attributes_value(const struct ble_msg_attributes_value_evt_t *msg) {
    #ifdef DEBUG
        Serial.print("###\tattributes_value: { ");
        Serial.print("connection: "); Serial.print(msg -> connection, HEX);
        Serial.print(", reason: "); Serial.print(msg -> reason, HEX);
        Serial.print(", handle: "); Serial.print(msg -> handle, HEX);
        Serial.print(", offset: "); Serial.print(msg -> offset, HEX);
        Serial.print(", value_len: "); Serial.print(msg -> value.len, HEX);
        Serial.print(", value_data: ");
        // this is a "uint8array" data type, which is a length byte and a uint8_t* pointer
        for (uint8_t i = 0; i < msg -> value.len; i++) {
            if (msg -> value.data[i] < 16) Serial.write('0');
            Serial.print(msg -> value.data[i], HEX);
        }
        Serial.println(" }");
    #endif

    // check for data written to "c_rx_data" handle
    if (msg -> handle == GATT_HANDLE_C_RX_DATA && msg -> value.len > 0) {
        // set ping 8, 9, and 10 to three lower-most bits of first byte of RX data
        // (nice for controlling RGB LED or something)
        digitalWrite(8, msg -> value.data[0] & 0x01);
        digitalWrite(9, msg -> value.data[0] & 0x02);
        digitalWrite(10, msg -> value.data[0] & 0x04);
    }
}
