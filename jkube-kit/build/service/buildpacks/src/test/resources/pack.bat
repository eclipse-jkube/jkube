@echo off

echo %*
IF "%~1" == "--version" (
  echo 0.32.1+git-b14250b.build-5241
) ELSE (
  echo "%*"
)
EXIT /B 0