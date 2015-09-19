/**
 *  Nest Protect
 *
 *  Copyright 2015 nick@nickhbailey.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
preferences {
    input("username", "text", title: "Username", description: "Your Nest username (usually an email address)")
    input("password", "password", title: "Password", description: "Your Nest password")
    input("mac", "text", title: "MAC Address", description: "The MAC Address of your Nest Protect")
}

metadata {
	definition (name: "Nest Protect", namespace: "smartthings-users", author: "nick@nickhbailey.com") {
		capability "Battery"
		capability "Carbon Monoxide Detector"
		capability "Polling"
		capability "Smoke Detector"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {

        valueTile("smoke", "device.smoke", width: 2, height: 2){
        	//state ("clear", label: "OK", unit: "Smoke", backgroundColor:"#44B621")
			//state ("detected", icon:"st.alarm.smoke.smoke", label:"SMOKE", backgroundColor:"#e86d13", unit: "Smoke")
			//state ("tested", label:"TEST", backgroundColor:"#003CEC", unit: "Smoke")
            state "smoke", label: 'Smoke ${currentValue}', unit:"smoke",
            	backgroundColors: [
                    [value: "clear", color: "#44B621"],
                    [value: "detected", color: "#e86d13"],
                    [value: "tested", color: "#003CEC"]
                ]
		}
		valueTile("carbonMonoxide", "device.carbonMonoxide"){
        	//state("clear", backgroundColor:"#44B621", unit: "CO")
			//state("detected", label:"CO", icon:"st.alarm.smoke.smoke", backgroundColor:"#e86d13", unit: "CO")
			//state("tested", label:"TEST", icon:"st.alarm.smoke.test", backgroundColor:"#003CEC", unit: "CO")
            state("carbonMonoxide", label: 'CO ${currentValue}', unit:"CO", backgroundColors: [
                    [value: "clear", color: "#44B621"],
                    [value: "detected", color: "#e86d13"],
                    [value: "tested", color: "#003CEC"]
                ]
            )
		}
        valueTile("battery", "device.battery"){
        	//state("OK", backgroundColor:"#44B621", unit: "Batt")
			//state("Low", label:"Low", backgroundColor:"#e86d13")
            state("battery", label: 'Battery ${currentValue}', unit:"battery", backgroundColors: [
                    [value: "OK", color: "#44B621"],
                    [value: "Low", color: "#e86d13"]
                ]
            )
		}
        standardTile("refresh", "device.smoke", inactiveLabel: false, decoration: "flat") {
            state "default", action:"polling.poll", icon:"st.secondary.refresh"
        }
		main "smoke"
        details(["smoke", "carbonMonoxide", "battery", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'battery' attribute
	// TODO: handle 'carbonMonoxide' attribute
	// TODO: handle 'smoke' attribute

}

def auto() {
    log.debug "Executing 'auto'"
}

// handle commands
def poll() {
    log.debug "Executing 'poll'"
    api('status', []) {
        data.topaz = it.data.topaz.getAt(settings.mac.toUpperCase())

        data.topaz.smoke_status = data.topaz.smoke_status == 0? "clear" : "detected"
        data.topaz.co_status = data.topaz.co_status == 0? "clear" : "detected"
        data.topaz.battery_health_state = data.topaz.battery_health_state  == 0 ? "OK" : "Low"
             
        sendEvent(name: 'smoke', value: data.topaz.smoke_status)
        sendEvent(name: 'carbonMonoxide', value: data.topaz.co_status)
        sendEvent(name: 'battery', value: data.topaz.battery_health_state )
        log.debug settings.mac
        log.debug data.topaz.wifi_mac_address
        log.debug data.topaz.smoke_status
        log.debug data.topaz.co_status
        log.debug data.topaz.battery_health_state
    }
}

def api(method, args = [], success = {}) {
    if(!isLoggedIn()) {
        log.debug "Need to login"
        login(method, args, success)
        return
    }
 
    def methods = [
        'status': [uri: "/v2/mobile/${data.auth.user}", type: 'get']
    ]
    
    def request = methods.getAt(method)
    
    log.debug "Logged in"
    doRequest(request.uri, args, request.type, success)
}

// Need to be logged in before this is called. So don't call this. Call api.
def doRequest(uri, args, type, success) {
    log.debug "Calling $type : $uri : $args"
    
    if(uri.charAt(0) == '/') {
        uri = "${data.auth.urls.transport_url}${uri}"
    }
    
    def params = [
        uri: uri,
        headers: [
            'X-nl-protocol-version': 1,
            'X-nl-user-id': data.auth.userid,
            'Authorization': "Basic ${data.auth.access_token}"
        ],
        body: args
    ]
    
    if(type == 'post') {
        httpPostJson(params, success)
    } else if (type == 'get') {
        httpGet(params, success)
    }
}
 
def login(method = null, args = [], success = {}) {    
    def params = [
        uri: 'https://home.nest.com/user/login',
        body: [username: settings.username, password: settings.password]
    ]        
    
    httpPost(params) {response -> 
        data.auth = response.data
        data.auth.expires_in = Date.parse('EEE, dd-MMM-yyyy HH:mm:ss z', response.data.expires_in).getTime()
        log.debug data.auth
        
        api(method, args, success)
    }
}
 
def isLoggedIn() {
    if(!data.auth) {
        log.debug "No data.auth"
        return false
    }
    
    def now = new Date().getTime();
    return data.auth.expires_in > now
}


