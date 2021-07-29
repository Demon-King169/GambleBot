@echo off
echo Starting the bot...

:: Build the project with gradle
echo    Building bot...
call gradlew.bat bootJar >nul 2>&1

:: create the environment variables for the bot
echo    Creating environment...
for /F %%A in (.env) do SET %%A

:: execute the program with java to avoid gradle overhead
echo    Executing bot...
start javaw -jar build\libs\gamblebot.jar <nul >nul 2>&1

echo Started the bot. The bot will appear online in a few seconds.
