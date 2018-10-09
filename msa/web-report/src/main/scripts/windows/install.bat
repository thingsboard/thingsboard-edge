@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Installing ${pkg.name} ...

SET BASE=%~dp0

%BASE%${pkg.name}.exe install

@ECHO ${pkg.name} installed successfully!

GOTO END

:END
