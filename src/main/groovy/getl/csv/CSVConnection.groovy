package getl.csv

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

import getl.csv.CSVDataset.QuoteMode
import getl.data.FileConnection
import getl.utils.*
import groovy.transform.InheritConstructors

/**
 * CSV connection class
 * @author Alexsey Konstantinov
 *
 */
@InheritConstructors
class CSVConnection extends FileConnection {
	CSVConnection () {
		super(driver: CSVDriver)
	}
	
	CSVConnection (Map params) {
		super(new HashMap([driver: CSVDriver]) + params)
		
		methodParams.register("Super", ["quoteStr", "fieldDelimiter", "rowDelimiter", "header", "escaped", 
										"nullAsValue", "quoteMode", "decimalSeparator", "formatDate", "formatTime", 
										"formatDateTime", "ignoreHeader"])
		if (this.getClass().name == 'getl.csv.CSVConnection') methodParams.validation("Super", params)
	}
	
	/**
	 * Quote delimiter string
	 */
	public String getQuoteStr () { ListUtils.NotNullValue([params.quoteStr, '"']) }
	public void setQuoteStr (String value) { params.quoteStr = value }
	
	/**
	 * Field delimiter
	 */
	public String getFieldDelimiter () { ListUtils.NotNullValue([params.fieldDelimiter, ","]) }
	public void setFieldDelimiter (String value) { params.fieldDelimiter = value }
	
	/**
	 * Row delimiter
	 */
	public String getRowDelimiter () { ListUtils.NotNullValue([params.rowDelimiter, "\n"]) }
	public void setRowDelimiter (String value) { params.rowDelimiter = value }
	
	/**
	 * File has header of fields name
	 */
	public boolean getHeader () { ListUtils.NotNullValue([params.header, true]) }
	public void setHeader (boolean value) { params.header = value }
	
	/**
	 * Ignore header field name
	 */
	public boolean getIgnoreHeader () { ListUtils.NotNullValue([params.ignoreHeader, false]) }
	public void setIgnoreHeader (boolean value) { params.ignoreHeader = value }
	
	/**
	 * Required convert string to escape value
	 */
	public boolean getEscaped () { ListUtils.NotNullValue([params.escaped, false]) }
	public void setEscaped (boolean value) { params.escaped = value }
	
	/**
	 * Convert NULL to value
	 */
	public String getNullAsValue () { params.nullAsValue }
	public void setNullAsValue (String value) { params.nullAsValue = value }
	
	/**
	 * Required format values for output to file
	 */
	public boolean getFormatOutput () { ListUtils.NotNullValue([params.formatOutput, true]) }
	public void setFormatOutput (boolean value) { params.formatOutput = value }
	
	/**
	 * Mode of quote value
	 */
	public QuoteMode getQuoteMode () { ListUtils.NotNullValue([params.quoteMode, QuoteMode.NORMAL]) }
	public void setQuoteMode (QuoteMode value) { params.quoteMode = value }
	
	/**
	 * Decimal separator for number fields
	 */
	public String getDecimalSeparator () { params.decimalSeparator?:"." }
	public void setDecimalSeparator (String value) { params.decimalSeparator = value }
	
	/**
	 * Format for date fields 
	 */
	public String getFormatDate () { params.formatDate }
	public void setFormatDate (String value) { params.formatDate = value }
	
	/**
	 * Format for time fields
	 */
	public String getFormatTime () { params.formatTime }
	public void setFormatTime (String value) { params.formatTime = value }
	
	/**
	 * Format for datetime fields
	 */
	public String getFormatDateTime () { params.formatDateTime }
	public void setFormatDateTime (String value) { params.formatDateTime = value }
}