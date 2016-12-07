// APIv1 Details
var apiRoot = 'https://api.at.govt.nz/v1/';
var apiKey = '1e6069be9fa8e5f7aa3fbcee39a783e6';
var epochKey = 'a471a096baaa08c893f48a909d0ae3d3';
var sharedSecret = '3843d5c0be9dc71e3a547d16bf2fcc9c';

// Global vars
var map;
var locationsUrl = 'public-restricted/realtime/vehicleLocations/';
var drift = 0;
var markers = [];
var markLen = 0;
var jsonData;
//shift_up = 0;
//shift_right = 0;

var isDoubleDecker = function(vehicle) {
  
	// List from: github.com/consindo/dymajo-transit/blob/master/server/realtime.js
	var doubleDeckers = [
	
		// NZ Bus / Metrolink
		// Alexander Dennis Enviro500
		'3A99', '3A9A', '3A9B', '3A9C', '3A9D', '3A9E', '3A9F', 
		'3AA0', '3AA1', '3AA2', '3AA3', '3AA4', '3AA5', '3AA6', 
		'3AA7', '3AA8', '3AA9', '3AAA', '3AAB', '3AAC', '3AAD', 
		'3AAE', '3AAF', 

		// Howick and Eastern
		// More Enviro500
		'5FB4', '5FB5', '5FB6', '5FB7', '5FB8', '5FB9', '5FBA',
		'5FBB', '5FBC', '5FBD', '5FBE', '5FBF', '5FC0', '5FC1',
		'5FC2',

		//NEX BCI CityRider FBC6123BRZ
		'5622', '5623', '5624', '5625', '5626', '5627', '5628',
		'5629', '562A', '562B', '562C', '562D', '562E', '562F',
		'5630'
	];
	
	// uncomment this line if you want it to randomly be a double decker
	// if (Math.ceil(Math.random()*2) >1) {
	if (doubleDeckers.indexOf(vehicle) !== -1) {
		return true;
	}
	return false;
}

function initMap() {
	
	// Go to Auckland
	map = new google.maps.Map(document.getElementById('map'), {
		center: {lat: -36.854, lng: 174.822},
		zoom: 11
	});
	
	// Call for data
	getData();
}

function addMarker(myLatLng, title, pos) {
	
	// Add marker to map
	marker = new google.maps.Marker({
		position: myLatLng,
		map: map,
		title: title
	});
	
	// Get parameters needed
	var entity = jsonData.response.entity[pos];
	var tripId = entity.vehicle.trip.trip_id;
	var routeId = entity.vehicle.trip.route_id;
	var startTime = entity.vehicle.trip.start_time;
	var lati = entity.vehicle.position.latitude;
	var longi = entity.vehicle.position.longitude;
	var vehicle = entity.vehicle.vehicle.id;
	var occupancy_status = entity.vehicle.occupancy_status;
	var timestamp = entity.vehicle.timestamp;
	
	// Show details in bottom plane on click
	marker.addListener('click', function() {
		
		// Create HTML
		document.getElementById('message').innerHTML = 
				  'Trip ID: '			+ tripId 
				+ '<br>Route ID: '		+ routeId 
				+ '<br>Start time: '	+ startTime
				+ '<br>Vehicle ID: '	+ vehicle
				+ '<br>Lattitude: '		+ lati
				+ '	   Longitude: '		+ longi;
	});
	
	// Colour trains green
	if (startTime == undefined) marker.setIcon('http://maps.google.com/mapfiles/ms/icons/green-dot.png');
	
	// Alternative method of colouring trains
	//if (occupancy_status != 6) marker.setIcon('http://maps.google.com/mapfiles/ms/icons/blue-dot.png');
	
	// Colour records > 200s old blue
	//if (timestamp < (Date.now()/1000 - 200)) marker.setIcon('http://maps.google.com/mapfiles/ms/icons/blue-dot.png');
	
	// Colour double deckers blue
	if (isDoubleDecker(vehicle)) marker.setIcon('http://maps.google.com/mapfiles/ms/icons/blue-dot.png');
	
	markers[markLen++] = marker;
}

function getData() {
	
	// Make AJAX request
	$.getJSON(
			//apiRoot + locationsUrl + getAuthorization() + '&callback=?'	// Public use this
			//"../buffer?api=vehiclelocations"								// Doesn't have occupancy status
			"../buffer?api=realtime"
			//"../fake_api/combined.json"
			//"../fake_api/vehiclelocations_v2.json"
	, function(data){
		
		// Store in global variable
		jsonData = data;
		
		// Loop through each vehicle
		for (var i=0; i<data.response.entity.length; i++) {
			
			// If not a vehicle entry skip
			if (data.response.entity[i].vehicle == undefined) continue;
			
			// Get parameters needed
			var lati = data.response.entity[i].vehicle.position.latitude;
			var longi = data.response.entity[i].vehicle.position.longitude;	
			var startTime = data.response.entity[i].vehicle.trip.start_time;
			
			// Fix the location of trains
			if (startTime == undefined) {
				
				lati = lati*1.66 + 23.7564;
				longi = longi*1.66 - 114.8370;
				
				if (lati < -37.091) {
					lati += 0.6639;
				}
				
			} else {
				//continue; //only show trains
			}
			
			// Add marker to map
			var latLng = {lat: lati, lng: longi};
			addMarker(latLng, '', i);
		}
	});
}

function refresh() {
	
	// Remove existing markers from the map
	for (var i=0; i < markLen; i++) {
		markers[i].setMap(null);
	}
	markLen = 0;
	
	// Call for new data
	getData();
}

/* Needed for APIv1 Authentication */
function getAuthorization() {
	setDriftDeferred();
	var epoch = Math.floor((new Date()).valueOf() / 1000) + drift + '';
	var signature = CryptoJS.HmacSHA1(epoch + apiKey , sharedSecret) + '';
	return '?api_sig=' + signature + '&api_key=' + apiKey + '&ivu=true';
}
function setDriftDeferred() {
	$.getJSON(apiRoot + 'time/epoch?api_key=' + epochKey, {noIntercept:true,ignoreLoadingBar:true}, function(json) {
		var urlTime = json.response.time;
		var computerTime = Math.floor((new Date()).valueOf()/1000); //13 figures of time
		drift = urlTime - computerTime ;
	});
	return drift;
}
