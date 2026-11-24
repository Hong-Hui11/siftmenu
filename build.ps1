# Sift Menu build script (no Gradle required).
#
# Requirements:
#   - JDK 25 (javac must target release 25)
#   - Minecraft 26.3 libraries (usually %APPDATA%\.minecraft\libraries)
#   - An access-widened Minecraft jar (mc-26.3-aw.jar)
#   - A mods directory that contains fabric-api and the mod's optional integrations
#
# Example:
#   .\build.ps1 -McJar "C:\path\to\mc-26.3-aw.jar" -ModsDir "D:\...\mods"
#
# Output: build/siftmenu-<version>.jar

param(
    [string]$McJar = "",
    [string]$ModsDir = "",
    [string]$Libraries = "$env:APPDATA\.minecraft\libraries",
    [string]$JavaHome = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcJava = Join-Path $root "src\main\java"
$srcResources = Join-Path $root "src\main\resources"
$buildDir = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$stageDir = Join-Path $buildDir "stage"
$fapiDir = Join-Path $buildDir "fapi"

function Find-Jdk {
    param([string]$explicit)
    $candidates = @()
    if ($explicit) { $candidates += $explicit }
    $candidates += "E:\tools\jdk-25.0.4.1+1"
    $candidates += "$env:APPDATA\.minecraft\runtime\java-runtime-epsilon"
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    foreach ($jdkHome in $candidates) {
        $bin = Join-Path $jdkHome "bin\javac.exe"
        if ($jdkHome -and (Test-Path $bin)) {
            $version = (& $bin -version 2>&1) -join " "
            if ($version -match "javac 25") {
                return $bin
            }
        }
    }
    throw "javac 25 not found. Pass -JavaHome pointing to a JDK 25 or set JAVA_HOME."
}

$javac = Find-Jdk $JavaHome
Write-Host "[build] javac: $javac"

if (-not $McJar) {
    $local = Join-Path $root "lib\mc-26.3-aw.jar"
    if (Test-Path $local) { $McJar = $local }
}
if (-not $McJar -or -not (Test-Path $McJar)) {
    throw "Minecraft dependency jar not found. Pass -McJar pointing to mc-26.3-aw.jar."
}
if (-not $ModsDir -or -not (Test-Path $ModsDir)) {
    $ModsDir = "$env:APPDATA\.minecraft\versions\Echoes of the Sift Fabric\mods"
}
if (-not (Test-Path $ModsDir)) {
    throw "mods directory not found. Pass -ModsDir."
}

# ---------- extract Fabric API submodules ----------
New-Item -ItemType Directory -Force -Path $fapiDir | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apiJar = Get-ChildItem $ModsDir -Filter "fabric-api-*.jar" | Select-Object -First 1
if ($apiJar) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($apiJar.FullName)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -like "META-INF/jars/*.jar") {
                $target = Join-Path $fapiDir (Split-Path $entry.FullName -Leaf)
                if (-not (Test-Path $target)) {
                    $in = $entry.Open()
                    try {
                        $out = [System.IO.File]::Create($target)
                        try { $in.CopyTo($out) } finally { $out.Dispose() }
                    } finally {
                        $in.Dispose()
                    }
                }
            }
        }
    } finally {
        $zip.Dispose()
    }
} else {
    Write-Warning "[build] fabric-api-*.jar not found in mods directory"
}

# ---------- classpath ----------
$cp = New-Object System.Collections.Generic.List[string]
$cp.Add($McJar)
Get-ChildItem $Libraries -Recurse -Filter "*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch "\\natives\\" } |
    ForEach-Object { $cp.Add($_.FullName) }
Get-ChildItem $fapiDir -Filter "*.jar" | ForEach-Object { $cp.Add($_.FullName) }
Get-ChildItem $ModsDir -Filter "*.jar" | ForEach-Object { $cp.Add($_.FullName) }

$joinedCp = ($cp | ForEach-Object { $_.Replace("\", "/") }) -join ";"

# ---------- clean ----------
if (Test-Path $classesDir) { Remove-Item -Recurse -Force $classesDir }
if (Test-Path $stageDir) { Remove-Item -Recurse -Force $stageDir }
New-Item -ItemType Directory -Force -Path $classesDir, $stageDir | Out-Null

# ---------- compile ----------
$sources = Get-ChildItem $srcJava -Recurse -Filter "*.java"
Write-Host "[build] compiling $($sources.Count) source files..."
$argFile = Join-Path $buildDir "javac.args"
$lines = @("-proc:none", "-nowarn", "--release", "25", "-encoding", "UTF-8", "-cp", "`"$joinedCp`"", "-d", "`"$($classesDir.Replace('\','/'))`"")
foreach ($s in $sources) { $lines += "`"$($s.FullName.Replace('\','/'))`"" }
[System.IO.File]::WriteAllLines($argFile, $lines, (New-Object System.Text.UTF8Encoding($false)))

& $javac "-J-Duser.language=en" "-J-Duser.country=US" "-Xmaxerrs" "200" "@$argFile"
if ($LASTEXITCODE -ne 0) {
    throw "[build] compile failed (exit $LASTEXITCODE)"
}

# ---------- package ----------
Copy-Item -Recurse -Force "$srcResources\*" $stageDir
Copy-Item -Recurse -Force "$classesDir\*" $stageDir
$version = "0.0.0"
$fabricMod = Join-Path $stageDir "fabric.mod.json"
if (Test-Path $fabricMod) {
    $json = Get-Content $fabricMod -Raw -Encoding UTF8 | ConvertFrom-Json
    $version = $json.version
}
$jarPath = Join-Path $buildDir "siftmenu-$version.jar"
if (Test-Path $jarPath) { Remove-Item -Force $jarPath }
$jarTool = Join-Path (Split-Path (Split-Path $javac -Parent) -Parent) "bin\jar.exe"
if (-not (Test-Path $jarTool)) {
    throw "jar tool not found: $jarTool"
}
# Use the JDK jar tool: .NET ZipFile on Windows stores entry names with backslashes,
# which makes the JVM unable to load classes from the archive.
& $jarTool --create --file $jarPath -C $stageDir .
if ($LASTEXITCODE -ne 0) {
    throw "[build] package failed (exit $LASTEXITCODE)"
}
Write-Host "[build] done: $jarPath"
