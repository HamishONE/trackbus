-- phpMyAdmin SQL Dump
-- version 4.5.4.1deb2ubuntu2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Dec 03, 2016 at 01:14 AM
-- Server version: 5.7.16-0ubuntu0.16.04.1
-- PHP Version: 7.0.8-0ubuntu0.16.04.3

--
-- Table structure for table `bearings`
--
CREATE TABLE `bearings` (
  `trip_id` char(34) NOT NULL,
  `timestamp` int(11) NOT NULL,
  `bearing` smallint(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `buffer`
--
CREATE TABLE `buffer` (
  `api` varchar(20) NOT NULL,
  `data` mediumtext NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Indexes for table `bearings`
--
ALTER TABLE `bearings`
  ADD PRIMARY KEY (`trip_id`),
  ADD UNIQUE KEY `trip_id` (`trip_id`);

--
-- Indexes for table `buffer`
--
ALTER TABLE `buffer`
  ADD PRIMARY KEY (`api`),
  ADD UNIQUE KEY `api` (`api`);
