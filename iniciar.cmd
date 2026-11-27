@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

set "binJdk="
if exist "%JAVA_HOME%\bin\javac.exe" set "binJdk=%JAVA_HOME%\bin\"
if not defined binJdk (
    for /d %%J in ("%ProgramFiles%\Java\jdk*") do (
        if exist "%%~fJ\bin\javac.exe" set "binJdk=%%~fJ\bin\"
    )
)
if not defined binJdk (
    where javac >nul 2>&1
    if errorlevel 1 (
        echo JDK nao encontrado. Configure JAVA_HOME ou adicione o JDK ao PATH.
        exit /b 1
    )
)

set "saida=compilacao"
if not exist "%saida%" mkdir "%saida%" || exit /b 1
if /i "%~1"=="testar" goto testar

"%binJdk%javac.exe" -encoding UTF-8 -sourcepath src -d "%saida%" src\view\ServidorLocal.java
if errorlevel 1 exit /b 1
"%binJdk%java.exe" -cp "%saida%" view.ServidorLocal
exit /b %errorlevel%

:testar
"%binJdk%javac.exe" -encoding UTF-8 -sourcepath src -d "%saida%" test\SolicitacaoTeste.java test\ContaDoadorTeste.java test\DoadorSqliteTeste.java test\EstoqueDoadorTeste.java
if errorlevel 1 exit /b 1
for %%T in (SolicitacaoTeste ContaDoadorTeste DoadorSqliteTeste EstoqueDoadorTeste) do (
    "%binJdk%java.exe" -cp "%saida%" %%T
    if errorlevel 1 exit /b 1
)
exit /b 0
