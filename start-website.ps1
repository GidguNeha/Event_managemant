$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$frontendUrl = "http://localhost:5500/"
$apiUrl = "http://localhost:8081/api/events"

function Test-WebsiteEndpoint([string]$url) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Wait-ForWebsiteEndpoint([string]$url, [System.Diagnostics.Process]$process, [int]$timeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-WebsiteEndpoint $url) {
            return
        }
        if ($process.HasExited) {
            throw "The server process exited while starting. Check its terminal for the startup error."
        }
        Start-Sleep -Seconds 2
    }
    throw "The server did not respond at $url within $timeoutSeconds seconds. Check its terminal for the startup error."
}

$node = Get-Command node -ErrorAction SilentlyContinue
$nodePath = if ($node) { $node.Source } else { $null }
if (-not $nodePath) {
    $programFilesNode = Join-Path ${env:ProgramFiles} "nodejs\node.exe"
    if (Test-Path $programFilesNode) { $nodePath = $programFilesNode }
}
if (-not $nodePath) {
    throw "Node.js was not found. Install Node.js 18 or newer, then run start-website.cmd again."
}

$npm = Get-Command npm -ErrorAction SilentlyContinue
if (-not $npm -and $nodePath) {
    $npmCandidate = (Split-Path $nodePath -Parent) + "\npm.cmd"
    if (Test-Path $npmCandidate) {
        $npm = @{ Source = $npmCandidate }
    }
}

$java = Get-Command java -ErrorAction SilentlyContinue
$javaPath = if ($java) { $java.Source } else { $null }
if (-not $javaPath -and $env:JAVA_HOME) {
    $configuredJava = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $configuredJava) { $javaPath = $configuredJava }
}
if (-not $javaPath) {
    $javaDirectories = Join-Path $root ".tools\java"
    if (Test-Path $javaDirectories) {
        foreach ($directory in Get-ChildItem -Path $javaDirectories -Directory) {
            $candidate = Join-Path $directory.FullName "bin\java.exe"
            if (Test-Path $candidate) {
                $javaPath = $candidate
                break
            }
        }
    }
}
if (-not $javaPath) {
    throw "Java 17 or newer was not found. Install Java or provide it in .tools\java, then run start-website.cmd again."
}

$maven = Get-Command mvn -ErrorAction SilentlyContinue
$mavenPath = if ($maven) { $maven.Source } else { $null }
if (-not $mavenPath) {
    $bundledMaven = Join-Path $root ".tools\maven\apache-maven-3.9.11\bin\mvn.cmd"
    if (Test-Path $bundledMaven) { $mavenPath = $bundledMaven }
}
if (-not $mavenPath) {
    throw "Maven was not found. Install Maven 3.8 or newer or provide it in .tools\maven, then run start-website.cmd again."
}

$javaHome = Split-Path (Split-Path $javaPath -Parent) -Parent
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$($env:Path)"

$frontendProcess = $null
if (-not (Test-WebsiteEndpoint $frontendUrl)) {
    $frontendProcess = Start-Process -FilePath $nodePath -ArgumentList "server.cjs" `
        -WorkingDirectory (Join-Path $root "frontend") -PassThru
}

$backendProcess = $null
if (-not (Test-WebsiteEndpoint $apiUrl)) {
    $backendCommand = @"
`$env:JAVA_HOME = '$javaHome'
`$env:Path = (Join-Path `$env:JAVA_HOME 'bin') + ';' + `$env:Path
`$env:SPRING_PROFILES_ACTIVE = 'demo'
`$env:PORT = '8081'
`$env:CORS_ALLOWED_ORIGIN = 'http://localhost:5500'
Remove-Item Env:DB_URL -ErrorAction SilentlyContinue
Set-Location '$root'
& '$mavenPath' -f backend\pom.xml spring-boot:run
if (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE }
"@
    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($backendCommand))
    $backendProcess = Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-NoExit", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encodedCommand) `
        -WorkingDirectory $root -PassThru
}

if ($frontendProcess) {
    Wait-ForWebsiteEndpoint $frontendUrl $frontendProcess 30
}
if ($backendProcess) {
    Wait-ForWebsiteEndpoint $apiUrl $backendProcess 120
}

Write-Host "EventHub is ready at $frontendUrl"
Write-Host "API is responding at $apiUrl"
Write-Host "Keep the server terminal windows open while using the site. Run this script again to reopen the site."
if (-not $env:EVENTHUB_SKIP_BROWSER) {
    Start-Process $frontendUrl
}
