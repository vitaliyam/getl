package getl.files

/*
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


import getl.data.*
import getl.exception.ExceptionGETL
import getl.jdbc.*
import getl.proc.Flow
import getl.utils.*
import getl.tfs.*
import groovy.sql.Sql

import java.sql.PreparedStatement
import java.sql.ResultSet

import groovy.transform.Synchronized

/**
 * File manager abstract class
 * @author Alexsey Konstantinov
 *
 */
abstract class Manager {
	protected ParamMethodValidator methodParams = new ParamMethodValidator()
	protected File localDirFile = new File(TFS.storage.path)
	
	Manager () {
		methodParams.register("super", ["rootPath", "localDirectory", "scriptHistoryFile"])
		methodParams.register("buildList", ["path", "type", "maskFile", "recursive", "story", "takePathInStory"])
		methodParams.register("downloadFiles", ["deleteLoadedFile", "story", "ignoreError", "folders", "filter", "order"])
		
		countFileList = 0
		
		initMethods()
		
		if (fileListName == null) {
			fileList = TDS.dataset(tableName: "FILE_MANAGER_${StringUtils.RandomStr().replace("-", "_").toUpperCase()}") 
		} 
		else {
			fileList = TDS.dataset(tableName: fileListName)
		} 
	}
	
	public static Manager BuildManager (String name) {
		Map fileParams = Config.content."files"?."$name"
		if (fileParams == null) throw new ExceptionGETL("File manager \"$name\" not found in \"files\" section by config")
		def className = fileParams."manager"
		if (className == null) throw new ExceptionGETL("Reqired class name as \"manager\" property in \"files.$name\" file server")
		Manager manager = Class.forName(className).newInstance()
		manager.params.putAll(MapUtils.CleanMap(fileParams, ["manager"]))
		manager.validateParams()

		manager
	}

	/**
	 * Type of file in list
	 */
	public enum TypeFile {FILE, DIRECTORY, LINK, ALL}
	
	/**
	 * Parameters
	 */
	public Map params = [:]
	
	/**
	 * Root path
	 */
	public String getRootPath () { params.rootPath }
	public void setRootPath (String value) {
		params.rootPath = value
	}
	
	/**
	 * Local directory
	 */
	public String getLocalDirectory () { params.localDirectory }
	public void setLocalDirectory (String value) { 
		FileUtils.ValidPath(value)
		params.localDirectory = value
		localDirFile = new File(value)
	}
	
	/**
	 * Log script file on running commands 
	 */
	public String getScriptHistoryFile () { params.scriptHistoryFile }
	public void setScriptHistoryFile (String value) { 
		params.scriptHistoryFile = value
		fileNameScriptHistory = null 
	}
	
	/**
	 * Name section parameteres value in config file
	 * Store parameters to config file from section "FTPSERVERS"
	 */
	private String config
	
	public String getConfig () { config }
	public void setConfig (String value) {
		config = value
		if (config != null) {
			if (Config.ContainsSection("files.${this.config}")) {
				doInitConfig()
			}
			else {
				Config.RegisterOnInit(doInitConfig)
			}
		}
	}
	
	private final Closure doInitConfig = {
		if (config == null) return
		Map cp = Config.FindSection("files.${config}")
		if (cp.isEmpty()) throw new ExceptionGETL("Config section \"files.${config}\" not found")
		methodParams.validation("super", cp)
		onLoadConfig(cp)
		Logs.Config("Load config \"files\".\"config\" for object \"${this.getClass().name}\"")
	}
	
	/**
	 * Validate parameters
	 * @return
	 */
	public validateParams () {
		methodParams.validation("super", params)
	}
	
	/**
	 * Init configuration load
	 * @param configSection
	 */
	protected void onLoadConfig (Map configSection) {
		MapUtils.MergeMap(params, configSection)
		if (configSection.containsKey("localDirectory")) {
			setLocalDirectory(params.localDirectory)
		}
		else {
			params.localDirectory = localDirFile.absolutePath
		}
	}
	
	/**
	 * File name is case-sensitive
	 * @return
	 */
	public abstract boolean isCaseSensitiveName ()
	
	/**
	 * Init validator methods
	 */
	protected void initMethods () { }
	
	/**
	 * Connect to server
	 */
	public abstract void connect ()
	
	/**
	 * Disconnect from server
	 */
	public abstract void disconnect ()
	
	/**
	 * Return list files of current directory from server
	 * Parameters node list: fileName, fileSize, fileDate
	 * @param maskFiles
	 * @return
	 */
	public abstract void list (String maskFiles, Closure processCode)
	
	/**
	 * Return list files of current directory from server
	 */
	public void list (Closure processCode) {
		list(null, processCode)
	}
	
	/**
	 * Return list files of current directory from server
	 * @param maskFiles
	 * @return
	 */
	public List<Map> list (String maskFiles) {
		List<Map> res = []
		Closure addToList = { res << it }
		list(maskFiles, addToList)
		
		res
	}
	
	/**
	 * Return list files of current directory from server
	 * @return
	 */
	public List<Map> list () {
		List<Map> res = []
		Closure addToList = { res << it }
		list(null, addToList)
		
		res
	}
	
	/**
	 * Absolute current path
	 */
	public abstract String getCurrentPath ()
	
	/**
	 * Set new absolute current path
	 */
	public abstract void setCurrentPath (String path)
	
	/**
	 * Change current server directory
	 * @param dir
	 */
	public void changeDirectory (String dir) {
		if (dir == null) throw new ExceptionGETL("Null dir not allowed for cd operation")
		if (dir == '.') return
		if (dir == '..') {
			changeDirectoryUp()
			return
		}
		
		if (dir[0] == '/') {
			try {
				currentPath = dir
			}
			catch (Exception e) {
				Logs.Severe("Can not change directory to \"$dir\"")
				throw e
			}
		}
		else {
			try {
				currentPath = "$currentPath/$dir"
			}
			catch (Exception e) {
				Logs.Severe("Can not change directory to \"$currentPath/$dir\"")
				throw e
			}
		}
	}
	
	/**
	 * Change current directory to parent directory 
	 */
	public abstract void changeDirectoryUp ()
	
	/**
	 * Change current directory to root
	 */
	public void changeDirecoryToRoot () {
		currentPath = rootPath
	}
	
	/**
	 * Download file from server
	 * @param fileName
	 */
	public abstract void download (String fileName, String path, String localFileName)
	
	/**
	 * Download file from server
	 * @param fileName
	 */
	public void download (String fileName) {
		download(fileName, fileName)
	}
	
	/**
	 * Download file from server
	 * @param fileName
	 * @param localFileName
	 */
	public void download (String fileName, String localFileName) {
		def ld = currentLocalDir()
		if (ld != null) FileUtils.ValidPath(ld)
		download(fileName, ld, localFileName)
	}
	
	/**
	 * Upload file to server
	 * @param fileName
	 */
	public abstract void upload (String path, String fileName)
	
	/**
	 * Upload file to server
	 * @param fileName
	 */
	public void upload (String fileName) {
		upload(currentLocalDir(), fileName)
	}
	
	/**
	 * Remove file from server
	 * @param fileName
	 */
	public abstract void removeFile (String fileName)
	
	/**
	 * Create directory from server
	 * @param dirName
	 */
	public abstract void createDir (String dirName)
	
	/**
	 * Remove directory from server
	 * @param dirName
	 */
	public abstract void removeDir (String dirName)
	
	/**
	 * Return current directory with full path
	 */
	public String currentAbstractDir() {
		currentPath
	}
	
	/**
	 * Return current directory with relative path
	 * @return
	 */
	public String currentDir() {
		def cur = currentPath
		if (cur == null) throw new ExceptionGETL("Current path not set")

//		if (!isCaseSensitiveName()) cur = cur.toLowerCase()
		cur = cur.replace("\\", "/") 
		if (rootPath == null || rootPath.length() == 0) throw new ExceptionGETL("Root path not set")
		 
		def root = rootPath
//		if (!isCaseSensitiveName()) root = root.toLowerCase()
		root = root.replace("\\", "/")
		
		if (cur == root) return "."
		
		def rp = root
		if (rp[rp.length() - 1] != "/") rp += "/"
		
		if (cur.matches("(?i)${rp}.*")) cur = cur.substring(rp.length())
		
		cur
	}

	
	/**
	 * Rename file from server
	 * @param fileName
	 * @param path
	 */
	public abstract void rename (String fileName, String path)
	
	/**
	 * File list
	 */
	public TableDataset fileList /*= TDS.dataset()*/
	
	/**
	 * Name of table file list
	 */
	private String fileListName
	public String getFileListName () { fileListName }
	public void setFileListName (String value) {
		fileListName = value
		fileList.tableName = value
	}
	
	/**
	 * Count found files 
	 */
	public long countFileList
	
	protected void processList (Map params) {
		Path path = params.path
		String maskFile = params.maskFile
		TypeFile type = params.type
		boolean recursive = params.recursive
		int filelevel = params."filelevel"?:1
		boolean requiredAnalize = params."requiredAnalize"
		
		Closure code = params.code
		Closure updater = params.updater
		def curPath = currentDir()
		def addFile = { Map file ->
			if (!recursive || file.type == TypeFile.FILE) {
				def fn = "${((recursive && curPath != '.')?curPath + '/':'')}${file.filename}"
				def m = path.analizeFile(fn)
				if (m != null && (type == TypeFile.ALL || type == file.type)) {
					def nf = [:]
					nf.filepath = curPath
					nf.putAll(file)
					nf.filetype = file.type.toString()
					nf.localfilename = nf.filename
					nf.filelevel = filelevel
					m.each { var, value ->
						nf."${var.toLowerCase()}" = value
					}
					def b = (code != null)?code(nf):true
					if (b) {
						countFileList++
						updater(nf)
					}
				}
			}
			
			if (recursive && file.type == TypeFile.DIRECTORY) {
				def b = true
				if (!requiredAnalize) {
					b = true
				}
				else {
					b = false
					def fn = "${(curPath != '.' && curPath != "")?curPath + '/':''}${file.filename}"
					def m = path.analizeDir(fn)
					if (m != null) {
						if (code != null) {
							def nf = [:]
							nf.filepath = curPath
							nf.putAll(file)
							nf.filetype = file.type.toString()
							nf.localfilename = nf.filename
							nf.filelevel = filelevel
							m.each { var, value ->
								nf."${var.toLowerCase()}" = value
							}
							b = code(nf)
						}
					}
				}
				
				if (b) {
					changeDirectory(file.filename)
					processList(path: path, maskFile: maskFile, type: type, recursive: recursive, filelevel: filelevel + 1, code: code, requiredAnalize: requiredAnalize, updater: updater)
					changeDirectory("..")
				}
			}
		}
		
		list(maskFile, addFile)
	}
	
	/**
	 * Build list files with path processor<br>
	 * <p><b>Dynamic parameters:</b></p>
	 * <ul>
	 * <li>Path path - path processor
	 * <li>TypeFile type - process type file
	 * <li>String maskFile - mask processed files
	 * <li>TableDataset story - story table on file history
	 * <li>Boolean recursive - find as recursive
	 * </ul>
	 * @param params - parameters
	 * @param code - processing code for file attributes as boolean code (Map file)
	 * @return
	 */
	public void buildList (Map lparams, Closure code) {
		methodParams.validation("buildList", lparams)
		Path path = lparams.path?:(new Path(mask: "*.*"))
		boolean requiredAnalize = !(path.vars.isEmpty())
		String maskFile = lparams.maskFile?:null
		TypeFile type = lparams.type?:TypeFile.FILE
		boolean recursive = (lparams.recursive != null)?lparams.recursive:false
		boolean takePathInStory =  (lparams.takePathInStory != null)?lparams.takePathInStory:true
		if (recursive && (maskFile != null || type != TypeFile.FILE)) throw new ExceptionGETL("Don't compatibility parameters recursive vs maskFile/type")
		
		TableDataset story = lparams."story"
		
		TableDataset newFiles = TDS.dataset()
		newFiles.field << new Field(name: "FILENAME", length: 250, isKey: true)
		if (takePathInStory) newFiles.field << new Field(name: "FILEPATH", length: 500, isKey: true)
		newFiles.create()
		
		Sql tdsCon = ((JDBCConnection)newFiles.connection).sqlConnection
		tdsCon.cacheStatements = true
		
		def sqlExists, sqlInsert
		if (takePathInStory) {
			sqlExists = "SELECT 1 AS ISEXISTS FROM ${newFiles.fullNameDataset()} WHERE FILENAME= ? AND FILEPATH = ?"
			sqlInsert = "INSERT INTO ${newFiles.fullNameDataset()} (FILENAME, FILEPATH) VALUES (?, ?)"
		}
		else {
			sqlExists = "SELECT 1 AS ISEXISTS FROM ${newFiles.fullNameDataset()} WHERE FILENAME= ?"
			sqlInsert = "INSERT INTO ${newFiles.fullNameDataset()} (FILENAME) VALUES (?)"
		}
		newFiles.connection.driver.saveToHistory(sqlExists)
		newFiles.connection.driver.saveToHistory(sqlInsert)
		
		Closure procCode
		if (story != null) {
			if (!story.connection.connected) story.connection.connected = true
			
			Sql con = ((JDBCConnection)story.connection).sqlConnection
			con.cacheStatements = true
			
			def sql
			if (takePathInStory) {
				sql = "SELECT 1 AS ISEXISTS FROM ${story.fullNameDataset()} WHERE FILENAME= ? AND FILEPATH = ?"
			}
			else {
				sql = "SELECT 1 AS ISEXISTS FROM ${story.fullNameDataset()} WHERE FILENAME= ?"
			}
			story.connection.driver.saveToHistory(sql)
			
			procCode = { file ->
				if (code != null) {
					if (!code(file)) return false
				}
				
				String filepath = file."filepath"
				String filename = file."localfilename"?:file."filename"
				List rowsParam = [filename]
				if (takePathInStory) rowsParam << filepath
				
				def prevRow = tdsCon.rows(sqlExists, rowsParam)
				if (!prevRow.isEmpty()) return false
				if (tdsCon.executeUpdate(sqlInsert, rowsParam) != 1) throw new ExceptionGETL("Can not insert file name to buffer table")
				
				def row = con.rows(sql, rowsParam)
				if (!row.isEmpty()) return false
				
				true
			}
		}
		else {
			procCode = { file ->
				if (code != null) {
					if (!code(file)) return false
				}
				
				String filepath = file."filepath"
				String filename = file."localfilename"?:file."filename"
				List rowsParam = [filename]
				if (takePathInStory) rowsParam << filepath

				def prevRow = tdsCon.rows(sqlExists, rowsParam)
				if (!prevRow.isEmpty()) return false
				if (tdsCon.executeUpdate(sqlInsert, rowsParam) != 1) throw new ExceptionGETL("Can not insert file name to buffer table")
								
				true
			}
		}
			
		
		initFileList()
		path.vars.each { key, attr ->
			def ft = attr.type?:Field.Type.STRING
			def length = attr.lenMax?:((ft == Field.Type.STRING)?250:30)
			fileList.field << new Field(name: key.toUpperCase(), type: ft, length: length, precision: attr.precision?:0)
		}
		fileList.create()
		
		countFileList = 0
		new Flow().writeTo(dest: fileList) { Closure updater ->
			processList(path: path, maskFile: maskFile, type: type, recursive: recursive, code: procCode, requiredAnalize: requiredAnalize, updater: updater)
		}
	}
	
	/**
	 * Build list files with path processor<br>
	 * <p><b>Dynamic parameters:</b></p>
	 * <ul>
	 * <li>Path path - path processor
	 * <li>TypeFile type - process type file
	 * <li>String maskFile - mask processed files
	 * </ul>
	 * @param params - parameters
	 * @return
	 */
	public void buildList (Map params) {
		buildList(params, null)
	}
	
	/**
	 * Download files of list<br>
	 * <p><b>Dynamic parameters:</b></p>
	 * <ul>
	 * <li>boolean deleteLoadedFile - delete file after download (default false)
	 * <li>TableDataset story - save download history and check already downloaded files
	 * <li>boolean ignoreError - ignore download errors and continue download next files (default false)
	 * <li>boolean folders - download as original structure folders (default true)
	 * <li>String filter - SQL filter expression on process file list
	 * <li>String order - SQL order by expression on process file list 
	 * </ul>
	 * @param params - parameters
	 * @param onDownloadFile - run code after download file as void onDownloadFile (Map file)  
	 * @return - list of download files
	 */
	public void downloadFiles(Map params, Closure onDownloadFile) {
		methodParams.validation("downloadFiles", params)
		if (fileList.field.isEmpty()) throw new ExceptionGETL("Before download build fileList dataset")
		
		boolean deleteLoadedFile = (params.deleteLoadedFile != null)?params.deleteLoadedFile:false
		TableDataset ds = params.story?:null
		boolean useStory = (ds != null)
		boolean ignoreError = (params.ignoreError != null)?params.ignoreError:false
		boolean folders = (params.folders != null)?params.folders:true
		String sqlWhere = params.filter?:null
		List<String> sqlOrderBy = params.order?:null
		
		TableDataset storyFiles
		
		if (useStory) {
			if (ds == null) throw new ExceptionGETL("For use store db required set \"ds\" property")
			if (ds.field.isEmpty()) {
				if (ds.manualSchema) throw new ExceptionGETL("Empty fields structure for dataset history")
				ds.retrieveFields()
			}
			
			String storyTable = "T_${StringUtils.RandomStr().replace('-', '_').toUpperCase()}"
			storyFiles = new TableDataset(connection: ds.connection, tableName: storyTable, manualSchema: true, type: JDBCDataset.Type.LOCAL_TEMPORARY)
			storyFiles.field = fileList.field
			storyFiles.create()
			
			new Flow().writeTo(dest: storyFiles, dest_batchSize: 1000) { updater ->
				fileList.eachRow { file ->
					def row = [:]
					row.putAll(file)
					updater(row)
				} 
			}
				
			def query = """
DELETE FROM ${storyTable}
WHERE 
	EXISTS(
		SELECT * 
		FROM  ${ds.fullNameDataset()} s
			WHERE s.FILEPATH = ${storyTable}.FILEPATH AND s.FILENAME = ${storyTable}.FILENAME
	)
									"""	
				
			ds.connection.executeCommand(command: query)
			ds.connection.startTran()
			ds.openWrite()
		} 
		
		def ld = (localDirectory != null)?localDirectory + "/":""
		def curDir = currentDir()

		try {
			TableDataset files = (useStory)?storyFiles:fileList
			
			files.eachRow(where: sqlWhere, order: sqlOrderBy) { file ->
				def filepath = file.filepath
				if (curDir != filepath) {
					setCurrentPath("${rootPath}/${filepath}")
					curDir = currentDir()
				}
				
				def lpath = (folders)?"${localDirectory}/${file.filepath}":localDirectory
				FileUtils.ValidPath(lpath)
				
				def tempName = "_" + FileUtils.UniqueFileName()  + ".tmp"
				try {
					download(file.filename, lpath, tempName)
				}
				catch (Exception e) {
					Logs.Severe("Can not download file ${file.filepath}/${file.filename}")
					new File("${lpath}/${tempName}").delete()
					if (!ignoreError) {
						throw e
					}
					Logs.Warning(e)
					return
				}
					
				def temp = new File("${lpath}/${tempName}")
				def localFileName = (file.localfilename != null)?file.localfilename:file.filename
				def dest = new File("${lpath}/${localFileName}")
	
				try {			
					dest.delete()
					temp.renameTo(dest)
				}
				catch (Exception e) {
					new File("${lpath}/${tempName}").delete()
					throw e
				}
				
				if (useStory) {
					Map row = [:]
					row.putAll(file)
					row.filename = localFileName
					row.fileloaded = DateUtils.Now()
				
					ds.write(row)
				}
				
				if (onDownloadFile != null) onDownloadFile(file)
				if (deleteLoadedFile) {
					try {
						removeFile(file.filename)
					}
					catch (Exception e) {
						if (!ignoreError) throw e
						Logs.Warning(e)
					}
				}
			}
			
			if (useStory) ds.doneWrite()
		}
		catch (Exception e) {
			if (useStory) ds.connection.rollbackTran()
			throw e
		}
		finally {
			if (useStory) {
				ds.closeWrite()
				storyFiles.drop(ifExists: true)
			}
		}
		if (useStory) ds.connection.commitTran()
	}
	
	/**
	 * Download files of list<br>
	 * @param onDownloadFile - run code after download file as void onDownloadFile (Map file)  
	 * @return - list of download files
	 */
	public void downloadFiles(Closure onDownloadFile) {
		downloadFiles([:], onDownloadFile)
	}
	
	/**
	 * Download files of list
	 */
	public void downloadFiles() {
		downloadFiles([:], null)
	}
	
	/**
	 * Download files of list<br>
	 * <p><b>Dynamic parameters:</b></p>
	 * <ul>
	 * <li>boolean deleteLoadedFile - delete file after download (default false)
	 * <li>TableDataset story - save download history and check already downloaded files
	 * <li>boolean ignoreError - ignore download errors and continue download next files (default false)
	 * <li>boolean folders - download as original structure folders (default true)
	 * <li>String filter - SQL filter expression on process file list
	 * <li>String order - SQL order by expression on process file list
	 * </ul>
	 * @param params - parameters
	 * @return - list of download files
	 */
	public downloadFiles(Map params) {
		downloadFiles(params, null)
	}

	/**
	 * Adding system fields to dataset for history table operations
	 * @param dataset
	 */
	public static void AddFieldsToDS(Dataset dataset) {
		dataset.field = []
		dataset.field << new Field(name: "FILENAME", length: 250, isNull: false, isKey: true, ordKey: 1)
		dataset.field << new Field(name: "FILEPATH", length: 500, isNull: false, isKey: true, ordKey: 2)
		dataset.field << new Field(name: "FILEDATE", type: "DATETIME", isNull: false)
		dataset.field << new Field(name: "FILESIZE", type: "BIGINT", isNull: false)
		dataset.field << new Field(name: "FILELOADED", type: "DATETIME", isNull: false)
	}
	
	/**
	 * Init file list table structure
	 */
	private void initFileList() {
		fileList.drop(ifExists: true)
		fileList.field = []
		fileList.field << new Field(name: "FILENAME", length: 250, isNull: false, isKey: true, ordKey: 1)
		fileList.field << new Field(name: "FILEPATH", length: 500, isNull: false, isKey: true, ordKey: 2)
		fileList.field << new Field(name: "FILEDATE", type: "DATETIME", isNull: false)
		fileList.field << new Field(name: "FILESIZE", type: "BIGINT", isNull: false)
		fileList.field << new Field(name: "FILETYPE", length: 20, isNull: false)
		fileList.field << new Field(name: "LOCALFILENAME", length: 250, isNull: false)
	}

	/**
	 * Valid and return file from path
	 * @param path
	 * @return
	 */
	public File fileFromLocalDir(String path) {
		def f = new File(path)
		if (!f.exists() || !f.file) throw new ExceptionGETL("File \"${path}\" not found")
		
		f
	}
	
	/**
	 * Processing local path to directory and return absolute path
	 * @param dir
	 * @return
	 */
	protected String processLocalDirPath(String dir) {
		if (dir == null) throw new ExceptionGETL("Required not null directory parameter")
		
		if (dir == '.') return localDirFile.path
		if (dir == '..') return localDirFile.parent
		
		dir = dir.replace('\\', '/')
		def lc = localDirectory?.replace('\\', '/')
		
		File f
		def n
		if (lc != null && dir.matches("(?i)${lc}/.*")) {
			f = new File(dir)
		}
		else {
			f = new File("${localDirFile.path}/${dir}")
		}
		
		f.absolutePath
	}
	
	/**
	 * Create new local directory
	 * @param dir
	 * @param throwError
	 */
	public void createLocalDir (String dir, boolean throwError) {
		def fn = "${currentLocalDir()}/${dir}"
		if (!new File(fn).mkdirs() && throwError) throw new ExceptionGETL("Cannot create local directory \"${fn}\"")
	}

	/**
	 * Create new local directory
	 * @param dir
	 */
	public void createLocalDir (String dir) {
		createLocalDir(dir, true)
	}
	
	/**
	 * Remove local directory
	 * @param dir
	 * @param throwError
	 */
	public void removeLocalDir (String dir, boolean throwError) {
		def fn = "${currentLocalDir()}/${dir}"
		if (!new File(fn).delete() && throwError) throw new ExceptionGETL("Can not remove local directory \"${fn}\"")
	}
	
	/**
	 * Remove local directory
	 * @param dir
	 */
	public void removeLocalDir (String dir) {
		removeLocalDir(dir, true)
	}
	
	/**
	 * Remove local directories
	 * @param dirName
	 * @param throwError
	 */
	public void removeLocalDirs (String dirName, boolean throwError) {
		String[] dirs = dirName.replace("\\", "/").split("/")
		dirs.each { dir -> changeLocalDirectory(dir) }
		for (int i = dirs.length; i--; i >= 0) {
			changeLocalDirectoryUp()
			removeLocalDir(dirs[i])
		}
	}

	/**
	 * Remove local directories
	 * @param dirName
	 */
	public void removeLocalDirs (String dirName) {
		removeLocalDirs(dirName, true)
	}
	
	/**
	 * Remove local file
	 * @param fileName
	 */
	public void removeLocalFile (String fileName) {
		def fn = "${currentLocalDir()}/$fileName"
		if (!new File(fn).delete()) throw new ExceptionGETL("Can not remove Local file \"$fn\"")
	}

	/**
	 * Current local directory path	
	 * @return
	 */
	public String currentLocalDir () {
		localDirFile.absolutePath.replace("\\", "/")
	}
	
	/**
	 * Change local directory
	 * @param dir
	 */
	public void changeLocalDirectory (String dir) {
		if (dir == '.') return
		if (dir == '..') {
			changeLocalDirectoryUp()
		}
		
		setCurrentLocalPath(processLocalDirPath(dir))
	}
	
	/**
	 * Set new current local directory path
	 * @param path
	 * @return
	 */
	protected File setCurrentLocalPath(String path) {
		File f = new File(path)
		if (!f.exists() || !f.directory) throw new ExceptionGETL("Local directory \"${path}\" not found")
		localDirFile = f
	}
	
	/**
	 * Change local directory to up
	 */
	public void changeLocalDirectoryUp () {
		setCurrentLocalPath(localDirFile.parent)
	}

	/**
	 * 	Change local directory to root
	 */
	public void changeLocalDirectoryToRoot () {
		setCurrentLocalPath(localDirectory)
	}
	
	/**
	 * Validate local path
	 * @param dir
	 * @return
	 */
	public boolean existsLocalDirectory(String dir) {
		new File(processLocalDirPath(dir)).exists()
	}
	
	/**
	 * Validate path
	 * @param dir
	 * @return
	 */
	public abstract boolean existsDirectory (String dirName)
	
	/**
	 * Delete empty directory
	 * @param dirName
	 * @param recursive
	 */
	public boolean deleteEmptyFolder(String dirName, boolean recursive) {
		changeDirectory(dirName)
		def existsFiles = false
		try {
			list() { file ->
				if (file."type" == TypeFile.DIRECTORY) {
					if (recursive) {
						existsFiles = deleteEmptyFolder(file."filename", recursive) || existsFiles
					}
					else {
						existsFiles = true
					}
				}
				else {
					existsFiles = true
				}
				
				true
			}
		}
		finally {
			changeDirectoryUp()
		}
		
		if (!existsFiles) {
			removeDir(dirName)
		}
		
		existsFiles
	}
	
	/**
	 * Delete empty directories for current directory
	 * @param recursive
	 */
	public void deleteEmptyFolder (boolean recursive) {
		list() { file -> 
			if (file."type" == TypeFile.DIRECTORY) deleteEmptyFolder(file."filename", recursive)
			true
		}
	}
	
	/**
	 * Delete empty directories for current directory
	 * @param recursive
	 */
	public void deleteEmptyFolder () {
		deleteEmptyFolder(true)
	}
	
	/**
	 * Allow run command on server
	 */
	public boolean isAllowCommand() { false }
	
	/**
	 * Run command on server
	 * @param command - single command for command processor server
	 * @param out - output console log
	 * @param err - error console log
	 * @return - 0 on sucessfull, greater 0 on error, -1 on invalid command
	 */
	public int command(String command, StringBuilder out, StringBuilder err) {
		out.setLength(0)
		err.setLength(0)
		
		if (!allowCommand) throw new ExceptionGETL("Run command is not allowed by \"server\" server")
		
		writeScriptHistoryFile("COMMAND: $command")
		
		def res = doCommand(command, out, err)
		writeScriptHistoryFile("OUT:\n${out.toString()}")
		if (err.length() > 0) writeScriptHistoryFile("ERROR: ${err.toString()}")

		res
	}
	
	/**
	 * Internal driver runner command
	 * @param command
	 * @param out
	 * @param err
	 * @return
	 */
	protected int doCommand(String command, StringBuilder out, StringBuilder err) { }
	
	/**
	 * Real script history file name
	 */
	private String fileNameScriptHistory
	
	/**
	 * Validation script history file
	 */
	@Synchronized
	protected void validScriptHistoryFile () {
		if (fileNameScriptHistory == null) {
			fileNameScriptHistory = StringUtils.EvalMacroString(scriptHistoryFile, StringUtils.MACROS_FILE)
			FileUtils.ValidFilePath(fileNameScriptHistory)
		}
	}
	
	/**
	 * Write to script history file 
	 * @param text
	 */
	@Synchronized
	protected void writeScriptHistoryFile (String text) {
		if (scriptHistoryFile == null) return
		validScriptHistoryFile()
		def f = new File(fileNameScriptHistory).newWriter("utf-8", true)
		try {
			f.write("${DateUtils.NowDateTime()}\t$text\n")
		}
		finally {
			f.close()
		}
	}
}