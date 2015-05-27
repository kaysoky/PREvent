---
layout: post
title: Product Requirements Document
---
### Summary
Usually, we do not realize that more than 50% of health problems originate from  ambient air.  
The air quality we should be concerned is not the same with what we check on weather forecasts 
but related to places more intimate in our lives such as our home and office. However, our current 
air quality monitoring system can not provide such detailed and immediate data for each individual. 
That is why we choose to develop a wearable device that can continuously monitor the air quality around us. 

### Deliverables
* wearable wristband with LED indicators  
* web server backend to aggregate and analyze collected data as heap maps
* android application as user interface 

### Critical Features
* Accurately monitor environmental factors including humidity level, 
PM (particulate matter) density, VOC(volatile organic compound) density and temperature. 
* Provide real time data and suggestions to users.
* Allow logging and analytics of collected data on server backend 
* Comfortable and convenient to wear.

### Milestones
see [project schedule](https://www.google.com/calendar/embed?src=oml584uniamsa8ihe1kano3v18%40group.calendar.google.com&ctz=America/Los_Angeles)
### Team Member Responsibilities
* Joseph Wu: android development, Bluetooth integration, web server construction 
* Jay Feng: backend data aggregation
* Mingyu Zhang: android development, UI design
* William Hwang: Bluetooth integration, hardware design and integration

### Materials & Budget
| Sensor | Link | Cost | Voltage | Current | Interface |
| :---: | :---: | :---: | :---: | :---: | :---: |
| PM2.5 | [Sharp Microelectronics GP2Y1010AU0F](https://www.sparkfun.com/products/9689) | $11.95 | 4.5-5.5 V | 20 mA | Digital |
| Volatile Organic Chemical (VOC) Gas | [AMS AS-MLV-P2](http://www.digikey.com/product-detail/en/AS-MLV-P2/AS-MLV-P2-ND/5117220) | $18.91 | 2.7 V | 13 mA | Analog |
| Humidity | [Amphenol ChipCap2](http://www.digikey.com/product-search/EN?mpart=CC2D25S-SIP) | $15.21 | 5 V | 0.6 - 750 uA | I2C |

| Other Hardware | Link | Cost | Voltage | Comments |
| :---: | :---: | :---: | :---: | :---: |
| Arduino | [TinyDuino Processor Board](https://tiny-circuits.com/tinyduino-processor-board.html) | $19.95 | 2.7-5.5V | 1.2mA |
| Bluetooth | [TinyShield Bluetooth Low Energy](https://tiny-circuits.com/tiny-shield-bluetooth-low-energy-146.html) | $49.95 | 3-5 V | up to 27mA |
| Boost Converter | [NCP1402](https://www.pololu.com/product/798) | $4.95 | 5 V | ~85% Efficiency |
| Buck Converter | [Intersil ISL85415](https://www.pololu.com/product/2841) | $4.95 | 2.5 V | ~85% Efficiency |
| Battery | [Lithium Ion Polymer Battery](https://www.sparkfun.com/products/339) | $9.95 | 3.7 V | 1000mAh Capacity |
| Charger | [Lithium Battery Charger](https://www.sparkfun.com/products/10217) | $7.95 | 3.7 V | 500mA Charge Rate |
| Connector | [PM Sensor Connector](https://www.sparkfun.com/products/9690) | $0.35 | | 6 Pin Connector |
| Crimp Pins | [PM Sensor Crimp Pins](https://www.sparkfun.com/products/9728) | $0.60 | | 6x Crimp Pins |
| Miscellaneous Electronics & Shipping | | $21.10 | | |

Note: For development purposes, we will be getting the [TinyDuino Basic Kit](https://tiny-circuits.com/tiny-duino-basic-kit-158.html), which includes the programming interface.  This item is not included in the BOM because it is not part of the final product.
* Total Cost: $165.82

### Risks
??? 

