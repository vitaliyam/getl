/*
 GETL - based package in Groovy, which automates the work of loading and transforming data. His name is an acronym for "Groovy ETL".

 GETL is a set of libraries of pre-built classes and objects that can be used to solve problems unpacking,
 transform and load data into programs written in Groovy, or Java, as well as from any software that supports
 the work with Java classes.
 
 Copyright (C) 2013-2015  Alexsey Konstantonov (ASCRUS)

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

package getl.transform

import getl.csv.CSVDataset
import getl.data.*
import getl.data.Field.Type
import getl.driver.Driver
import getl.driver.Driver.Operation
import getl.driver.Driver.Support
import getl.exception.ExceptionGETL
import groovy.transform.InheritConstructors

/**
 * Multiple writer driver class
 * @author Alexsey Konstantinov
 *
 */
@InheritConstructors
class MutlipleDatasetDriver extends Driver {

	@Override
	public List<Support> supported() {
		[Driver.Support.WRITE]
	}

	@Override
	public List<Operation> operations() {
		[]
	}

	@Override
	protected boolean isConnect() {
		return true
	}

	@Override
	protected void connect() {
	}

	@Override
	protected void disconnect() {
	}

	@Override
	protected List<Object> retrieveObjects(Map params, Closure filter) {
		throw new ExceptionGETL("Retrieve objects not supported")
	}
	
	private Map<String, Dataset> getDestinition(Dataset dataset) {
		Map<String, Dataset> ds = dataset.params.dest
		if (ds == null) throw new ExceptionGETL("Required MultipleDataset object class")
		if (ds.isEmpty()) throw new ExceptionGETL("Required set param \"dest\" with dataset")
		
		ds
	}

	@Override
	protected List<Field> fields(Dataset dataset) {
		throw new ExceptionGETL("Retrieve fields not supported")
	}
	
	private List<Field> destField(Dataset dataset) {
		Map<String, Dataset> ds = getDestinition(dataset)
		
		def alias = ds.keySet().toArray()
		ds.get(alias[0]).field
	}

	@Override
	protected void startTran() {
	}

	@Override
	protected void commitTran() {
	}

	@Override
	protected void rollbackTran() {
	}

	@Override
	protected void createDataset(Dataset dataset, Map params) {
	}

	@Override
	protected void dropDataset(Dataset dataset, Map params) {
	}

	@Override
	protected long eachRow(Dataset dataset, Map params, Closure prepareCode, Closure code) {
	}

	@Override
	protected void openWrite(Dataset dataset, Map params, Closure prepareCode) {
		Map<String, Dataset> ds = getDestinition(dataset)
		List<Field> fields = destField(dataset)
		ds.each { String alias, Dataset dest ->
			if (dest.field.isEmpty()) dest.field = fields
			dest.openWrite(params)
		}
		if (prepareCode != null) prepareCode(fields)
	}

	@Override
	protected void write(Dataset dataset, Map row) {
		Map<String, Dataset> ds = getDestinition(dataset)
		Map<String, Closure> cond = dataset.params.condition?:[:]
		
		// Valid conditions and write only filtered rows		
		cond.each { String alias, Closure valid ->
			Dataset dest = ds.get(alias)
			if (dest == null) throw new ExceptionGETL("Unknown alias dataset \"$alias\"")
			if (valid(row)) {
				dest.write(row)
			}
		}

		// Write rows to dataset has not conditions
		ds.each { String alias, Dataset dest ->
			if (!cond.containsKey(alias)) {
				dest.write(row)
			}
		}
	}

	@Override
	protected void doneWrite(Dataset dataset) {
		Map<String, Dataset> ds = getDestinition(dataset)
		ds.each { String alias, Dataset dest ->
			dest.doneWrite()
		}
	}

	@Override
	protected void closeWrite(Dataset dataset) {
		Map<String, Dataset> ds = getDestinition(dataset)
		ds.each { String alias, Dataset dest ->
			dest.closeWrite()
		}
	}

	@Override
	protected void bulkLoadFile(CSVDataset source, Dataset dest, Map params, Closure prepareCode) {
	}

	@Override
	protected void clearDataset(Dataset dataset, Map params) {
	}

	@Override
	protected long executeCommand(String command, Map params) {
		throw new ExceptionGETL("Execution command not supported")
	}

	@Override
	public long getSequence(String sequenceName) {
		throw new ExceptionGETL("Sequence not supported")
	}

}
