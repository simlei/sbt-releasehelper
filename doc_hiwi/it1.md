Tätigkeiten:
=========

 - Issue #102 untersucht
 - Scalariform multiple runs untersucht (bisher kein Erfolg)
 - Release automation
    - SBT plugin begonnen
    - FTP und external config implementation, erste Tests
    - lokaler FTP-Server zum Testen
    - Tasks für alle notwendigen Schritte angelegt und 2dos / fragen

Gelesen:
 - wegen vielen wiederholten Ivy resolutions: https://bitbucket.org/delors/opal/issues/102/build-process-dies-dramatically-in-case-of
 - Wiederholung SBT scoping: http://www.scala-sbt.org/0.13/docs/Cached-Resolution.html
 - Sequencing von SBT tasks: http://eed3si9n.com/sequencing-tasks-with-sbt-sequential
 - Error handling with Try[_] http://danielwestheide.com/blog/2012/12/26/the-neophytes-guide-to-scala-part-6-error-handling-with-try.html
 - etwas ftp4j documentation

Fragen
 - https://github.com/sbt/sbt-release
 - s. ReleasehelperPlugin