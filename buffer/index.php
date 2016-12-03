<?php

header('Content-Type: application/json');

include "preferences.php";
$mysqli = new mysqli("p:$host", $user, $password, $database, $port);
if ($mysqli->connect_errno) {
	echo "Failed to connect to MySQL: (" . $mysqli->connect_errno . ") " . $mysqli->connect_error;
}

$api = $_GET['api'];

$querry = "SELECT data FROM buffer WHERE api = '$api'";
$res = $mysqli->query($querry);

$res->data_seek(0);
$row = $res->fetch_assoc();
echo $row['data'];

?>
