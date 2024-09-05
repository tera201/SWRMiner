package org.repodriller.util

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.repodriller.scm.entities.CommitSize
import org.repodriller.scm.entities.DeveloperInfo
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement


class DataBaseUtil(val url:String) {
    var conn: Connection
    init {
        try {
            Class.forName("org.h2.Driver")
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        conn = DriverManager.getConnection("jdbc:h2:" + url)
    }

    private fun getIdExecute(pstmt: PreparedStatement):Int?{
        pstmt.executeQuery().use { rs ->
            if (rs.next()) return rs.getInt("id")
            else return null
        }
    }

    private fun isExistExecute(pstmt: PreparedStatement): Boolean{
        pstmt.executeQuery().use { rs -> if (rs.next())  return true }
        return false
    }

    fun create() {
        createTables(conn)
    }

    fun getUrlPath():String {
        return url
    }

    fun closeConnection() {
        conn.close()
    }

    fun insertProject(name: String, filePath: String):Int {
        val sql = "MERGE INTO Projects(name, filePath) KEY (name, filePath) VALUES(?, ?)"
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            pstmt.setString(1, name)
            pstmt.setString(2, filePath)
            pstmt.executeUpdate()
            pstmt.generatedKeys.use { generatedKeys ->
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)
                }
            }
        }
        return -1
    }

    fun getProjectId(projectName: String, filePath: String): Int? {
        val sql = "SELECT id FROM Projects WHERE name = ? AND filePath = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, projectName)
            pstmt.setString(2, filePath)
            return getIdExecute(pstmt)
        }
    }

    fun insertAuthor(projectId:Int, name: String, email: String):Int {
        val sql = "MERGE INTO Authors(projectId, name, email) KEY (projectId, email) VALUES(?, ?, ?)"
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, name)
            pstmt.setString(3, email)
            pstmt.executeUpdate()
            pstmt.generatedKeys.use { generatedKeys ->
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)
                }
            }
        }
        return -1
    }

    fun getAuthorId(projectId: Int, email: String): Int? {
        val sql = "SELECT id FROM Authors WHERE projectId = ? AND email = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, email)
            return getIdExecute(pstmt)
        }
    }

    fun insertCommit(projectId:Int, authorId: Int, hash: String, date: Int, projectSize: Long, stability: Double, fileEntity: FileEntity):String {
        val sql = "MERGE INTO Commits(projectId, authorId, hash, date, projectSize, stability, filesAdded, filesDeleted, filesModified, linesAdded, linesDeleted, linesModified, changes) KEY (hash, projectId) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setInt(2, authorId)
            pstmt.setString(3, hash)
            pstmt.setInt(4, date)
            pstmt.setLong(5, projectSize)
            pstmt.setDouble(6, stability)
            pstmt.setInt(7, fileEntity.fileAdded)
            pstmt.setInt(8, fileEntity.fileDeleted)
            pstmt.setInt(9, fileEntity.fileModified)
            pstmt.setInt(10, fileEntity.linesAdded)
            pstmt.setInt(11, fileEntity.linesDeleted)
            pstmt.setInt(12, fileEntity.linesModified)
            pstmt.setInt(13, fileEntity.changes)
            pstmt.executeUpdate()
            pstmt.generatedKeys.use { generatedKeys ->
                if (generatedKeys.next()) {
                    return generatedKeys.getString(1)
                }
            }
        }
        return ""
    }

    fun getDeveloperInfo(projectId: Int, filePath: String) : Map<String, CommitSize>  {
        val commitSizeMap = mutableMapOf<String, CommitSize>()
        val sql = """
        SELECT c.*, a.email as authorEmail, a.name as authorName
        FROM Commits c
        JOIN Files f ON f.hash = c.hash AND f.projectId = c.projectId
        JOIN Authors a ON a.id = c.authorId AND a.projectId = c.projectId
        WHERE c.projectId = ?
          AND f.filePath LIKE ?
    """
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setIntOrNull(1, projectId)
            pstmt.setString(2, filePath + "%")
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                val hash = rs.getString("hash")
                val authorName = rs.getString("authorName")
                val authorEmail = rs.getString("authorEmail")
                val date = rs.getInt("date")
                val size = rs.getLong("projectSize")
                val stability = rs.getDouble("stability")
                commitSizeMap.put(hash, CommitSize(hash, authorName, authorEmail, size, date, stability))
            }
        }
        return commitSizeMap
    }

    fun getCommitSizeMap(projectId: Int, filePath: String) : Map<String, CommitSize>  {
        val commitSizeMap = mutableMapOf<String, CommitSize>()
        val sql = """
        SELECT c.*, a.email as authorEmail, a.name as authorName
        FROM Commits c
        JOIN Files f ON f.hash = c.hash AND f.projectId = c.projectId
        JOIN Authors a ON a.id = c.authorId AND a.projectId = c.projectId
        WHERE c.projectId = ?
          AND f.filePath LIKE ?
    """
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setIntOrNull(1, projectId)
            pstmt.setString(2, filePath + "%")
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                val hash = rs.getString("hash")
                val authorName = rs.getString("authorName")
                val authorEmail = rs.getString("authorEmail")
                val date = rs.getInt("date")
                val size = rs.getLong("projectSize")
                val stability = rs.getDouble("stability")
                commitSizeMap.put(hash, CommitSize(hash, authorName, authorEmail, size, date, stability))
            }
        }
        return commitSizeMap
    }

    fun isCommitExist(hash: String): Boolean {
        val sql = "SELECT * FROM Commits WHERE hash = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, hash)
            return isExistExecute(pstmt)
        }
    }

    fun insertBlameFile(projectId: Int, filePath: String, fileHash: String):Int {
        val sql = "MERGE INTO BlameFiles(projectId, filePath, fileHash) KEY (projectId, filePath, fileHash) VALUES(?, ?, ?)"
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, filePath)
            pstmt.setString(3, fileHash)
            pstmt.executeUpdate()
            pstmt.generatedKeys.use { generatedKeys ->
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)
                }
            }
        }
        return -1
    }

    fun insertFile(fileList: List<org.repodriller.scm.entities.FileEntity>) {
        val sql = "MERGE INTO Files(projectId, filePath, hash, date) KEY (projectId, filePath, hash) VALUES(?, ?, ?, ?)"
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            for (file in fileList) {
                pstmt.setInt(1, file.projectId)
                pstmt.setString(2, file.filePath)
                pstmt.setString(3, file.hash)
                pstmt.setInt(4, file.date)
                pstmt.addBatch()
            }
            pstmt.executeBatch()
        }
    }

    fun insertFile(file: org.repodriller.scm.entities.FileEntity):Int {
        val sql = "MERGE INTO Files(projectId, filePath, hash, date) KEY (projectId, filePath, hash) VALUES(?, ?, ?, ?)"
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            pstmt.setInt(1, file.projectId)
            pstmt.setString(2, file.filePath)
            pstmt.setString(3, file.hash)
            pstmt.setInt(4, file.date)
            pstmt.executeUpdate()
            pstmt.generatedKeys.use { generatedKeys ->
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)
                }
            }
        }
        return -1
    }

    fun updateBlameFileSize(blameFileId: Int) {
        val sqlUpdate = """
        UPDATE BlameFiles SET lineSize = (SELECT SUM(lineSize) FROM Blames WHERE blameFileId = ? GROUP BY projectId AND blameFileId) WHERE id = ?
    """.trimIndent()

        conn.prepareStatement(sqlUpdate).use { pstmt ->
            pstmt.setInt(1, blameFileId)
            pstmt.setInt(2, blameFileId)
            pstmt.executeUpdate()
        }
    }

    fun getBlameFileId(projectId: Int, filePath: String, fileHash: String):Int? {
        val sql = "SELECT * FROM BlameFiles WHERE projectId = ? AND filePath = ? AND fileHash = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, filePath)
            pstmt.setString(3, fileHash)
            return getIdExecute(pstmt)
        }
    }

    fun getLastFileHash(projectId: Int, filePath: String):String? {
        val sql = "SELECT hash FROM Files WHERE projectId = ? AND filePath = ? ORDER BY date DESC LIMIT 1"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, filePath)
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                val hash = rs.getString("hash")
                return hash
            }
        }
        return null
    }

    fun getFirstHashForFile(projectId: Int, filePath: String):String? {
        val sql = """
            SELECT MIN(hash) as hash
            FROM Files
            WHERE projectId = ? AND filePath = ?
        """.trimIndent()
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, filePath)
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                val hash = rs.getString("hash")
                return hash
            }
        }
        return null
    }

    fun getFirstAndLastHashForFile(projectId: Int, filePath: String):Pair<String, String>? {
        val sql = """
            SELECT MIN(hash) AS firstHash, MAX(hash) AS lastHash
            FROM Files
            WHERE projectId = ? AND filePath = ?
        """.trimIndent()
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, filePath)
            val rs = pstmt.executeQuery()
            return if (rs.next()) {
                Pair(rs.getString("firstHash"), rs.getString("lastHash"))
            } else {
                null
            }
        }
    }

    fun insertBlame(blameEntity: BlameEntity) {
        val sql = "MERGE INTO Blames(projectId, authorId, blameFileId, blameHashes, lineIds, lineCounts, lineSize) KEY (projectId, authorId, blameFileId) VALUES(?, ?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            pstmt.setInt(1, blameEntity.projectId)
            pstmt.setInt(2, blameEntity.authorId)
            pstmt.setInt(3, blameEntity.blameFileId)
            pstmt.setString(4, convertListToJson(blameEntity.blameHashes))
            pstmt.setString(5, convertListToJson(blameEntity.lineIds))
            pstmt.setLong(6, blameEntity.lineIds.size.toLong())
            pstmt.setLong(7, blameEntity.lineSize)
            pstmt.executeUpdate()
        }
    }

    fun insertBlame(blameEntities: List<BlameEntity>) {
        val sql = "MERGE INTO Blames(projectId, authorId, blameFileId, blameHashes, lineIds, lineCounts, lineSize) KEY (projectId, authorId, blameFileId) VALUES(?, ?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { pstmt ->
            for (blame in blameEntities) {
                pstmt.setInt(1, blame.projectId)
                pstmt.setInt(2, blame.authorId)
                pstmt.setInt(3, blame.blameFileId)
                pstmt.setString(4, convertListToJson(blame.blameHashes))
                pstmt.setString(5, convertListToJson(blame.lineIds))
                pstmt.setLong(6, blame.lineIds.size.toLong())
                pstmt.setLong(7, blame.lineSize)
                pstmt.addBatch()
            }
            pstmt.executeBatch()  // Выполнение пакетной вставки
        }
    }

    fun getDevelopersByProjectId(projectId: Int): Map<String, Int> {
        val developers = mutableMapOf<String, Int>()
        val sql = "SELECT id, email FROM Authors WHERE projectId = ?"

        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                val id = rs.getInt("id")
                val email = rs.getString("email")
                developers.put(email, id)
            }
        }
        return developers
    }

    fun developerUpdateByBlameInfo(projectId: Int, developers: Map<String, DeveloperInfo>): Pair<Int, Long> {
        val sql = """
        SELECT COUNT(b.lineCounts) AS lineCount, SUM(b.lineSize) AS lineSize, authors.email, string_agg(bf.filePath, ', ') as filePaths
        FROM Blames b
        JOIN BlameFiles bf ON b.blameFileId = bf.id
        JOIN Authors authors on authors.id = b.authorId
        WHERE b.projectId = ?
        GROUP BY b.projectId, b.authorId
    """

        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                val email = rs.getString("email")
                val count = rs.getLong("lineCount")
                val size = rs.getLong("lineSize")
                val filePaths = rs.getString("filePaths").split(", ")
                developers.get(email)?.actualLinesSize = size
                developers.get(email)?.actualLinesOwner = count
                developers.get(email)?.ownerForFiles?.addAll(filePaths)
            }
        }
        return Pair(0, 0)
    }

//    fun getBlameInfoByFilePattern(projectId: Int, filePathPattern: Int): Pair<Int, Long> {
//        val sql = """
//        SELECT A.email AS email, A.name AS name, Sum(b.lineSize) AS lineSize, Sum(b.lineCounts) AS lineCounts, Sum(b.lineSize) / SUM(bf.lineSize) AS Owner/percent
//        FROM Blames b
//        JOIN BlameFiles bf ON b.blameFileId = bf.id
//        JOIN Authors A on A.id = b.authorId
//        WHERE b.projectId = ?
//          AND bf.filePath LIKE ?
//        GROUP BY b.projectId, b.authorId
//    """
//
//        conn.prepareStatement(sql).use { pstmt ->
//            pstmt.setInt(1, projectId)
//            pstmt.setInt(2, filePathPattern)
//            val rs = pstmt.executeQuery()
//            while (rs.next()) {
//                val count = rs.getInt("count")
//                val size = rs.getLong("size")
//                return Pair(count, size)
//            }
//        }
//        return Pair(0, 0)
//    }

    fun convertListToJson(list: List<Any>): String {
        val mapper = ObjectMapper()
        var jsonString: String = "{}"
        try {
            jsonString = mapper.writeValueAsString(list)
        } catch (e: JsonProcessingException) {
            e.printStackTrace()
        }
        return jsonString
    }

}

fun main() {
    var projectPath = "."
    val projectDir = File(projectPath).canonicalFile
    var dbUrl = "$projectDir/clonnedGit/db/model.db"
    DataBaseUtil(dbUrl)
//    dropTables("jdbc:sqlite:" + dbUrl)
}