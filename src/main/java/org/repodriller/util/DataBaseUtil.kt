package org.repodriller.util

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException


class DataBaseUtil(url:String) {
    var conn: Connection
    init {
        try {
            createTables("jdbc:sqlite:" + url)
        } catch (e: SQLException) {
            println(e.message)
        }
        conn = DriverManager.getConnection("jdbc:sqlite:" + url)
    }

    private fun getLastInsertId():Int {
        val sqlLastId = "SELECT last_insert_rowid()"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sqlLastId).use { rs ->
                if (rs.next()) {
                    return rs.getInt(1)
                }
            }
        }
        return -1
    }

    private fun getLastInsertStringId():String {
        val sqlLastId = "SELECT last_insert_rowid()"
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sqlLastId).use { rs ->
                if (rs.next()) {
                    return rs.getString(1)
                }
            }
        }
        return ""
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

    fun insertProject(name: String, filePath: String):Int {
        val sql = "INSERT OR IGNORE INTO Projects(name, filePath) VALUES(?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, name)
            pstmt.setString(2, filePath)
            if (pstmt.executeUpdate() > 0) return getLastInsertId()
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
        val sql = "INSERT OR IGNORE INTO Authors(projectId, name, email) VALUES(?, ?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, name)
            pstmt.setString(3, email)
            if (pstmt.executeUpdate() > 0) return getLastInsertId()
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

    fun insertCommit(projectId:Int, authorId: Int, hash: String, date: Int):String {
        val sql = "INSERT OR IGNORE INTO Commits(projectId, authorId, hash, date) VALUES(?, ?, ?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setInt(2, authorId)
            pstmt.setString(3, hash)
            pstmt.setInt(4, date)
            if (pstmt.executeUpdate() > 0) return getLastInsertStringId()
        }
        return ""
    }

    fun isCommitExist(hash: String): Boolean {
        val sql = "SELECT * FROM Commits WHERE hash = ?"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setString(1, hash)
            return isExistExecute(pstmt)
        }
    }

    fun insertBlameFile(projectId: Int, filePath: String, fileHash: String):Int {
        val sql = "INSERT OR IGNORE INTO BlameFiles(projectId, filePath, fileHash) VALUES(?, ?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setString(2, filePath)
            pstmt.setString(3, fileHash)
            if (pstmt.executeUpdate() > 0) return getLastInsertId()
        }
        return -1
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

    fun insertBlame(blameEntities: List<BlameEntity>) {
        val sql = "INSERT OR IGNORE INTO Blames(projectId, authorId, blameFileId, blameHash, lineId, lineSize) VALUES(?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            conn.setAutoCommit(false)
            for (blame in blameEntities) {
                pstmt.setInt(1, blame.projectId)
                pstmt.setInt(2, blame.authorId)
                pstmt.setInt(3, blame.blameFileId)
                pstmt.setString(4, blame.blameHash)
                pstmt.setInt(5, blame.lineId)
                pstmt.setLong(6, blame.lineSize)
                pstmt.addBatch()
            }
            pstmt.executeBatch()  // Выполнение пакетной вставки
            conn.commit()
        }
    }

    fun insertBlame(projectId: Int, authorId: Int, blameFileId: Int, blameHash: String, lineId: Int, lineSize: Long):Int {
        val sql = "INSERT OR IGNORE INTO Blames(projectId, authorId, blameFileId, blameHash, lineId, lineSize) VALUES(?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setInt(2, authorId)
            pstmt.setInt(3, blameFileId)
            pstmt.setString(4, blameHash)
            pstmt.setInt(5, lineId)
            pstmt.setLong(6, lineSize)
            if (pstmt.executeUpdate() > 0) return getLastInsertId()
        }
        return -1
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

    fun getBlameInfoByFilePattern(projectId: Int, authorId: Int, filePathPattern: Int): Pair<Int, Long> {
        val sql = """
        SELECT COUNT(*) AS count, SUM(b.lineSize) AS size 
        FROM Blames b
        JOIN BlameFiles bf ON b.blameFileId = bf.id
        WHERE b.projectId = ?
          AND b.authorId = ?
          AND bf.filePath LIKE ?
        GROUP BY b.projectId, b.authorId, b.blameFileId
    """

        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setInt(2, authorId)
            pstmt.setInt(3, filePathPattern)
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                val count = rs.getInt("count")
                val size = rs.getLong("size")
                return Pair(count, size)
            }
        }
        return Pair(0, 0)
    }

    fun getBlameInfoForProject(projectId: Int, authorId: Int, filePathPattern: Int): Pair<Int, Long> {
        val sql = """
        SELECT COUNT(*) AS count, SUM(b.lineSize) AS size 
        FROM Blames b
        GROUP BY b.projectId, b.authorId
    """

        conn.prepareStatement(sql).use { pstmt ->
            pstmt.setInt(1, projectId)
            pstmt.setInt(2, authorId)
            pstmt.setInt(3, filePathPattern)
            val rs = pstmt.executeQuery()
            while (rs.next()) {
                val count = rs.getInt("count")
                val size = rs.getLong("size")
                return Pair(count, size)
            }
        }
        return Pair(0, 0)
    }

}

fun main() {
    var projectPath = "."
    val projectDir = File(projectPath).canonicalFile
    var dbUrl = "$projectDir/clonnedGit/db/model.db"
    DataBaseUtil(dbUrl)
//    dropTables("jdbc:sqlite:" + dbUrl)
}