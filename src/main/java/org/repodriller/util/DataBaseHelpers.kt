package org.repodriller.util

import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

fun createTables(url: String) {
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
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            UNIQUE (hash, projectId)
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
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (fileHash) REFERENCES Commits(hash),
            UNIQUE (projectId, filePath, fileHash) 
        );
    """.trimIndent()

    val sqlCreateBlame = """
        CREATE TABLE IF NOT EXISTS Blames (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            projectId TEXT NOT NULL,
            authorId TEXT NOT NULL,
            blameFileId TEXT NOT NULL,
            blameHash TEXT NOT NULL,
            lineId INTEGER NOT NULL,
            lineSize LONG NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (blameHash) REFERENCES Commits(hash),
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            UNIQUE (projectId, authorId, BlameFileId, lineId) 
        );
    """.trimIndent()

    try {
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(sqlCreateProjects)
                stmt.execute(sqlCreateAuthors)
                stmt.execute(sqlCreateCommits)
                stmt.execute(sqlCreateChanges)
                stmt.execute(sqlCreateBlameFiles)
                stmt.execute(sqlCreateBlame)
                println("Tables have been created.")
            }
        }
    } catch (e: SQLException) {
        println(e.message)
    }
}

fun dropTables(url: String) {
    val sqlDropProjects = "DROP TABLE IF EXISTS Projects"
    val sqlDropModels = "DROP TABLE IF EXISTS Models"
    val sqlDropAuthors = "DROP TABLE IF EXISTS Authors"
    val sqlDropCommits = "DROP TABLE IF EXISTS Commits"
    val sqlDropChanges = "DROP TABLE IF EXISTS Changes"
    val sqlDropBlames = "DROP TABLE IF EXISTS Blames"

    try {
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(sqlDropProjects)
                stmt.execute(sqlDropModels)
                stmt.execute(sqlDropAuthors)
                stmt.execute(sqlDropCommits)
                stmt.execute(sqlDropChanges)
                stmt.execute(sqlDropBlames)
                println("Tables have been created.")
            }
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