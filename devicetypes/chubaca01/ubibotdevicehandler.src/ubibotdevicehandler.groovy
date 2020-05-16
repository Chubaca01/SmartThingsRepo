/**
 *  UbibotVirtualDevice
 *
 *  Copyright (c) 2014 Statusbits.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain a
 *  copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License  for the specific language governing permissions and limitations
 *  under the License.
 *
 *  
 *  
 *  
 *  05-10-2020 : V1.0 ( updated version from original - johnconstantelo)
 *  05-12-2020 : V1.1. Added unit temperature selection 
 *  03-13-2020 : V1.1.1 added Battery level
  * 03-15-2020 : V1.1.2 fix bug degree to Farenheit - issue with server converting back to fahrenheit  
 */
 
import groovy.json.JsonSlurper

metadata {
    definition (name:"UbibotDeviceHandler", namespace:"Chubaca01", author:"Phil Tourn") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Sensor"
        capability "Illuminance Measurement"
        capability "Voltage Measurement"
        capability "Battery"
        capability "Refresh"

        // custom commands
        command "parse"     // (String "temperature:<value>" or "humidity:<value>" )
    }
	tiles(scale: 1) {
		multiAttributeTile(name:"temperature", type: "generic", width: 6, height: 4){
			tileAttribute ("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", label:'🏊🏼${currentValue}°', unit:"F",
                backgroundColors:[
                    [value: 31, color: "#153591"],
                    [value: 44, color: "#1e9cbb"],
                    [value: 59, color: "#90d2a7"],
                    [value: 74, color: "#44b621"],
                    [value: 84, color: "#f1d801"],
                    [value: 95, color: "#d04e00"],
                    [value: 96, color: "#bc2323"]
                ]
            )
			}
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
        		attributeState("humidity", label:'${currentValue}%', defaultState: true)
    		}
            tileAttribute("device.illuminance", key: "SECONDARY_CONTROL") {
        		attributeState("illuminance",label:'\t\t 🔆 ${currentValue} lx', unit:"lx", defaultState: true)
    		}
		}
        valueTile("extemperature", "device.extemperature", decoration: "flat", width: 2, height: 2) {
			state "extemperature", label: '⛅\r${currentValue} °'
		}
        valueTile("voltage", "device.voltage", decoration: "flat", width: 2, height: 2) {
			state "voltage", label: '⚡\r${currentValue} V'
		}
        valueTile("battery", "device.battery",decoration: "flat", width: 2, height: 2) {
			state "battery", label:'🔋\r${currentValue}%'
		}
        valueTile("wifiRSSI", "device.wifiRSSI", decoration: "flat", width: 2, height: 2) {
			state "wifiRSSI", label: 'Wifi 📶\r${currentValue} dB'
		}
        valueTile("chanId", "device.chanId", decoration: "flat", width: 2, height: 2) {
			state "chanId", label: '\rChannel Id\r${currentValue}'
		}
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }
        main(["temperature"])
        details(["temperature","illuminance","chanId","extemperature","battery","voltage","wifiRSSI","refresh"])
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
	// init flipValue to true : used to trigger sendEvent on refresh to SsmartApp
	state.flipValue = true
    state.sensorList = [1: [sensorName:"illuminance", initialValue: 300],
              2: [sensorName:"temperature", initialValue: 25],
              3: [sensorName:"extemperature", initialValue: 30],
              4: [sensorName:"humidity", initialValue: 29],
              5: [sensorName:"voltage", initialValue: 2.78],
              6: [sensorName:"wifiRSSI", initialValue: -26],
              7: [sensorName:"battery", initialValue: 80]]
    TRACE( "initialize")
    state.sensorList.eachWithIndex { entry, i ->
		TRACE( "$i - Index: $entry.key Value: $entry.value")
		def mapSensor = entry.value
        if (!device.currentState(mapSensor.sensorName)) {
        	TRACE( "Found : $mapSensor.sensorName  value:mapSensor.initialValue ")
        	sendEvent(name: mapSensor.sensorName, value: getSensorVal(mapSensor.sensorName, mapSensor.initialValue))
		}
    // init channel ID
    sendEvent(name: "chanId", value: "")
	}
}

def parse(message) {
	def event
	def jsonSlurper = new JsonSlurper()
    def msg = jsonSlurper.parseText(message)
    if (msg.containsKey("channelId")) {
    	TRACE("ChannelId in msg : $msg.channelId ")
        event = [
                name  : "chanId",
                value : msg.channelId,
            ]
        TRACE("event: (${event})")
    	sendEvent(event)
    }
    else
    {
    	TRACE("parseMap : $msg ")
        def unit
    	msg.eachWithIndex { entry, i ->
        	TRACE( "$i - Index: $entry.key Value: $entry.value")
            def mapSensor = entry.value
            TRACE("mapSensor : $mapSensor ")
            unit = mapSensor.unit
            Float fValue = mapSensor.currentValue.toFloat()
            if (mapSensor.unit == "F"){
            	fValue = celciusTofahrenheit(fValue)
            }
            if (mapSensor.unit == "C") {
             	// to avoid trick from the server let's stick the unit to F
             	unit = "F"
            }
            
            if (mapSensor.sensorName == "battery"){
            	fValue = calcBatteryLevel(fValue)
            }
            event = [
                name  : mapSensor.sensorName,
                value : fValue.round(1),
                unit  : unit,
            ]
    	TRACE("event: (${event})")
    	sendEvent(event)    
		}
    }
    return null
}

def refresh() {
	TRACE("Send event refresh to smart app")
    state.flipValue = !state.flipValue
    sendEvent(name:"refreshPushed", value: state.flipValue)
}

private getSensorVal(sensorType, val) {
    def ts = device.currentState(sensorType)
    Integer value = ts ? ts.integerValue : val
    return value
}

def celciusTofahrenheit(tempC){
	def tempF = (tempC * 9)/5 + 32
    return tempF
    }
    
def calcBatteryLevel(voltMeas){
  	Float batLevel = ((voltMeas * 100)-200)/0.9
   	if (batLevel < 0) {
   		batLevel = 0
   	}
   	if (batLevel > 100) {
   		batLevel = 100
   	}
    return batLevel
}

private def TRACE(message) {
    log.debug message
}

