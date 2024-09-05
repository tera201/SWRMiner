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
    val sqlCreateProjects = """
        CREATE TABLE IF NOT EXISTS Projects (
            id INT PRIMARY KEY AUTO_INCREMENT,
            name VARCHAR(255) NOT NULL,
            filePath TEXT NOT NULL,
            UNIQUE (name, filePath) 
        );
    """.trimIndent()

    val sqlCreateAuthors = """
        CREATE TABLE IF NOT EXISTS Authors (
            id INT PRIMARY KEY AUTO_INCREMENT,
            name VARCHAR(255) NOT NULL,
            email VARCHAR(255) NOT NULL,
            projectId INT NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            UNIQUE (email, projectId) 
        );
    """.trimIndent()

    val sqlCreateCommits = """
        CREATE TABLE IF NOT EXISTS Commits (
            hash TEXT PRIMARY KEY,
            date INT NOT NULL,
            projectSize BIGINT,
            projectId INT NOT NULL,
            authorId INT NOT NULL,
            stability DOUBLE NOT NULL,
            filesAdded INT NOT NULL,
            filesDeleted INT NOT NULL,
            filesModified INT NOT NULL,
            linesAdded INT NOT NULL,
            linesDeleted INT NOT NULL,
            linesModified INT NOT NULL,
            changes INT NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            UNIQUE (hash, projectId)
        );
    """.trimIndent()

    val sqlCreateFiles = """
        CREATE TABLE IF NOT EXISTS Files (
            id INT PRIMARY KEY AUTO_INCREMENT,
            projectId INT NOT NULL,
            filePath TEXT NOT NULL,
            hash TEXT,
            date INT NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (hash) REFERENCES Commits(hash),
            UNIQUE (hash, projectId, filePath)
        );
    """.trimIndent()

    val sqlCreateChanges = """
        CREATE TABLE IF NOT EXISTS Changes (
            id INT PRIMARY KEY AUTO_INCREMENT,
            hash TEXT NOT NULL,
            authorId INT NOT NULL,
            projectId INT NOT NULL,
            changesCount INT NOT NULL,
            changesSize INT NOT NULL,
            linesAdded INT NOT NULL,
            linesModified INT NOT NULL,
            fileAdded INT NOT NULL,
            fileDeleted INT NOT NULL,
            fileModified INT NOT NULL,
            FOREIGN KEY (hash) REFERENCES Commits(hash),
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            UNIQUE (projectId, authorId, hash) 
        );
    """.trimIndent()

    val sqlCreateBlameFiles = """
        CREATE TABLE IF NOT EXISTS BlameFiles (
            id INT PRIMARY KEY AUTO_INCREMENT,
            projectId INT NOT NULL,
            filePath TEXT NOT NULL,
            fileHash TEXT NOT NULL,
            lineSize BIGINT,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (fileHash) REFERENCES Commits(hash),
            UNIQUE (projectId, filePath, fileHash) 
        );
    """.trimIndent()

    val sqlCreateBlame = """
        CREATE TABLE IF NOT EXISTS Blames (
            id INT PRIMARY KEY AUTO_INCREMENT,
            projectId INT NOT NULL,
            authorId INT NOT NULL,
            blameFileId INT NOT NULL,
            blameHashes TEXT NOT NULL,
            lineIds TEXT NOT NULL,
            lineCounts BIGINT NOT NULL,
            lineSize BIGINT NOT NULL,
            FOREIGN KEY (projectId) REFERENCES Projects(id),
            FOREIGN KEY (authorId) REFERENCES Authors(id),
            UNIQUE (projectId, authorId, blameFileId) 
        );
    """.trimIndent()

    if (!allTablesExist(conn, listOf("Projects", "Authors", "Commits", "Files", "Changes", "BlameFiles", "Blames"))) {
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
    }
}

fun dropTables(conn: Connection) {
    val sqlDropProjects = "DROP TABLE IF EXISTS Projects"
    val sqlDropAuthors = "DROP TABLE IF EXISTS Authors"
    val sqlDropCommits = "DROP TABLE IF EXISTS Commits"
    val sqlDropFiles = "DROP TABLE IF EXISTS Files"
    val sqlDropChanges = "DROP TABLE IF EXISTS Changes"
    val sqlDropBlameFiles = "DROP TABLE IF EXISTS BlameFiles"
    val sqlDropBlames = "DROP TABLE IF EXISTS Blames"

    conn.createStatement().use { stmt ->
        stmt.execute(sqlDropProjects)
        stmt.execute(sqlDropAuthors)
        stmt.execute(sqlDropCommits)
        stmt.execute(sqlDropFiles)
        stmt.execute(sqlDropChanges)
        stmt.execute(sqlDropBlameFiles)
        stmt.execute(sqlDropBlames)
        println("Tables have been dropped.")
    }
}

fun PreparedStatement.setIntOrNull(index: Int, value: Int?) {
    if (value != null) {
        this.setInt(index, value)
    } else {
        this.setNull(index, java.sql.Types.INTEGER)
    }
}
