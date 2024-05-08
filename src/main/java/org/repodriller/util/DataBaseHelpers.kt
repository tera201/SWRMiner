package org.repodriller.util

import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

fun createTables(url: String) {
    // SQL statement for creating tables
    val sqlCreateCommits = """
        CREATE TABLE IF NOT EXISTS Commits (
            hash INTEGER PRIMARY KEY AUTOINCREMENT,
            date INTEGER NOT NULL,
            projectSize INTEGER NOT NULL,
            projectId INTEGER NOT NULL,
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            FOREIGN KEY (projectId) REFERENCES Project(id),
            UNIQUE (hash, projectId)
        );
    """.trimIndent()

    val sqlCreateProjects = """
        CREATE TABLE IF NOT EXISTS Projects (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL
        );
    """.trimIndent()

    val sqlCreateAuthors = """
        CREATE TABLE IF NOT EXISTS Authors (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT NOT NULL,
            projectId INTEGER NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Project(id),
            UNIQUE (email, projectId) 
        );
    """.trimIndent()

    val sqlCreateAuthors = """
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
            UNIQUE (email, projectId) 
        );
    """.trimIndent()

    try {
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(sqlCreateProjects)
                stmt.execute(sqlCreateAuthors)
                stmt.execute(sqlCreateCommits)
                println("Tables have been created.")
            }
        }
    } catch (e: SQLException) {
        println(e.message)
    }
}

fun dropTables(url: String) {
    val sqlDropModels = "DROP TABLE IF EXISTS Models"
    val sqlDropPackages = "DROP TABLE IF EXISTS Packages"
    val sqlDropClasses = "DROP TABLE IF EXISTS Classes"

    try {
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(sqlDropModels)
                stmt.execute(sqlDropPackages)
                stmt.execute(sqlDropClasses)
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