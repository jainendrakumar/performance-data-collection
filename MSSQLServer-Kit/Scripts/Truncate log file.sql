ALTER DATABASE DATABASENAME SET RECOVERY SIMPLE;
DBCC SHRINKFILE (DATABASENAME_log, 1, TRUNCATEONLY);
ALTER DATABASE DATABASENAME SET RECOVERY FULL;