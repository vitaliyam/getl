package getl.utils

import getl.proc.*
import getl.jdbc.*

class SQLClient extends Job {
	
	static main(args) {
		new SQLClient().run(args)
	}

	@Override
	public void process() {
		println "SQL client, (c) EasyData LTD"
		if (args."connection" == null && args."script" == null) {
			println """
*** Run syntax:"
SQLScripter config.filename=<Config file name> connection=<Connection name> script=<Script file name> var.<Variable name>=<Variable value>"

*** Config syntax:"
{
  "log": {
    "file": "<Log file name>"
  },

  "connections": {
    "<Connection name>": {
      "connection": "<Connection class>",
      "connectionURL": "<Connection url path>",
      "login": "<Connect login>",
      "password": "<Connect password>"
    }
  },

  "vars": {
    "<Variable name>": "<Variable value>"
  }
}

*** Avaible connection classes:
getl.h2.H2Connection
getl.mssql.MSSQLConnection
getl.mysql.MySQLConnection
getl.oracle.OracleConnection
getl.postgresql.PostgreSQLConnection
getl.vertica.VerticaConnection

*** Script command:
{<Variable name>}
- include variable value to script

ECHO
- echo text to console

SET SELECT <Select query> ;
- take the field values first row to the variables

IF <Condition>;
<SQL operations>
END IF;
- execute operations if condition true

FOR SELECT <Select query> ;
<SQL operations>
END FOR;
- execute operations for each rows

/*:<Variable name>*/
<DML operator>
- run DML operator and set processed row count to variable

Example script with variable var.RunVar=DEMO:
IF '{RunVar}' = 'DEMO';
  ECHO RunVar has value: {RunVar}
END IF;

ECHO Process table1 rows
FOR SELECT id AS group_id FROM table1 ORDER BY id;
  /*:update_count*/ 
  UPDATE table2 SET value + 1 WHERE group_id = {group_id};
  ECHO Update {update_count} rows for {group_id} group

  SET SELECT Avg(value) AS avg_value FROM table2 WHERE group_id = {group_id};
  ECHO Detected {avg_value} average value for {group_id} group
END FOR;"""
			return
		}
		
		String connectionName = args."connection"
		JDBCConnection connection = JDBCConnection.CreateConnection(config: connectionName)
		
		
		getl.jdbc.SQLScripter scripter = new getl.jdbc.SQLScripter(connection: connection, logEcho: "INFO")
		scripter.vars.putAll(Config.content."vars")
		scripter.loadFile(args."script", "utf-8")
		scripter.runSql()
		Logs.Info("Finish, ${scripter.rowCount} rows processed")
	}
}