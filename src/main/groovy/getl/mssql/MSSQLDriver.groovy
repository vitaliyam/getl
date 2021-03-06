/*
 GETL - based package in Groovy, which automates the work of loading and transforming data. His name is an acronym for "Groovy ETL".

 GETL is a set of libraries of pre-built classes and objects that can be used to solve problems unpacking,
 transform and load data into programs written in Groovy, or Java, as well as from any software that supports
 the work with Java classes.
 
 Copyright (C) 2013-2017  Alexsey Konstantonov (ASCRUS)

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

package getl.mssql

import getl.data.Field

import java.sql.PreparedStatement
import getl.data.Dataset
import getl.driver.Driver
import getl.jdbc.JDBCDriver
import groovy.transform.InheritConstructors


/**
 * MSSQL driver class
 * @author Alexsey Konstantinov
 *
 */
@InheritConstructors
class MSSQLDriver extends JDBCDriver {
	MSSQLDriver () {
		super()
		
		methodParams.register('eachRow', ['with'])

		defaultSchemaName = "dbo"
		fieldPrefix = '['
		fieldEndPrefix = ']'
		tablePrefix = '['
		tableEndPrefix = ']'
		commitDDL = true
	}

	@Override
	public List<Driver.Support> supported() {
		return super.supported() +
				[Driver.Support.SEQUENCE, Driver.Support.BLOB, Driver.Support.CLOB,
				 Driver.Support.INDEX, Driver.Support.UUID, Driver.Support.TIME, Driver.Support.DATE,
				 Driver.Support.BOOLEAN]
	}

	@Override
	public List<Driver.Operation> operations() {
		return super.operations() +
				[Driver.Operation.CLEAR, Driver.Operation.DROP, Driver.Operation.EXECUTE, Driver.Operation.CREATE]
	}

	@Override
	public Map getSqlType () {
		Map res = super.getSqlType()
		res.DOUBLE.name = 'float'
		res.BOOLEAN.name = 'bit'
		res.BLOB.name = 'varbinary'
		res.BLOB.useLength = JDBCDriver.sqlTypeUse.ALWAYS
		res.TEXT.name = 'varchar'
		res.TEXT.useLength = JDBCDriver.sqlTypeUse.ALWAYS
		res.DATETIME.name = 'datetime'
		res.UUID.name = 'uniqueidentifier'

		return res
	}

	@Override
	public String defaultConnectURL () {
		return 'jdbc:sqlserver://{host};databaseName={database}'
	}
	
	@Override
	public void sqlTableDirective (Dataset dataset, Map params, Map dir) {
		if (params.with != null) {
			dir.afteralias = "with (${params."with"})"
		}
	}
	
	@Override
	protected String sessionID() {
		String res = null
		def rows = sqlConnect.rows('SELECT @@SPID AS session_id')
		if (!rows.isEmpty()) res = rows[0].session_id.toString()
		
		return res
	}

	@Override
	protected String getChangeSessionPropertyQuery() { return 'SET {name} {value}' }

	@Override
	public void prepareField (Field field) {
		super.prepareField(field)

		if (field.typeName != null) {
			if (field.typeName.matches("(?i)TEXT")) {
				field.type = Field.Type.TEXT
				field.dbType = java.sql.Types.CLOB
				return
			}

			if (field.typeName.matches("(?i)UNIQUEIDENTIFIER")) {
				field.type = Field.Type.UUID
				field.dbType = java.sql.Types.VARCHAR
				field.length = 36
				field.precision = null
//				return
			}
		}
	}

	@Override
	public boolean blobReadAsObject () { return false }
}
