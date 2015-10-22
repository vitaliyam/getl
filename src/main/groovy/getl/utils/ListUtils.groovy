package getl.utils

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

/**
 * List library functions class
 * @author Alexsey Konstantinov
 *
 */
@groovy.transform.CompileStatic
class ListUtils {
	/**
	 * Return to list items that fit the specified condition 
	 * @param list
	 * @param from
	 * @return
	 */
	public static List CopyWhere(List list, Closure from) {
		if (list == null) return
		
		def result = []
		list.each {  
			if (from(it)) result << it
		}
		result
	}
	
	/**
	 * Enclose each item in the list in quotes
	 * @param list
	 * @param quote
	 * @return
	 */
	public static List<String> QuoteList(List list, String quote) {
		if (list == null) return null
		
		def res = []
		list.each { res << "${quote}${it}${quote}" }
		res
	}
	
	/**
	 * Return the sorted list for a given condition
	 * @param list
	 * @param closure
	 * @return
	 */
	public static List SortListTo(List list, Closure closure) {
		if (list == null) return null
		
		list.sort(false, closure)
	}
	
	/**
	 * Sort the list by a specified condition
	 * @param list
	 * @param closure
	 */
	public static void SortList(List list, Closure closure) {
		if (list == null) return
		
		list.sort(true, closure)
	}
	
	/**
	 * Convert all element of list to lower case
	 * @param list
	 * @return
	 */
	public static List<String> ToLowerCase(List<String> list) {
		if (list == null) return null
		
		def res = []
		list.each {
			res << it.toLowerCase()
		}
		res
	}
	
	/**
	 * Convert all element of list to upper case
	 * @param list
	 * @return
	 */
	public static List<String> ToUpperCase(List<String> list) {
		if (list == null) return null
		
		def res = []
		list.each {
			res << it.toUpperCase()
		}
		res
	}
	
	/**
	 * Return first not null value from list
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static def NotNullValue(List value) {
		if (value == null) return null
		
		def res = value.find { it != null }
		
		res
	}
	
	/**
	 * Evaluate macros for value in list
	 * @param value
	 * @param vars
	 * @return
	 */
	public static List EvalMacroValues(List value, Map vars) {
		def res = []
		
		value.each { v ->
			if (v instanceof String || v instanceof GString) { 
				res << GenerationUtils.EvalGroovyScript('"""' + ((String)v).replace("\\", "\\\\") + '"""', vars)
			}
			else if (v instanceof List) {
				res << EvalMacroValues((List)v, vars)
			}
			else if (v instanceof Map) {
				res << MapUtils.EvalMacroValues((Map)v, vars)
			}
			else {
				res << v
			}
		}
		
		res
	}

	/**
	 * Split string and return list	
	 * @param value
	 * @param expr
	 * @return
	 */
	public static List Split(String value, String expr) {
		if (value == null) return null
		
		String[] res = value.split(expr)
		(List)res.collect()
	} 
	
	public static String ToJson(List list) {
		if (list == null) return null
		
		StringBuilder sb = new StringBuilder()
		sb << "[\n"
		list.each { sb << "	$it,\n" }
		sb << "]"
		
		sb.toString()
	}
}