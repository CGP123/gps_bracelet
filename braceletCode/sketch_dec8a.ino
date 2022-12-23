#include <Arduino.h>
#include <HardwareSerial.h>
#include <Wire.h>

#include "BluetoothSerial.h"
BluetoothSerial SerialBT;
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#include <TinyGPS++.h>

#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>

#include "MAX30105.h"
#include "heartRate.h"

#define RXPin (14)  //tx
#define TXPin (15)  //rx

#define SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define GPS_LNG_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define GPS_LAT_UUID "d015b622-8f3d-4dc6-abb8-f010f34fdb12"
#define BPM_UUID "48395a47-5309-4b77-98dd-5fa1c53379d5"
#define IR_UUID "4a5a1499-923e-45ca-8734-44eb6c1bfee3"
#define STEP_UUID "51f00c69-bb68-42f5-a476-b68e3161f5a3"
#define FALL_UUID "7a974a9e-09f2-42c1-af7c-5dbe479757f4"

#define ACS_X_UUID
#define ACS_Y_UUID
#define ACS_Z_UUID

#define GYRO_X_UUID
#define GYRO_Y_UUID
#define GYRO_Z_UUID


#define NAME_DEVICE "ESP_BLE"

bool deviceConnected = false;

BLECharacteristic GPS_lng(GPS_LNG_UUID,
                          BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLECharacteristic GPS_lat(GPS_LAT_UUID,
                          BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLECharacteristic BPM(BPM_UUID,
                      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLECharacteristic IR(IR_UUID,
                     BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLECharacteristic STEP(STEP_UUID,
                       BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
BLECharacteristic FACS(FALL_UUID,
                       BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);

Adafruit_MPU6050 mpu;
MAX30105 particleSensor;
TinyGPSPlus atgm336h;
HardwareSerial ss(2);


class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
  };
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
  }
};

static const uint32_t GPSBaud = 9600;
unsigned long timing;
unsigned long timing1;
unsigned long timing2;
char GPS_lat_out[12], GPS_lng_out[12];
char ir_out[6], bpm_out[6];
char step_out[6];
char facs_out[4];

const byte RATE_SIZE = 4;
long lastBeat = 0;
float beatsPerMinute;

int flag = 0;
unsigned int st;

void setup_bluetooth();
void setup_modules();
void gps_func();
void acs_func();
void pulse_func();

void setup() {
  Serial.begin(115200);
  if (!SerialBT.begin("ESP32")) {
    Serial.println("An error occurred initializing Bluetooth");
  } else {
    Serial.println("Bluetooth initialized");
  }
  //setup_bluetooth();
  setup_modules();
  delay(100);
}

void loop() {

  while (ss.available() > 0)
    if (atgm336h.encode(ss.read()))
      gps_func();

  Serial.println();
  pulse_func();

  acs_func();
  delay(10);
}

void setup_bluetooth() {
  BLEDevice::init(NAME_DEVICE);

  BLEServer* pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService* pService = pServer->createService(SERVICE_UUID);

  pService->addCharacteristic(&GPS_lng);
  pService->addCharacteristic(&GPS_lat);
  pService->addCharacteristic(&BPM);
  pService->addCharacteristic(&IR);
  pService->addCharacteristic(&STEP);
  pService->addCharacteristic(&FACS);
  pService->start();

  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");
}

void setup_modules() {
  ss.begin(GPSBaud, SERIAL_8N1, RXPin, TXPin, false);  //ATGM336H-GPS

  particleSensor.begin(Wire, I2C_SPEED_FAST, 0x57);  //particleSensor-PULSE
  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x0A);
  particleSensor.setPulseAmplitudeGreen(0);

  mpu.begin(0x68);  //MPU6050-ACS_GYRO
  mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);
}

void gps_func() {
  if (atgm336h.location.isValid()) {
    dtostrf(atgm336h.location.lat(), 10, 6, GPS_lat_out);
    dtostrf(atgm336h.location.lng(), 10, 6, GPS_lng_out);
    Serial.print("Coordinates: ");
    Serial.print(GPS_lat_out);
    Serial.print(" ");
    Serial.print(GPS_lng_out);
    //SerialBT.println("LAT:" + String(GPS_lat_out));
    //SerialBT.println("LNG:" + String(GPS_lng_out));
    //GPS_lat.setValue(GPS_lat_out); GPS_lat.notify();
    //GPS_lng.setValue(GPS_lng_out); GPS_lng.notify();
  } else
    Serial.print("INVALID");

  Serial.println();
}

void pulse_func() {
  int irValue = particleSensor.getIR();

  if (checkForBeat(irValue) == true) {
    long delta = millis() - lastBeat;
    lastBeat = millis();
    beatsPerMinute = 60 / (delta / 1000.0);
  }

  dtostrf(beatsPerMinute, 6, 2, bpm_out);
  dtostrf(irValue, 5, 0, ir_out);
  if (millis() - timing1 > 7000) {  // Вместо 10000 подставьте нужное вам значение паузы
    timing1 = millis();
    if (irValue > 50000) {
      SerialBT.println("Браслет надет");
    } else {
      SerialBT.println("Браслет снят");
    }
  }
  if (millis() - timing > 10000) {  // Вместо 10000 подставьте нужное вам значение паузы
    timing = millis();
    if (irValue > 50000) {
      SerialBT.println("Пульс: " + String(bpm_out));
    }
  }

  //BPM.setValue(bpm_out); BPM.notify();
  //IR.setValue(ir_out); IR.notify();

  Serial.print("IR=");
  Serial.print(irValue);
  Serial.print(", BPM=");
  Serial.print(beatsPerMinute);
  if (irValue < 50000)
    Serial.print(" No finger?");
  Serial.println();
}

void acs_func() {
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);
  float s = sq(a.acceleration.x) + sq(a.acceleration.y) + sq(a.acceleration.z);
  float at = sqrt(sq(a.acceleration.x) + sq(a.acceleration.y) + sq(a.acceleration.z));

  Serial.print("Acceleration X: ");
  Serial.print(a.acceleration.x);
  Serial.print(", Y: ");
  Serial.print(a.acceleration.y);
  Serial.print(", Z: ");
  Serial.print(a.acceleration.z);
  Serial.println(" m/s^2");

  Serial.print("Rotation X: ");
  Serial.print(g.gyro.x);
  Serial.print(", Y: ");
  Serial.print(g.gyro.y);
  Serial.print(", Z: ");
  Serial.print(g.gyro.z);
  Serial.println(" rad/s");

  Serial.print("Temperature: ");
  Serial.print(temp.temperature);
  Serial.println(" degC");

  Serial.println();

  if (s > 270 && flag == 0) {
    st++;
    flag = 1;
  }

  if (s <= 270 && flag == 1) {  // Защита от многократного срабатывания
    flag = 0;
  }
  if (millis() - timing2 > 13000) {  // Вместо 10000 подставьте нужное вам значение паузы
    timing2 = millis();

    SerialBT.println("Количество шагов: " + String(step_out));
  }
  if (at < 1) {
    Serial.println("free fall detected -------------------------------------------------------------------------------------------------------------------------------------------------------------------");
    SerialBT.println("Свободное падение");
  }
  Serial.print("full acs: ");
  Serial.println(at);
  dtostrf(at, 4, 2, facs_out);

  //FACS.setValue(facs_out); FACS.notify();

  Serial.print("step count: ");
  Serial.println(st);
  dtostrf(st, 6, 0, step_out);
  //STEP.setValue(step_out); STEP.notify();
}