<?php

header('Content-Type: application/json');

include "preferences.php";
$mysqli = new mysqli("p:$host", $user, $password, $database, $port);
if ($mysqli->connect_errno) {
	echo "Failed to connect to MySQL: (" . $mysqli->connect_errno . ") " . $mysqli->connect_error;
}

$querry = "SELECT trip_id, bearing FROM bearings";
$res = $mysqli->query($querry);

echo "{\"data\": [";
for ($row_no=0; $row_no < $res->num_rows; $row_no++) {
	
    $res->data_seek($row_no);
    $row = $res->fetch_assoc();
	
	$trip_id = $row['trip_id'];
	$bearing = $row['bearing'];
	
	echo "{\"trip_id\": \"$trip_id\", \"bearing\": $bearing}";
	if ($row_no != $res->num_rows - 1) echo ",";
}
echo "]}";

?>
