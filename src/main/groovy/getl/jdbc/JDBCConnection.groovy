package getl.jdbc

/**
 GETL - based package in Groovy, which automates the work of loading and transforming data. His name is an acronym for �Groovy ETL�.

 GETL is a set of libraries of pre-built classes and objects that can be used to solve problems unpacking,
 transform and load data into programs written in Groovy, or Java, as well as from any software that supports
 the work with Java classes.
 
 Copyright (C) 2013  Alexsey Konstantonov (ASCRUS)

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License and
 GNU Lesser General Public License along with this program.
 If not, see <http://www.gnu.org/licenses/>.
*/

import getl.data.Connection
import getl.data.Dataset
import getl.exception.ExceptionGETL
import getl.jdbc.JDBCDataset.Type
import getl.utils.*
import groovy.sql.Sql
import groovy.transform.InheritConstructors

/**
 * JDBC connection class
 * @author Alexsey Konstantinov
 *
 */
@InheritConstructors
class JDBCConnection extends Connection {
	JDBCConnection () {
		super(driver: JDBCDriver)
	}
	
	JDBCConnection (Map params) {
		super(params + ((params.driver == null)?[driver: JDBCDriver]:[:]))
		if (this.getClass().name == 'getl.jdbc.JDBCConnection') methodParams.validation("Super", params)
	}
	
	/**
	 * Register connection parameters with method validator
	 */
	@Override
	protected void registerParameters () {
		super.registerParameters()
		methodParams.register("Super", ["login", "password", "connectURL", "sqlHistoryFile", "autoCommit", "connectProperty", "dbName",
			"javaConnection", "maskDate", "maskDateTime", "sessionProperty", "maskTime", "schemaName", "driverName", "connectHost", "connectDatabase",
			"balancer", "fetchSize", "loginTimeout"])
	}
	
	@Override
	protected void onLoadConfig (Map configSection) {
		super.onLoadConfig(configSection)
		if (this.getClass().name == 'getl.jdbc.JDBCConnection') methodParams.validation("Super", params)
	}

	@Override
	protected void doDoneConnect () {
		super.doDoneConnect()
		JDBCDriver drv = driver
		sysParams.sessionID = drv.sessionID()
		drv.saveToHistory("-- USER CONNECT (URL: ${sysParams."currentConnectURL"})${(autoCommit)?' WITH AUTOCOMMIT':''}")
	}
	
	protected void doDoneDisconnect () {
		JDBCDriver drv = driver
		drv.saveToHistory("-- USER DISCONNECT (URL: ${sysParams."currentConnectURL"})${(autoCommit)?' WITH AUTOCOMMIT':''}")
		super.doDoneDisconnect()
		sysParams.sessionID = null
	}
	
	/**
	 * Use exists JDBC connection 
	 * @return
	 */
	public java.sql.Connection getJavaConnection () { params.javaConnection }
	public void setJavaConnection (java.sql.Connection value) { params.javaConnection = value }
	
	/**
	 * JDBC connection URL
	 * @return
	 */
	public String getConnectURL () { params.connectURL }
	public void setConnectURL (String value) { params.connectURL = value }
	
	/**
	 * Build jdbc connection url
	 * @return
	 */
	public String currentConnectURL () { sysParams."currentConnectURL" }
	
	/**
	 * Server host for connection url
	 * @return
	 */
	public String getConnectHost () { params."connectHost" }
	public void setConnectHost (String value) {
		params."connectHost" = value
	}
	
	/**
	 * Database name for connection url
	 * @return
	 */
	public String getConnectDatabase () { params."connectDatabase" }
	public void setConnectDatabase (String value) { params."connectDatabase" = value }
	
	/**
	 * Connection balancer
	 * @return
	 */
	public Balancer getBalancer () { params."balancer" }
	public void setBalancer (Balancer value) { params."balancer" = value }
	
	/**
	 * JDBC driver name
	 * @return
	 */
	public String getDriverName () { params.driverName }
	public void setDriverName (String value) { params.driverName = value }
	
	/**
	 * Connection login
	 * @return
	 */
	public String getLogin () { params.login }
	public void setLogin (String value) { params.login = value }
	
	/**
	 * Connection password
	 * @return
	 */
	public String getPassword () { params.password } 
	public void setPassword (String value) { params.password = value }
	
	/**
	 * Auto commit transaction
	 * @return
	 */
	public boolean getAutoCommit () { ListUtils.NotNullValue([params.autoCommit, false]) }
	public void setAutoCommit (boolean value) { 
		params.autoCommit = value
		if (connected) driver.setAutoCommit(value)
	}
	
	/**
	 * Database name from access to objects in datasets
	 * @return
	 */
	public String getDbName () { params.dbName }
	public void setDbName (String value) { params.dbName = value }

	/**
	 * Schema name from access to objects in datasets
	 * @return
	 */
	public String getSchemaName () { params.schemaName }
	public void setSchemaName (String value) { params.schemaName = value }
	
	/**
	 * Extend connection properties
	 * @return
	 */
	public Map getConnectProperty () { 
		if (params.connectProperty == null) params.connectProperty = [:]
		params.connectProperty
	}
	public void setConnectProperty (Map value) {
		connectProperty.clear()
		addConnectionProperty(value)
	}
	public void addConnectionProperty (Map value) {
		connectProperty.putAll(value)
	}
	
	/**
	 * Session properties
	 * @return
	 */
	public Map getSessionProperty () {
		if (params.sessionProperty == null) params.sessionProperty = [:]
		params.sessionProperty
	}
	public void setSessionProperty (Map value) {
		sessionProperty.clear()
		addSessionProperty(value)
	}
	public addSessionProperty (Map value) {
		sessionProperty.putAll(value)
	}
	
	/**
	 * Default mask for date values
	 */
	public String getMaskDate () { params.maskDate }
	public void setMaskDate (String value) { params.maskDate = value }
	
	/**
	 * Default mask for time values
	 */
	public String getMaskTime () { params.maskTime }
	public void setMaskTime (String value) { params.maskTime = value }
	
	/**
	 * Default mask for datetime values
	 */
	public String getMaskDateTime () { params.maskDateTime }
	public void setMaskDateTime (String value) { params.maskDateTime = value }
	
	/*
	 * Name of file history sql commands
	 */
	public String getSqlHistoryFile () { params.sqlHistoryFile }
	public void setSqlHistoryFile (String value) { 
		params.sqlHistoryFile = value
		fileNameSqlHistory = null 
	}
	
	/**
	 * Fetch size records for read query 
	 */
	public Integer getFetchSize () { params.fetchSize }
	public void setFetchSize (Integer value) { params.fetchSize = value }
	
	/**
	 * Set login timeout for connection driver (in seconds) 
	 * @return
	 */
	public Integer getLoginTimeout () { params.loginTimeout }
	public void setLoginTimeout (Integer value) { params.loginTimeout = value }
	
	/**
	 * Return using groovy SQL connection
	 * @return
	 */
	public Sql getSqlConnection () { sysParams.sqlConnect }
	
	/**
	 * Return session ID (if supported RDBMS driver)
	 * @return
	 */
	public String getSessionID () { sysParams.sessionID }
	
	/**
	 * Return datasets list by parameters
	 * @param params
	 * @param code
	 * @return
	 */
	public List<TableDataset> retrieveDatasets (Map params, Closure code) {
		if (params == null) params = [:]
		List<Dataset> result = []
		def o = retrieveObjects(params, code)
		o.each { row ->
			TableDataset d = new TableDataset(connection: this, type: JDBCDataset.Type.UNKNOWN)
			d.with {
				d.autoSchema = true
				d.dbName = row.dbName
				d.schemaName = row.schemaName
				d.tableName = row.tableName
				if (row.type?.toUpperCase() == "TABLE") {
					d.type = JDBCDataset.Type.TABLE
				}
				else if (row.type?.toUpperCase() == "VIEW") {
					d.type = JDBCDataset.Type.VIEW
				}
				
				d.description = row.description
			}
			result << d
		}
		result
	}
	
	/**
	 * Return datasets list
	 * @return
	 */
	public List<Dataset> retrieveDatasets () {
		retrieveDatasets([:], null)
	}
	
	/**
	 * Return datasets list
	 * @param code
	 * @return
	 */
	public List<Dataset> retrieveDatasets (Closure code) {
		retrieveDatasets([:], code)
	}
	
	/**
	 * Return datasets list
	 * @param params
	 * @return
	 */
	public List<Dataset> retrieveDatasets (Map params) {
		retrieveDatasets(params, null)
	}
	
	@Override
	public String getObjectName () { connectURL }
	
	/**
	 * Save sql to history file
	 * @param sql
	 */
	public void saveToHistory(String sql) {
		JDBCDriver drv = driver
		drv.saveToHistory(sql)
	}
	
	/**
	 * Return used balancer server attributes
	 * @return
	 */
	public Map getBalancerServer () {
		sysParams."balancerServer"
	}
	
	/**
	 * Build connection params for connect url 
	 * @return
	 */
	public String buildConnectParams () {
		JDBCDriver drv = driver
		drv.buildConnectParams()
	}
	
	/**
	 * Parse connection host and return host name without port
	 * @param host
	 * @return
	 */
	public static String ConnectHost2HostName(String host) {
		if (host == null) return null
		def pos = host.indexOf(":")
		String res = host
		if (pos != -1) res = host.substring(0, pos)
		
		res
	}
	
	/**
	 * Parse connection host and return port number without host name
	 * @param host
	 * @return
	 */
	public static Integer ConnectHost2PortNumber(String host) {
		if (host == null) return null
		def pos = host.indexOf(":")
		Integer res
		if (pos != -1) res = Integer.valueOf(host.substring(pos + 1))
		
		res
	}
	
	/**
	 * Build connection host
	 * @param hostName
	 * @param portNumber
	 * @return
	 */
	public static String BuildConnectHost(String hostName, Integer portNumber) {
		if (hostName == null) throw new ExceptionGETL("Required value for \"hostName\" parameter")
		def res = hostName
		if (portNumber != null) res += ":$portNumber"
		
		res
	}
	
	/**
	 * Current host name
	 * @return
	 */
	public String getConnectHostName () {
		ConnectHost2HostName(connectHost)
	}
	
	/**
	 * Current port number
	 * @return
	 */
	public Integer getConnectPortNumber () {
		ConnectHost2PortNumber(connectHost)
	}
	
	/**
	 * Real script history file name
	 */
	protected String fileNameSqlHistory
	
	/**
	 * Validation script history file
	 */
	protected validSqlHistoryFile () {
		if (fileNameSqlHistory == null) {
//			fileNameSqlHistory = StringUtils.EvalMacroString(sqlHistoryFile.replace('\\', '\\\\'), [date: DateUtils.NowDate()])
			fileNameSqlHistory = StringUtils.EvalMacroString(sqlHistoryFile, StringUtils.MACROS_FILE)
			FileUtils.ValidFilePath(fileNameSqlHistory)
		}
	}
}