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
 *  05-10-2018 : V1.0 ( updated version from original - johnconstantelo)
 *
 */
 
import groovy.json.JsonSlurper

metadata {
    definition (name:"UbibotDeviceHandler", namespace:"Chubaca01", author:"Phil Tourn") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Sensor"
        capability "Illuminance Measurement"
        capability "Refresh"

        // custom commands
        command "parse"     // (String "temperature:<value>" or "humidity:<value>" )
    }
	tiles(scale: 1) {
		multiAttributeTile(name:"extemperature", type: "generic", width: 6, height: 4){
			tileAttribute ("device.extemperature", key: "PRIMARY_CONTROL") {
				attributeState("extemperature", label:'🏊🏼${currentValue}°', unit:"F",
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
        valueTile("temperature", "device.temperature", decoration: "flat", width: 2, height: 2) {
			state "temperature", label: '⛅\r${currentValue} °'
		}
        valueTile("voltage", "device.voltage", decoration: "flat", width: 2, height: 2) {
			state "voltage", label: '🔋\r${currentValue} V'
		}
        valueTile("wifiRSSI", "device.wifiRSSI", decoration: "flat", width: 2, height: 2) {
			state "wifiRSSI", label: '📶\r${currentValue} dB'
		}
        valueTile("chanId", "device.chanId", decoration: "flat", width: 2, height: 2) {
			state "chanId", label: '\rChannel Id\r${currentValue}'
		}
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }
        main(["extemperature"])
        details(["extemperature","temperature","illuminance","voltage","wifiRSSI","chanId","refresh"])
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
    state.sensorList = [1: [sensorName:"illuminance", initialValue: 300, currentValue: 0, unit: "lux"],
              2: [sensorName:"temperature", initialValue: 25,currentValue: 0, unit: "F"],
              3: [sensorName:"extemperature", initialValue: 30,currentValue: 0, unit: "F"],
              4: [sensorName:"humidity", initialValue: 29,currentValue: 0, unit: "%"],
              5: [sensorName:"voltage", initialValue: 2.78,currentValue: 0, unit: "V"],
              6: [sensorName:"wifiRSSI", initialValue: -26,currentValue: 0, unit: "dB"]]
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
    	msg.eachWithIndex { entry, i ->
        	TRACE( "$i - Index: $entry.key Value: $entry.value")
            def mapSensor = entry.value
            TRACE("mapSensor : $mapSensor ")
            Float fValue = mapSensor.currentValue.toFloat()
            event = [
                name  : mapSensor.sensorName,
                value : fValue.round(1),
                unit  : mapSensor.unit,
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

private def TRACE(message) {
    //log.debug message
}

