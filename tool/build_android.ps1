[CmdletBinding()]
param(
    [string]$JavaHome,
    [string]$AndroidSdk,
    [string[]]$Tasks = @(':shared:jvmTest', ':androidApp:assembleDebug'),
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$repoDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$localBuildDirectory = Join-Path $repoDirectory '.local-build'
$localGradleHome = Join-Path $localBuildDirectory 'gradle-home'
$previousEnvironment = @{}
foreach ($name in @('JAVA_HOME', 'GRADLE_USER_HOME', 'ANDROID_HOME', 'ANDROID_SDK_ROOT', 'ANDROID_USER_HOME', 'TEMP', 'TMP')) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

function Copy-BuildCache([string]$Source, [string]$Destination) {
    if (!(Test-Path -LiteralPath $Source -PathType Container)) { return }
    & robocopy.exe $Source $Destination /E /XF '*.lock' '*.lck' '*.part' 'gc.properties' /R:1 /W:1 /NFL /NDL /NJH /NJS /NP | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "Could not seed the local build cache (robocopy exit $LASTEXITCODE)." }
}

try {
    New-Item -ItemType Directory -Force $localBuildDirectory | Out-Null
    if (!$JavaHome) {
        $localJdkRoot = Join-Path $localBuildDirectory 'jdk21'
        $portableJdkRoot = Join-Path $repoDirectory 'build\adapter-tools\jdk21'
        if (!(Test-Path -LiteralPath $localJdkRoot) -and (Test-Path -LiteralPath $portableJdkRoot)) {
            # Keep the toolchain outside build/, so a Gradle clean cannot remove it.
            Copy-BuildCache $portableJdkRoot $localJdkRoot
        }
        $candidates = @($env:JAVA_HOME)
        if (Test-Path -LiteralPath $localJdkRoot) {
            $candidates += @(Get-ChildItem -LiteralPath $localJdkRoot -Directory | Select-Object -ExpandProperty FullName)
        }
        foreach ($candidate in $candidates) {
            if (!$candidate) { continue }
            $releaseFile = Join-Path $candidate 'release'
            if ((Test-Path -LiteralPath $releaseFile) -and
                (Select-String -LiteralPath $releaseFile -Pattern '^JAVA_VERSION="21(?:\.|\")' -Quiet)) {
                $JavaHome = $candidate
                break
            }
        }
    }
    if (!$JavaHome -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) {
        throw 'Java 21 is required. Pass -JavaHome <JDK 21 directory> or set JAVA_HOME.'
    }
    $JavaHome = [IO.Path]::GetFullPath($JavaHome)
    if (!(Select-String -LiteralPath (Join-Path $JavaHome 'release') -Pattern '^JAVA_VERSION="21(?:\.|\")' -Quiet)) {
        throw 'The selected JavaHome must contain a Java 21 JDK.'
    }

    if (!$AndroidSdk) { $AndroidSdk = $env:ANDROID_HOME }
    if (!$AndroidSdk) { $AndroidSdk = $env:ANDROID_SDK_ROOT }
    $localProperties = Join-Path $repoDirectory 'local.properties'
    if (!$AndroidSdk -and (Test-Path -LiteralPath $localProperties)) {
        $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($sdkLine) { $AndroidSdk = ($sdkLine -replace '^sdk\.dir=', '').Replace('\\', '\').Replace('\:', ':') }
    }
    if (!$AndroidSdk -or !(Test-Path -LiteralPath $AndroidSdk -PathType Container)) {
        throw 'Android SDK is required. Pass -AndroidSdk <SDK directory> or set ANDROID_HOME.'
    }

    $globalGradleHome = $previousEnvironment['GRADLE_USER_HOME']
    if (!$globalGradleHome) { $globalGradleHome = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.gradle' }
    if ([IO.Path]::GetFullPath($globalGradleHome) -ne $localGradleHome) {
        $localModuleCache = Join-Path $localGradleHome 'caches\modules-2'
        if (!(Test-Path -LiteralPath $localModuleCache)) {
            Write-Host 'Seeding the project dependency cache from existing downloads...'
            Copy-BuildCache (Join-Path $globalGradleHome 'caches\modules-2') $localModuleCache
        }
        $wrapperProperties = Get-Content -LiteralPath (Join-Path $repoDirectory 'gradle\wrapper\gradle-wrapper.properties') -Raw
        if ($wrapperProperties -match 'distributionUrl=.*?/(gradle-[^/\r\n]+)\.zip') {
            $distributionFolder = $Matches[1]
            $localDistribution = Join-Path $localGradleHome "wrapper\dists\$distributionFolder"
            if (!(Test-Path -LiteralPath $localDistribution)) {
                Copy-BuildCache (Join-Path $globalGradleHome "wrapper\dists\$distributionFolder") $localDistribution
            }
        }
    }

    $env:JAVA_HOME = $JavaHome
    $env:GRADLE_USER_HOME = $localGradleHome
    $env:ANDROID_HOME = [IO.Path]::GetFullPath($AndroidSdk)
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    $env:ANDROID_USER_HOME = Join-Path $localBuildDirectory 'android-user-home'
    New-Item -ItemType Directory -Force $env:ANDROID_USER_HOME | Out-Null
    $localTempDirectory = Join-Path $localBuildDirectory 'temp'
    New-Item -ItemType Directory -Force $localTempDirectory | Out-Null
    $env:TEMP = $localTempDirectory
    $env:TMP = $localTempDirectory

    $gradleArguments = @(
        '--no-daemon', '--no-configuration-cache', '--max-workers=2', '--console=plain',
        '-Pkotlin.compiler.execution.strategy=in-process', "-Djava.io.tmpdir=$localTempDirectory"
    )
    $proxyValue = $env:HTTPS_PROXY
    if (!$proxyValue) { $proxyValue = $env:HTTP_PROXY }
    if ($proxyValue) {
        $proxyUri = [Uri]$proxyValue
        if ($proxyUri.Scheme -ne 'http') { throw 'The build launcher requires an HTTP CONNECT proxy in HTTPS_PROXY/HTTP_PROXY.' }
        if ($proxyUri.UserInfo) { throw 'Authenticated proxies must be configured separately in Gradle; credentials are not persisted by this launcher.' }
        foreach ($protocol in @('http', 'https')) {
            $gradleArguments += "-D$protocol.proxyHost=$($proxyUri.Host)"
            $gradleArguments += "-D$protocol.proxyPort=$($proxyUri.Port)"
        }
        Write-Host "Using the configured proxy at $($proxyUri.Host):$($proxyUri.Port)."
    }
    if ($Offline) { $gradleArguments += '--offline' }
    $gradleArguments += $Tasks
    Write-Host "Java 21: $JavaHome"
    Write-Host "Project Gradle cache: $localGradleHome"
    Push-Location $repoDirectory
    try {
        & (Join-Path $repoDirectory 'gradlew.bat') @gradleArguments
        $buildExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($buildExitCode -ne 0) { throw "Gradle failed with exit code $buildExitCode." }
} finally {
    foreach ($name in $previousEnvironment.Keys) {
        if ($null -eq $previousEnvironment[$name]) {
            Remove-Item -LiteralPath "Env:$name" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
        }
    }
}
