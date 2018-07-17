@echo off

REM ===========================================================================
REM HPE Face Extractor service stopper
REM ===========================================================================

net stop "HPE Face Extractor Services"
echo .

net stop "HPE Media Server"
echo.

net stop "HPE License Server"
echo.

pause
