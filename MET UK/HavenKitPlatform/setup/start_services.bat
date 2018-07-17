@echo off

REM ===========================================================================
REM HPE Face Extractor service starter
REM ===========================================================================

net start "HPE License Server"
echo.

pause 10

net start "HPE Media Server"
echo.

net start "HPE HavenKitPlatform Services"
echo .

pause
