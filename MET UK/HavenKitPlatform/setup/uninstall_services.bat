@echo off

REM ===========================================================================
REM HPE Face Extractor service uninstaller
REM ===========================================================================

C:\HewlettPackardEnterprise\face-extraction-app\setup\XYNTService.exe -u

C:\HewlettPackardEnterprise\IDOLServer-11.1.0\mediaserver\mediaserver.exe -uninstall
echo.

C:\HewlettPackardEnterprise\IDOLServer-11.1.0\licenseserver\licenseserver.exe -uninstall
echo.

pause
