/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `botcommandparameters`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `botcommandparameters` (
  `id` int(11) NOT NULL,
  `botcommandid` int(11) NOT NULL,
  `k` varchar(256) NOT NULL,
  `v` varchar(256) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `botcommandparametersid_UNIQUE` (`id`),
  KEY `botcommandparameters_botcommand_id_fk_idx` (`botcommandid`),
  CONSTRAINT `botcommandparameters_botcommand_id_fk` FOREIGN KEY (`botcommandid`) REFERENCES `botcommands` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `botcommands`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `botcommands` (
  `id` int(11) NOT NULL,
  `bot` int(11) NOT NULL,
  `command` varchar(256) NOT NULL,
  `queuedat` int(11) NOT NULL,
  `expiresat` int(11) NOT NULL,
  `attempts` int(11) NOT NULL,
  `onfail` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `botcommandid_UNIQUE` (`id`),
  KEY `botcommands_bot_index` (`bot`),
  CONSTRAINT `botcommands_bots_botid` FOREIGN KEY (`bot`) REFERENCES `bots` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `botconfig`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `botconfig` (
  `botid` int(11) NOT NULL,
  `configkey` varchar(45) NOT NULL,
  `configvalue` mediumtext,
  PRIMARY KEY (`botid`,`configkey`),
  KEY `botconfig_botid` (`botid`),
  CONSTRAINT `botconfig_bots_botid` FOREIGN KEY (`botid`) REFERENCES `bots` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bots`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bots` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `firstname` varchar(32) NOT NULL,
  `lastname` varchar(32) NOT NULL,
  `password` varchar(64) NOT NULL,
  `ownerid` int(11) NOT NULL,
  `lockedby` int(11) NOT NULL DEFAULT '-1',
  `lockeduntil` int(11) NOT NULL DEFAULT '0',
  `lockedserial` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `botownerid_idx` (`ownerid`),
  CONSTRAINT `botownerid` FOREIGN KEY (`ownerid`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schemaversions`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schemaversions` (
  `name` varchar(64) NOT NULL,
  `version` int(11) NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

insert into `schemaversions`(`name`,`version`) values('jslbotbridge',1);
