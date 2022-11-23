@echo OFF
java -Xmx1200M -cp target/xapi_core-0.0.1.jar;target/lib/* de.hsuifa.xapi.xapi_core.Launcher %*
pause