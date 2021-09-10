/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/src/drivers/wyzehub-plug-driver.groovy
 *
 *	Copyright 2021 Jake Lehner
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License. You may obtain a copy
 *	of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *	License for the specific language governing permissions and limitations
 *	under the License.
 *
 *
 */

import groovy.transform.Field

public static String version() {  return "v0.0.1"  }

public String deviceModel() { return 'WLPP1CFH' }

@Field static final String wyze_action_power_on = 'power_on'
@Field static final String wyze_action_power_off = 'power_off'

@Field static final String wyze_property_power = 'P3'
@Field static final String wyze_property_device_online = 'P5'
@Field static final String wyze_property_rssi = 'P1612'
@Field static final String wyze_property_vacation_mode = 'P1614'

@Field static final String wyze_property_power_value_on = '1'
@Field static final String wyze_property_power_value_off = '0'
@Field static final String wyze_property_device_online_value_true = '1'
@Field static final String wyze_property_device_online_value_false = '0'
@Field static final String wyze_property_device_vacation_mode_value_true = '1'
@Field static final String wyze_property_device_vacation_mode_value_false = '0'

metadata {
	definition(
		name: "WyzeHub Plug", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/src/drivers/wyzehub-plug-driver.groovy"
	) {
		capability "Outlet"
		capability "Refresh"

		attribute "vacationMode", "bool"
		attribute "online", "bool"
		attribute "rssi", "number"
	}

	preferences 
	{
		
	}
}

void installed() {
    log.debug "installed()"

	// TODO Make Configurable
	unschedule('refresh')
	schedule('0/10 * * * * ? *', 'refresh')

    refresh()
	initialize()
}

void updated() {
   log.debug "updated()"
   initialize()
}

void initialize() {
   log.debug "initialize()"
}

void parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright() { "&copy; 2021 Jake Lehner" }

def refresh() {
	app = getApp()
	app.logInfo("Refresh device ${device.label}")
	app.apiGetDevicePropertyList(device.deviceNetworkId, deviceModel()) { propertyList ->
		createDeviceEventsFromPropertyList(propertyList)
	}

	// TODO Make Configurable
	keepFresh = true
	keepFreshSeconds = 10
	runIn(keepFreshSeconds, 'refresh')
}

def on() {
	app = getApp()
	app.logInfo("'On' Pressed for device ${device.label}")

	app.apiRunAction(device.deviceNetworkId, deviceModel(), wyze_action_power_on) { result ->
		createDeviceEventsFromPropertyList([
			['pid': wyze_property_power, 'value': wyze_property_power_value_on]
		])
	}
	
}

def off() {
	app = getApp()
	app.logInfo("'Off' Pressed for device ${device.label}")

	app.apiRunAction(device.deviceNetworkId, deviceModel(), wyze_action_power_off)
	createDeviceEventsFromPropertyList([
		['pid': wyze_property_power, 'value': wyze_property_power_value_off]
	])
}

void createDeviceEventsFromPropertyList(List propertyList) {
	app = getApp()
    app.logDebug("createEventsFromPropertyList()")

    String eventName, eventUnit
    def eventValue // could be String or number

    // Feels silly to loop through this twice but we need colorMode early.
    // TODO Better way to search propertyList for element with pid = P1508?
    propertyList.each { property ->
        if(property.pid == wyze_property_color_mode) {
			
            deviceColorMode = (property.value == "1" ? 'RGB' : 'CT')
            
            if (device.hasCapability('ColorMode')) {
                eventName = "colorMode"
                eventUnit = null
                eventValue = deviceColorMode

				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: colorMode')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            }
        }
    }

    propertyList.each { property ->
	
        switch(property.pid) {
            // Switch State
            case wyze_property_power:
				eventName = "switch"
                eventUnit = null
                eventValue = property.value == wyze_property_power_value_on ? "on" : "off"
                
				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: switch')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break
        
            // Device Online
            case wyze_property_device_online:
                eventName = "online"
                eventUnit = null
                eventValue = property.value == wyze_property_device_online_value_true ? "true" : "false"
                
				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: online')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // RSSI
            case wyze_property_rssi:
                eventName = "rssi"
                eventUnit = 'db'
                eventValue = property.value

				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: rssi')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Vacation Mode
            case wyze_property_vacation_mode:
                eventName = "vacationMode"
                eventUnit = null
                eventValue = property.value == wyze_property_device_vacation_mode_value_true ? "true" : "false"

				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: vacationMode')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

        }
    }
}

private getApp() {
	app = getParent()
	while(app && app.name != "WyzeHub") {
		app = app.getParent()
	}
	return app
}
