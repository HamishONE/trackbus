<?php include "key.php"; ?>

<html>
<head>

	<meta name=viewport content="width=device-width, initial-scale=1">

	<script src="https://ajax.googleapis.com/ajax/libs/jquery/2.2.2/jquery.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.2/rollups/sha1.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.2/rollups/hmac-sha1.js"></script>
	<script src="https://maps.googleapis.com/maps/api/js?key=<?php echo $key; ?>&callback=initMap" async defer></script>
	<script src="buses.js"></script>
	
	<style>	
		.main {
			width: 95%;
			margin:0 auto;
		}
		.map {
			height: 80%;
			width: 100%;
		}
		.container-div {
			height: 15%; 
			width: 100%; 
			background-color: #81D4FA;
		}
		.left-div {
			float: left;
			width: 20%;
			height: 100px;
			margin-right: 8px;
		}
		.right-div {
			margin-left: 10%;
		}
		.button {
			background-color: #F06292;
		}
	</style>

</head>
<body>

	<div class="main">
		<div id="map" class="map"></div>
		<div class="container-div">
			<div class="left-div">
				<button onclick="refresh()" class="button">Refresh</button>
				<br>
				<!--button onclick="shift_up += 0.0001; refresh();" class="button">Up</button>
				<br>
				<button onclick="shift_up -= 0.0001; refresh();" class="button">Down</button>
				<br><br>
				<button onclick="shift_right -= 0.0001; refresh();" class="button">Left</button>
				<button onclick="shift_right += 0.0001; refresh();" class="button">Right</button-->
			</div>
			<div class="right-div">
				<p id="message">Trip ID:<br>Route ID:<br>Start time:</p>
				<!--button onclick="alert('shift_up='+shift_up+' shift_right='+shift_right);" class="button">Publish</button-->
			</div>
		</div>		
	</div>

</body>
</html>