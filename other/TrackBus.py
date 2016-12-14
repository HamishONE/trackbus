#!/bin/python3

########################################
### AT Bus Tracker by Hamish O'Neill ###
########################################

# WARNING: This program was created in early 2016 for the simple purpose of figuring out
# how to use the Auckland Transport APIs (which at that time were not fully public). The 
# Android app initially drew from here, but it has NOT been maintained along with the Java 
# code equivalent. It's only useful purpose would be as a learning exercise. Beware bugs!

# Import libraries
import time
import urllib.request
import json
import math
import hmac
from hashlib import sha1
import webbrowser

# Important strings (from android app)
apiRoot = 'https://api.at.govt.nz/v1/'
apiKey = '1e6069be9fa8e5f7aa3fbcee39a783e6'
sharedSecret = '3843d5c0be9dc71e3a547d16bf2fcc9c'
stopInfo = 'gtfs/stops/stopinfo/'
vehicleLocations = 'public-restricted/realtime/vehiclelocations/'
tripUpdates = 'public-restricted/realtime/tripUpdates/'
realtime = 'public-restricted/realtime/'

# Calls API and returns dictionary
def getJSON(apiUrl):
	return json.loads(urllib.request.urlopen(apiUrl).read().decode('utf-8'))

# Emulator for CryptoJS Hmac.SHA1
def hmacSHA1(in1, in2):
	return hmac.new(in2.encode(encoding='UTF-8'), in1.encode(encoding='UTF-8'), sha1).hexdigest()

# Returns string to append to api call
def getAuthorization():
	epoch = str(math.floor(time.time()))
	signature = hmacSHA1(epoch + apiKey, sharedSecret)
	return '?api_sig=' + signature + '&api_key=' + apiKey + '&ivu=true'

# Returns number of stop bus is through sequence ..............currently unused!
def getStopData(tripID):
	stopData = getJSON(apiRoot + tripUpdates + getAuthorization() + '&tripid=' + str(tripID)).get('response')
	if isinstance(stopData, list):
		return -1 # Indicates no data
	stopNum = stopData.get('entity')[0].get('trip_update').get('stop_time_update').get('stop_sequence')
	return stopNum

# Main function
def main():

	#webbrowser.open(apiRoot + stopInfo + str(7180) + getAuthorization())

	# Input bus stop
	stopCode = input("Enter bus stop ID: ")
	
	# Get data arrays for all buses
	tripData = getJSON(apiRoot + stopInfo + str(stopCode) + getAuthorization()).get('response')			# ~10KB
	locData = getJSON(apiRoot + vehicleLocations + getAuthorization()).get('response').get('entity')	# ~150KB
	stopData = getJSON(apiRoot + tripUpdates + getAuthorization()).get('response').get('entity')		# ~500KB??
	
	# Print headers
	print("\nHamish's Real Time Board for Stop " + stopCode + "\n")
	print("NUM  ROUTE   LATITUDE     LONGITUDE    TIME AGO   STOPS   DELAY    DUE     SCHD")
	
	# To store trip numbers for each bus printed
	array = list(range(1000))
	k = 1 # Bus number (index) counter
	
	# Loop through all bus trips
	for i in range(len(tripData)):
		# Extract key trip data
		route = tripData[i].get('route_short_name')
		tripDataTrip = tripData[i].get('trip_id')
		stopSeq = tripData[i].get('stop_sequence')
		schTimeStr = tripData[i].get('departure_time')
		
		# Parse scheduled time
		if schTimeStr[0:2] == '24':
			# Adjust time as falls in next day
			schTimeStr = schTimeStr.replace('24', '00')
			schTime = time.strptime(schTimeStr, '%H:%M:%S')
			schTimeTup = time.localtime()[0:2] + (time.localtime()[2] + 1,) + schTime[3:6] + (0, 1, -1)
		else:
			schTime = time.strptime(schTimeStr, '%H:%M:%S')
			schTimeTup = time.localtime()[0:3] + schTime[3:6] + (0, 1, -1)
		
		# Find stops sequence
		stopDict = next((item for item in stopData if item['trip_update'].get('trip').get('trip_id') == tripDataTrip), None)
		
		if stopDict != None:
			# Find stops away
			stopsAway = stopSeq - stopDict.get('trip_update').get('stop_time_update').get('stop_sequence')
			if stopsAway < 0:
				continue # Skips to next iteration
				
			# Find delay
			delayField = stopDict.get('trip_update').get('stop_time_update').get('departure')
			if delayField != None:
				delay = delayField.get('delay')
			else:
				delay = stopDict.get('trip_update').get('stop_time_update').get('arrival').get('delay')
			
			# Calculate due time
			dueTime = time.mktime(schTimeTup) + delay
			dueSecs = dueTime - time.time()
			
			# Find location
			locDict = next((item for item in locData if item['vehicle'].get('trip').get('trip_id') == tripDataTrip), None)
			lat = locDict.get('vehicle').get('position').get('latitude')
			long = locDict.get('vehicle').get('position').get('longitude')
			timestamp = locDict.get('vehicle').get('timestamp')
			array[k] = tripDataTrip
			secsAgo = time.time() - timestamp
			
			# Format numbers
			timeAgo = '-%d:%02d' % (secsAgo/60, secsAgo%60)
			delayStr = '%+d:%02d' % (delay/60, delay%60)
			dueStr = '%+d:%02d' % (dueSecs/60, dueSecs%60)
			lat = '%.5f' % (lat)
			long = '%.5f' % (long)
			
		elif schTimeTup > time.localtime():
			# Make strings blank
			lat = ''
			long = ''
			timeAgo = ''
			stopsAway = ''
			dueStr = ''
			delayStr = ''
			
		else:
			continue # Skips to next iteration
			
		# Print output line
		schTimeStrNew = time.strftime('%I:%M %p', schTimeTup)
		print("%-3d  %-4s   %-10s    %-10s   %-5s      %-3s     %-6s   %-6s  %-8s" \
			% (k, route, lat, long, timeAgo, stopsAway, delayStr, dueStr, schTimeStrNew))
		k += 1
	
	# Ask for which bus	
	print("\n")
	k = int(input("Enter bus number to track: "))
	tripDataTrip = array[k]
	
	# Check location every 10s and reopen if changed
	print("\n")
	while True:
		
		#print(tripDataTrip)
		# Get new data from api
		#webbrowser.open(apiRoot + realtime + getAuthorization() + '&tripid=' + tripDataTrip)
		newData = getJSON(apiRoot + realtime + getAuthorization() + '&tripid=' + tripDataTrip).get('response').get('entity')	# ~2KB
		stopDict = newData[0]
		locDict = newData[1]
		lat2 = locDict.get('vehicle').get('position').get('latitude')
		long2 = locDict.get('vehicle').get('position').get('longitude')
		if lat == None:
			lat = lat2
			long = long2
		stopsAway = stopSeq - stopDict.get('trip_update').get('stop_time_update').get('stop_sequence')
		
		# Ends program if stop reached
		if stopsAway < 0:
			print("\nBUS HAS DEPARTED\n")
			break
		
		# If new data is different reopen browser
		if (lat2 != lat) | (long2 != long):
			lat = lat2
			long = long2
			mapsUrl = 'https://www.google.co.nz/maps/place/' + str(lat) + '+' + str(long)
			webbrowser.open(mapsUrl)
			print("\nNew location found! The bus is %d stops away.\n" % (stopsAway))
		else:
			print("\nLocation unchanged :(\n")
			
		# Count down 10s
		for j in range(0,10):
			print("waiting: " + str(10-j))
			time.sleep(1)
	
main()
