@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  myplugin startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and MYPLUGIN_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\spring-boot-starter-amqp-1.5.8.RELEASE.jar;%APP_HOME%\lib\myplugin-5.0.1-SNAPSHOT.jar;%APP_HOME%\lib\guava-19.0-rc2.jar;%APP_HOME%\lib\monitoring-5.0.0.jar;%APP_HOME%\lib\exception-5.0.0.jar;%APP_HOME%\lib\logback-core-1.1.11.jar;%APP_HOME%\lib\spring-messaging-4.3.12.RELEASE.jar;%APP_HOME%\lib\httpclient-4.3.6.jar;%APP_HOME%\lib\spring-aop-4.3.12.RELEASE.jar;%APP_HOME%\lib\spring-core-4.3.12.RELEASE.jar;%APP_HOME%\lib\classmate-1.3.1.jar;%APP_HOME%\lib\mbknor-jackson-jsonschema_2.12-1.0.24.jar;%APP_HOME%\lib\jul-to-slf4j-1.7.25.jar;%APP_HOME%\lib\commons-codec-1.6.jar;%APP_HOME%\lib\commons-lang3-3.5.jar;%APP_HOME%\lib\snakeyaml-1.17.jar;%APP_HOME%\lib\spring-boot-starter-logging-1.5.8.RELEASE.jar;%APP_HOME%\lib\logback-classic-1.1.11.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\plugin-sdk-5.0.0.jar;%APP_HOME%\lib\spring-expression-4.3.12.RELEASE.jar;%APP_HOME%\lib\spring-tx-4.3.11.RELEASE.jar;%APP_HOME%\lib\hibernate-validator-5.3.5.Final.jar;%APP_HOME%\lib\json-schema-validator-0.1.10.jar;%APP_HOME%\lib\amqp-client-4.0.3.jar;%APP_HOME%\lib\log4j-over-slf4j-1.7.25.jar;%APP_HOME%\lib\spring-amqp-1.7.4.RELEASE.jar;%APP_HOME%\lib\commons-text-1.1.jar;%APP_HOME%\lib\slf4j-api-1.7.25.jar;%APP_HOME%\lib\jackson-core-2.8.7.jar;%APP_HOME%\lib\javax.el-api-3.0.0.jar;%APP_HOME%\lib\plugin-5.0.0.jar;%APP_HOME%\lib\spring-beans-4.3.12.RELEASE.jar;%APP_HOME%\lib\spring-rabbit-1.7.4.RELEASE.jar;%APP_HOME%\lib\jackson-databind-2.8.7.jar;%APP_HOME%\lib\spring-boot-1.5.8.RELEASE.jar;%APP_HOME%\lib\registration-5.0.0.jar;%APP_HOME%\lib\scala-library-2.12.1.jar;%APP_HOME%\lib\validation-api-1.1.0.Final.jar;%APP_HOME%\lib\json-20090211.jar;%APP_HOME%\lib\javax.el-2.2.6.jar;%APP_HOME%\lib\gson-2.8.0.jar;%APP_HOME%\lib\jcl-over-slf4j-1.7.25.jar;%APP_HOME%\lib\vim-drivers-5.0.0.jar;%APP_HOME%\lib\catalogue-5.0.0.jar;%APP_HOME%\lib\unirest-java-1.3.0.jar;%APP_HOME%\lib\spring-context-4.3.12.RELEASE.jar;%APP_HOME%\lib\spring-boot-starter-1.5.8.RELEASE.jar;%APP_HOME%\lib\hibernate-jpa-2.1-api-1.0.0.Final.jar;%APP_HOME%\lib\common-5.0.0.jar;%APP_HOME%\lib\spring-retry-1.2.0.RELEASE.jar;%APP_HOME%\lib\jackson-annotations-2.8.6.jar;%APP_HOME%\lib\httpcore-4.3.3.jar;%APP_HOME%\lib\spring-boot-autoconfigure-1.5.8.RELEASE.jar;%APP_HOME%\lib\jboss-logging-3.3.0.Final.jar;%APP_HOME%\lib\fast-classpath-scanner-2.0.20.jar;%APP_HOME%\lib\http-client-1.1.1.RELEASE.jar;%APP_HOME%\lib\httpasyncclient-4.0-beta4.jar;%APP_HOME%\lib\httpcore-nio-4.3-beta2.jar;%APP_HOME%\lib\slf4j-ext-1.7.25.jar;%APP_HOME%\lib\httpmime-4.3.jar;%APP_HOME%\lib\spring-web-4.3.11.RELEASE.jar

@rem Execute myplugin
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %MYPLUGIN_OPTS%  -classpath "%CLASSPATH%" org.icinga.myexample.MyPlugin %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable MYPLUGIN_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%MYPLUGIN_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
