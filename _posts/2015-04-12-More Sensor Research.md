---
layout: post
title: More Sensor Research
---
## Base system:
* Dust sensor
* Gas sensor (VOC?)
* Relative humidity sensor

### Notes on humidity
* Output of the sensors depends on humidity
* When humid, dust sensors detect water as small particles
* Gas sensors are also coupled to humidity
* Sensors not water proof (need rain-proofing)

## [Dust sensors on DigiKey](http://www.digikey.com/product-search/en?FV=fff4001e,fff8004e&mnonly=0&newproducts=0&ColumnSort=0&page=1&stock=1&quantity=0&ptm=0&fid=0&pageSize=25)
1. GP2Y1010AU0F [Datasheet](http://www.sharpsma.com/webfm_send/1488)
2. GP2Y1023AU0F [Datasheet](http://media.digikey.com/pdf/Data Sheets/Sharp PDFs/GP2Y1023AU0F pecs.pdf)
3. DN7C3CA006 [Datasheet](http://media.digikey.com/pdf/Data Sheets/Sharp PDFs/DN7C3CA006_Spec.pdf)
4. SM-PWM-01A [Datasheet](http://www.amphenol-sensors.com/en/component/edocman/225-telaire-dust-sensor-application-sheet/download)

### Notes on DigiKey sensors
* Sensors 1-3 output PWM/Analog, approximately proportional to dust density and have a lower operating current
* Sensor 4 has two outputs, one for PM2.5 and one for PM10.  
  Output is a waveform (page 10 of the datasheet) that shows whenever the sensor has been obstructed by a particle.  
  This sensor has a longer acquisition time because it uses a moving average to smooth out the data
* Need to find a way to calibrate the sensors

### Gas sensors
* [VOC sensor](http://ams.com/eng/content/download/686543/1787717/348218)
* [MQ-135](http://www.futurlec.com/Air_Quality_Control_Gas_Sensor.shtml)
* These sensors have an internal heating element that needs to be preheated to a certain temperature in order to operate properly.  
  * MQ-135 preheat time is approximately 24 hours
  * VOC sensor preheat time not listed, but the target temperature is 320 C
* Gas sensors do not measure precise concentrations
* Should investigate EPA standards on hazardous gases
* May need to contact [Civil and Environmental Engineering department](http://www.ce.washington.edu/research/environment/facilities.html) for calibration help
