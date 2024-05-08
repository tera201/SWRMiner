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

}

fun main() {
    var projectPath = "."
    val projectDir = File(projectPath).canonicalFile
    var dbUrl = "$projectDir/JavaToUMLSamples/db/model.db"
    dropTables("jdbc:sqlite:" + dbUrl)
}