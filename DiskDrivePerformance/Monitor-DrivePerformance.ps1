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

    # Local drives (DriveType = 3)
    Get-WmiObject Win32_LogicalDisk -Filter "DriveType=3" | ForEach-Object {
        $drive = $_.DeviceID              # e.g., "C:"
        $counterName = $drive.Replace(":", "")  # e.g., "C"
        $freeMB = [math]::Round($_.FreeSpace / 1MB, 2)
        $sizeMB = [math]::Round($_.Size / 1MB, 2)
        $usedMB = $sizeMB - $freeMB

        $counters = @(
            "\\LogicalDisk(${counterName}:)\\Disk Read Bytes/sec",
            "\\LogicalDisk(${counterName}:)\\Disk Write Bytes/sec",
            "\\LogicalDisk(${counterName}:)\\Avg. Disk sec/Transfer",
            "\\LogicalDisk(${counterName}:)\\Current Disk Queue Length",
            "\\LogicalDisk(${counterName}:)\\Disk Reads/sec",
            "\\LogicalDisk(${counterName}:)\\Disk Writes/sec"
        )

        try {
            $stats = Get-Counter -Counter $counters -ErrorAction Stop
            $vals = $stats.CounterSamples | Select-Object -ExpandProperty CookedValue
            if ($vals.Count -eq 6) {
                "$timestamp,$drive,Local,{0:F2},{1:F2},{2:F4},{3:F2},{4:F2},{5:F2},$freeMB,$usedMB" -f $vals[0],$vals[1],$vals[2],$vals[3],$vals[4],$vals[5] |
                Out-File -FilePath $outputFile -Append -Encoding utf8
            } else {
                "$timestamp,$drive,Local,N/A,N/A,N/A,N/A,N/A,N/A,$freeMB,$usedMB" | Out-File -FilePath $outputFile -Append -Encoding utf8
            }
        } catch {
            "$timestamp,$drive,Local,N/A,N/A,N/A,N/A,N/A,N/A,$freeMB,$usedMB" | Out-File -FilePath $outputFile -Append -Encoding utf8
        }
    }

    # Network drives (NAS - DriveType = 4)
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

Write-Host "`nDrive performance log saved to: $outputFile"
