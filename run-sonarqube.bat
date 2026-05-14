@echo off
set TOKEN=squ_ecf5cd101c211e4bcbd04c43088e664d731dbb50
set URL=http://localhost:9000

set SERVICES=ai-service auth-service export-service job-match-service notification-service resume-service template-service

for %%S in (%SERVICES%) do (
    echo.
    echo ========================================================
    echo Running SonarQube Analysis for %%S...
    echo ========================================================
    cd %%S
    call mvn clean verify sonar:sonar -Dsonar.projectKey=resumade-%%S -Dsonar.projectName="Resumade %%S" -Dsonar.host.url=%URL% -Dsonar.token=%TOKEN%
    cd ..
)

echo.
echo ========================================================
echo ALL SERVICES ANALYZED!
echo Please check your SonarQube dashboard at http://localhost:9000
echo ========================================================
pause
