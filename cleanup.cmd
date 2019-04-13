@echo off
REM based on: http://forum.oszone.net/thread-321401.html

setlocal
set DEBUG=false
set "ext=.jpg/.png/.diff"

for %%j in ("Screenshots" "differences") do (
  for /f "delims=" %%k in ('2^>nul dir %%j\ /a-d /b /s^| findstr /e /i /l /c:"%ext:/=" /c:"%"') do (
    if /i "%DEBUG%"=="true" ( >>cleanup.log echo del /a /f "%%k"
    ) else (
      echo del /a /f "%%k"
    )
  )
)
endlocal
exit /b
