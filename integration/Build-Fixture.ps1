param(
    [Parameter(Mandatory)][string]$PaperLibraries,
    [Parameter(Mandatory)][string]$ProtectionStonesJar,
    [Parameter(Mandatory)][string]$FakePlayerJar
)

$ErrorActionPreference = 'Stop'
$output = Join-Path $PSScriptRoot 'build'
$classes = Join-Path $output 'classes'
New-Item -Path $classes -ItemType Directory -Force | Out-Null
$libraries = Get-ChildItem -LiteralPath $PaperLibraries -Recurse -Filter '*.jar' -File
$plugins = (Resolve-Path -LiteralPath $ProtectionStonesJar,$FakePlayerJar).Path
$classPath = (@($libraries.FullName) + @($plugins)) -join [IO.Path]::PathSeparator
$source = Join-Path $PSScriptRoot 'PsHomeScenarios.java'
& javac --release 25 -proc:none -encoding UTF-8 -classpath $classPath -d $classes $source
if ($LASTEXITCODE -ne 0) { throw 'Fixture compilation failed.' }
$jarPath = Join-Path $output 'PsHomeScenarios.jar'
& jar --create --file $jarPath -C $classes . -C $PSScriptRoot plugin.yml
if ($LASTEXITCODE -ne 0) { throw 'Fixture packaging failed.' }
Get-Item -LiteralPath $jarPath
