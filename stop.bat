:: THIS FORCES THE PROCESS TO STOP IMMEDIATELY, NO SHUTDOWN HOOKS WILL GET EXECUTED. DATA MAY BE LOST!!!
@echo off
echo Stopping the bot forcefully...

:: not quite sure yet how to kill the process not forcefully as removing /F will refuse to kill the process
:: apparently that's due to the jar running in a console without a window
for /f "tokens=1" %%i in ('jps -m ^| find "gamblebot.jar"') do ( taskkill /F /PID %%i )
echo Stopped the bot (might get shown as online for a few minutes due to forced shutdown).