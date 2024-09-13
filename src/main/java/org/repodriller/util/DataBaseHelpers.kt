package org.repodriller.util

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

private fun allTablesExist(conn: Connection, tableNames: List<String>): Boolean {
    val meta = conn.metaData
    val schema = conn.schema // Используйте схему по умолчанию, если не указана другая

    for (tableName in tableNames) {
        val rs = meta.getTables(null, schema, tableName, null)
        var tableExists = false

        while (rs.next()) {
            val table = rs.getString("TABLE_NAME")
            println(table)
            if (table.equals(tableName, ignoreCase = true)) {
                tableExists = true
                break
            }
        }

        if (!tableExists) {
            System.err.println("Table $tableName does not exist")
            return false
        }
    }
    return true
}

fun createTables(conn: Connection) {
    // SQL statement for creating tables

    val sqlCreateProjects = """
        CREATE TABLE IF NOT EXISTS Projects (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            filePath TEXT NOT NULL,
            UNIQUE (name, filePath) 
        );
    """.trimIndent()

    val sqlCreateAuthors = """
        CREATE TABLE IF NOT EXISTS Authors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT NOT NULL,
            projectId INTEGER NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            UNIQUE (email, projectId) 
        );
    """.trimIndent()

    val sqlCreateCommits = """
        CREATE TABLE IF NOT EXISTS Commits (
            hash TEXT PRIMARY KEY,
            date INTEGER NOT NULL,
            projectSize LONG,
            projectId INTEGER NOT NULL,
            authorId INTEGER NOT NULL,
            stability DOUBLE NOT NULL,
            filesAdded INTEGER NOT NULL,
            filesDeleted INTEGER NOT NULL,
            filesModified INTEGER NOT NULL,
            linesAdded INTEGER NOT NULL,
            linesDeleted INTEGER NOT NULL,
            linesModified INTEGER NOT NULL,
            changes INTEGER NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            UNIQUE (hash, projectId)
        );
    """.trimIndent()

    val sqlCreateFiles = """
        CREATE TABLE IF NOT EXISTS Files (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            projectId INTEGER NOT NULL,
            filePath TEXT NOT NULL,
            hash TEXT,
            date INTEGER NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (hash) REFERENCES Commits(hash),
            UNIQUE (hash, projectId, filePath)
        );
    """.trimIndent()

    val sqlCreateChanges = """
        CREATE TABLE IF NOT EXISTS Changes (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            hash TEXT NOT NULL,
            authorId TEXT NOT NULL,
            projectId INTEGER NOT NULL,
            changesCount INTEGER NOT NULL,
            changesSize INTEGER NOT NULL,
            linesAdded INTEGER NOT NULL,
            linesModified INTEGER NOT NULL,
            fileAdded INTEGER NOT NULL,
            fileDeleted INTEGER NOT NULL,
            fileModified INTEGER NOT NULL,
            FOREIGN KEY (hash) REFERENCES Commits(hash),
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            UNIQUE (projectId, authorId, hash) 
        );
    """.trimIndent()

    val sqlCreateBlameFiles = """
        CREATE TABLE IF NOT EXISTS BlameFiles (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            projectId TEXT NOT NULL,
            filePath TEXT NOT NULL,
            fileHash TEXT NOT NULL,
            lineSize LONG,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (fileHash) REFERENCES Commits(hash),
            UNIQUE (projectId, filePath, fileHash) 
        );
    """.trimIndent()

    val sqlCreateBlame = """
        CREATE TABLE IF NOT EXISTS Blames (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            projectId INT NOT NULL,
            authorId INT NOT NULL,
            blameFileId INT NOT NULL,
            blameHashes TEXT NOT NULL,
            lineIds TEXT NOT NULL,
            lineCounts LONG NOT NULL,
            lineSize LONG NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (blameHashes) REFERENCES Commits(hash),
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            UNIQUE (projectId, authorId, BlameFileId) 
        );
    """.trimIndent()

//    if (!allTablesExist(conn, listOf("Projects", "Authors", "Commits", "Files", "Changes", "BlameFiles", "Blames"))) {
//        conn.createStatement().use { stmt ->
//            stmt.execute(sqlCreateProjects)
//            stmt.execute(sqlCreateAuthors)
//            stmt.execute(sqlCreateCommits)
//            stmt.execute(sqlCreateFiles)
//            stmt.execute(sqlCreateChanges)
//            stmt.execute(sqlCreateBlameFiles)
//            stmt.execute(sqlCreateBlame)
//            println("Tables have been created.")
//        }
//    }

    try {
        conn.createStatement().use { stmt ->
            stmt.execute(sqlCreateProjects)
            stmt.execute(sqlCreateAuthors)
            stmt.execute(sqlCreateCommits)
            stmt.execute(sqlCreateFiles)
            stmt.execute(sqlCreateChanges)
            stmt.execute(sqlCreateBlameFiles)
            stmt.execute(sqlCreateBlame)
            println("Tables have been created.")
        }
    } catch (e: SQLException) {
        println(e.message)
    }
}

fun dropTables(conn: Connection) {
    val sqlDropProjects = "DROP TABLE IF EXISTS Projects"
    val sqlDropModels = "DROP TABLE IF EXISTS Models"
    val sqlDropAuthors = "DROP TABLE IF EXISTS Authors"
    val sqlDropCommits = "DROP TABLE IF EXISTS Commits"
    val sqlDropFiles = "DROP TABLE IF EXISTS Files"
    val sqlDropChanges = "DROP TABLE IF EXISTS Changes"
    val sqlDropBlameFiles = "DROP TABLE IF EXISTS BlameFiles"
    val sqlDropBlames = "DROP TABLE IF EXISTS Blames"

    try {
        conn.createStatement().use { stmt ->
            stmt.execute(sqlDropProjects)
            stmt.execute(sqlDropModels)
            stmt.execute(sqlDropAuthors)
            stmt.execute(sqlDropCommits)
            stmt.execute(sqlDropChanges)
            stmt.execute(sqlDropBlames)
            stmt.execute(sqlDropFiles)
            stmt.execute(sqlDropBlameFiles)
            println("Tables have been created.")
        }
    } catch (e: SQLException) {
        println(e.message)
    }
}

fun PreparedStatement.setIntOrNull(index: Int, value: Int?) {
    if (value != null) {
        this.setInt(index, value)
    } else {
        this.setNull(index, java.sql.Types.INTEGER)
    }
}
