# Set output path
$outputFile = "C:\Drive_Performance_Log.csv"

# Duration and interval
$durationMinutes = 15
$intervalSeconds = 10
$endTime = (Get-Date).AddMinutes($durationMinutes)

# Initialize CSV header
"Timestamp,Drive,Type,Disk Read Bytes/sec,Disk Write Bytes/sec,Avg Disk sec/Transfer,Disk Queue Length,Disk Reads/sec,Disk Writes/sec,Free Space (MB),Used Space (MB)" | Out-File $outputFile -Encoding utf8

function Get-DriveStats {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

    # Local drives
    Get-WmiObject Win32_LogicalDisk -Filter "DriveType=3" | ForEach-Object {
        $drive = $_.DeviceID
        $sizeMB = [math]::Round($_.Size / 1MB, 2)
        $freeMB = [math]::Round($_.FreeSpace / 1MB, 2)
        $usedMB = $sizeMB - $freeMB

        $stats = Get-Counter -Counter @(
            "\\LogicalDisk($drive)\\Disk Read Bytes/sec",
            "\\LogicalDisk($drive)\\Disk Write Bytes/sec",
            "\\LogicalDisk($drive)\\Avg. Disk sec/Transfer",
            "\\LogicalDisk($drive)\\Current Disk Queue Length",
            "\\LogicalDisk($drive)\\Disk Reads/sec",
            "\\LogicalDisk($drive)\\Disk Writes/sec"
        ) -ErrorAction SilentlyContinue

        $vals = $stats.CounterSamples | Select-Object -ExpandProperty CookedValue
        "$timestamp,$drive,Local,{0:F2},{1:F2},{2:F4},{3:F2},{4:F2},{5:F2},$freeMB,$usedMB" -f $vals[0],$vals[1],$vals[2],$vals[3],$vals[4],$vals[5] |
        Out-File -FilePath $outputFile -Append -Encoding utf8
    }

    # Network drives (NAS)
    Get-WmiObject Win32_LogicalDisk -Filter "DriveType=4" | ForEach-Object {
        $drive = $_.DeviceID
        $freeMB = [math]::Round($_.FreeSpace / 1MB, 2)
        $sizeMB = [math]::Round($_.Size / 1MB, 2)
        $usedMB = $sizeMB - $freeMB

        "$timestamp,$drive,NAS,N/A,N/A,N/A,N/A,N/A,N/A,$freeMB,$usedMB" |
        Out-File -FilePath $outputFile -Append -Encoding utf8
    }
}

# Start monitoring loop
Write-Host "Monitoring drive performance for $durationMinutes minutes..."
while ((Get-Date) -lt $endTime) {
    Get-DriveStats
    Start-Sleep -Seconds $intervalSeconds
}

Write-Host "Drive performance log saved to: $outputFile"
