#!/usr/bin/python3
# REQUIRES: PyMySql (pip install pymysql) AND preferences.py file (create from sample)

# Get preference
import os.path
if not os.path.isfile("./preferences.py"):
	print("You need to include a preferences.py file in the directory.")
	print("Rename the sample file and edit it as required.")
	print("You will need the connection details for a MySQL server with the tables created.")
	quit()
import preferences
host,port,port,database,user,password,timeout_num,refresh_freq = preferences.preferences()

# AT Apis
apiRoot = 'https://api.at.govt.nz/v2/'
apiKey = "?subscription-key=323741614c1c4b9083299adefe100aa6"
tripupdates = 'public/realtime/tripupdates/'
realtime = 'public/realtime/'
vehiclelocations = 'public/realtime/vehiclelocations/'
routes = 'gtfs/routes/'
ferrys = "https://api.at.govt.nz/v1/api_node/realTime/ferryPositions?callback=lol"

# Imports
import time
import urllib.request
import json
import math
import pymysql.cursors
import datetime
import sys
import urllib.error
import socket
import threading

def addToDb(key, url, conn):

	# Retrieve data
	try:
		data = urllib.request.urlopen(url).read().decode('utf-8')
	except urllib.error.HTTPError as e:
		print(key + ": URL ERROR: " + str(e.code))
		sys.stdout.flush()
		return None
	except socket.error as e:
		print(key + ": SOCKET ERROR: " + str(e))
		sys.stdout.flush()
		return None
	except Exception as e:
		print(key + ": GENERIC EXCEPTION: " + str(e))
		sys.stdout.flush()
		return None
	
	# Write to database
	stmt = "REPLACE INTO buffer VALUES (%s, %s)"
	c = conn.cursor()
	c.execute(stmt, (key, data))
	conn.commit()
	
	return data

def logBearings(out, conn):

	if out == None:
		return
	
	locData = json.loads(out).get('response').get('entity')
	data = []
	
	# Loop through locData
	for bus in locData:
		
		# Extract data
		trip_id = bus.get('vehicle').get('trip').get('trip_id')
		bearing = bus.get('vehicle').get('position').get('bearing')
		timestamp = bus.get('vehicle').get('timestamp')
		
		# Reject if no bearing
		if bearing == None:
			continue
		
		# Add data to list
		tup = (trip_id, timestamp, bearing)
		data.append(tup)
	
	# Write to database
	stmt = "REPLACE INTO bearings VALUES (%s, %s, %s)"
	c = conn.cursor()
	c.executemany(stmt, data)
	
	# Remove entries older than 120s old
	old_time = math.floor(time.time()) - 120
	sql = "DELETE FROM bearings WHERE timestamp < %s"
	c.execute(sql, old_time)
	
	conn.commit()
	print("bearings updated")
	sys.stdout.flush()

def updateApi(key, url):

	# Connect to database
	conn = pymysql.connect(host=host, port=port, user=user, password=password,
			db=database, charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)

	# Loop forever
	while True:
		start = time.time()
		
		# Get data and write to database
		out = addToDb(key, url, conn)	
		if key == 'vehiclelocations':
			logBearings(out, conn)
		
		# Wait until at least update frequency has passed
		time_taken = time.time() - start
		if out != None:
			print("%s updated: time taken = %3.1fsecs"%(key, time_taken))
			sys.stdout.flush()
		sleep_time = refresh_freq - time_taken
		if sleep_time > 0:
			time.sleep(sleep_time)

# Print header
print("\n" + "*"*50)
print("START OF BUFFER APP @ " + str(datetime.datetime.now()))
print("*"*50 + "\n")
print("Calling the apis every %dsecs (maximum)"%refresh_freq)
print("Requests will time out after %dsecs\n"%timeout_num)
sys.stdout.flush()
	
# Set timeout
socket.setdefaulttimeout(timeout_num)
	
# Start a thread for each API
threading.Thread(target=updateApi, args=['tripupdates', apiRoot + tripupdates + apiKey]).start()
threading.Thread(target=updateApi, args=['routes', apiRoot + routes + apiKey]).start()
threading.Thread(target=updateApi, args=['ferrys', ferrys]).start()
threading.Thread(target=updateApi, args=['realtime', apiRoot + realtime + apiKey]).start()
threading.Thread(target=updateApi, args=['vehiclelocations', apiRoot + vehiclelocations + apiKey]).start()
