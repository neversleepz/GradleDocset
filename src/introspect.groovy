/**
 * Creates the db for a <a href="http://kapeli.org/dash">Dash</a> docset for the <a href="http://gradle.org">Gradle</a>
 * 2.0 manual.
 * Uses Java 8 and Groovy 2.3
 * @author Kon Soulianidis
 *
 * This project has the mysql db precreated.
 * To update, copy the Gradle manual (html, css, images, etc) to a folder underneath
 * Gradle.docset/Contents/Resources/Documents, replacing whats already there.
 * Open the sqllite db and truncate the searchIndex table.
 *
 * Update the @{link #gradleManualPath} variable.
 */


import groovy.sql.Sql

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function
import java.util.stream.Collectors

def pattern = ~/<\/?title>/

def userHome = "/Volumes/Data/UsersX/kon"
def docSetHome = "Library/Application Support/Dash/DocSets"

def gradleManualPath = Paths.get("$userHome/$docSetHome/Gradle.docset/Contents/Resources/Documents/")
def mapFileToChapterTitle =
   Files.list(gradleManualPath)
        .parallel()
        .filter { Path p -> p.getFileName().toString().endsWith(".html") }
        .collect(Collectors.groupingBy(
            Function.identity(),
            { new LinkedHashMap() },
            Collectors.mapping(
                 { Path p -> Files.lines(p).limit(1)
                                .flatMap(
                                    { String str ->
                                        pattern.splitAsStream(str).skip(1).limit(1)
                                            .map({ java.net.URLDecoder.decode(it, 'UTF-8').replaceAll("&nbsp;"," ") })
                                    })
                                .findAny().orElse("") },
                 Collectors.joining()
            )
        ));

def sqlListPath = Paths.get("$userHome/$docSetHome/Gradle.docset/Contents/Resources/docSet.dsidx")

@GrabConfig(systemClassLoader=true)
@Grab('org.xerial:sqlite-jdbc:3.7.2')
def sql = Sql.newInstance("jdbc:sqlite:$sqlListPath", "org.sqlite.JDBC")

mapFileToChapterTitle.forEach( { Path x, String title -> sql.execute "INSERT OR IGNORE INTO searchIndex(name, type, path) VALUES ($title, 'Guide', $x.fileName);" } )

println sql.rows("" +
        "SELECT * FROM searchIndex").join('\n')