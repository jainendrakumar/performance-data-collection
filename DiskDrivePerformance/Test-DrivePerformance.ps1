# Output CSV file
$outputFile = "C:\DrivePerf_Report.csv"

# Create or overwrite header
"Timestamp,Drive,Type,WriteSpeed_MBps,ReadSpeed_MBps,AvgDiskSecPerTransfer,DiskQueueLength,Status" | Out-File $outputFile -Encoding utf8

# Function to measure file I/O performance
function Measure-DrivePerformance {
    param (
        [string]$drive,
        [string]$type
    )

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $tempPath = Join-Path $drive "DrivePerfTemp"
    if (!(Test-Path $tempPath)) {
        New-Item -Path $tempPath -ItemType Directory | Out-Null
    }

    $testFile = Join-Path $tempPath "testfile.dat"
    $data = New-Object byte[](100MB)
    [System.Random]::new().NextBytes($data)

    # Measure Write
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    [System.IO.File]::WriteAllBytes($testFile, $data)
    $sw.Stop()
    $writeSpeed = [math]::Round((100 / $sw.Elapsed.TotalSeconds), 2)  # MB/s

    # Measure Read
    $sw.Restart()
    [System.IO.File]::ReadAllBytes($testFile) | Out-Null
    $sw.Stop()
    $readSpeed = [math]::Round((100 / $sw.Elapsed.TotalSeconds), 2)  # MB/s

    Remove-Item $testFile -Force

    # Performance Counters (optional fallback to N/A)
    $driveLetter = $drive.TrimEnd(":\\")
    $counters = @(
        "\\LogicalDisk(${driveLetter}:)\\Avg. Disk sec/Transfer",
        "\\LogicalDisk(${driveLetter}:)\\Current Disk Queue Length"
    )

    try {
        $stats = Get-Counter -Counter $counters -ErrorAction Stop
        $vals = $stats.CounterSamples | Select-Object -ExpandProperty CookedValue
        $avgDiskSec = "{0:N4}" -f $vals[0]
        $diskQueue = "{0:N2}" -f $vals[1]
        $status = "OK"
    } catch {
        $avgDiskSec = "N/A"
        $diskQueue = "N/A"
        $status = "Counters Unavailable"
    }

    # Log to CSV
    "$timestamp,$drive,$type,$writeSpeed,$readSpeed,$avgDiskSec,$diskQueue,$status" | Out-File -FilePath $outputFile -Append -Encoding utf8
}

# Local Drives
Get-WmiObject Win32_LogicalDisk -Filter "DriveType=3" | ForEach-Object {
    $drive = $_.DeviceID
    Write-Host "Testing local drive $drive..."
    Measure-DrivePerformance -drive $drive -type "Local"
}

# NAS Drives
Get-WmiObject Win32_LogicalDisk -Filter "DriveType=4" | ForEach-Object {
    $drive = $_.DeviceID
    Write-Host "Testing network drive $drive..."
    Measure-DrivePerformance -drive $drive -type "NAS"
}

Write-Host "`n✅ Drive performance test completed. Report saved to: $outputFile"
