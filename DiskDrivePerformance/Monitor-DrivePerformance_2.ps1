# Define Output File
$outputFile = "C:\Drive_Performance_Report.txt"

# Define the duration of monitoring (15 minutes)
$duration = 15 * 60  # 15 minutes in seconds

# Define the interval for collecting performance data (e.g., every 10 seconds)
$interval = 10

# Define the performance counters to monitor
$counters = @(
    "\PhysicalDisk(*)\Disk Reads/sec",
    "\PhysicalDisk(*)\Disk Writes/sec",
    "\PhysicalDisk(*)\Avg. Disk sec/Read",
    "\PhysicalDisk(*)\Avg. Disk sec/Write",
    "\PhysicalDisk(*)\Current Disk Queue Length",
    "\LogicalDisk(*)\Disk Reads/sec",
    "\LogicalDisk(*)\Disk Writes/sec",
    "\LogicalDisk(*)\Avg. Disk sec/Read",
    "\LogicalDisk(*)\Avg. Disk sec/Write",
    "\LogicalDisk(*)\Current Disk Queue Length"
)

# Initialize a hashtable to store the performance data
$performanceData = @{}

# Function to collect performance data
function Collect-PerformanceData {
    param (
        [int]$duration,
        [int]$interval
    )

    $endTime = (Get-Date).AddSeconds($duration)
    while ((Get-Date) -lt $endTime) {
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        $performanceData[$timestamp] = @()

        foreach ($counter in $counters) {
            $counterData = Get-Counter -Counter $counter
            foreach ($counterInstance in $counterData.CounterSamples) {
                $instanceName = $counterInstance.InstanceName
                $counterValue = $counterInstance.CookedValue
                $performanceData[$timestamp] += [PSCustomObject]@{
                    Counter = $counter
                    Instance = $instanceName
                    Value = $counterValue
                }
            }
        }

        Start-Sleep -Seconds $interval
    }
}

# Collect performance data
Collect-PerformanceData -duration $duration -interval $interval

# Write performance data to the output file
"Drive Performance Report`n" | Out-File -FilePath $outputFile -Encoding utf8
"Monitoring Duration: 15 minutes`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
"Interval: Every 10 seconds`n" | Out-File -FilePath $outputFile -Append -Encoding utf8

foreach ($timestamp in $performanceData.Keys) {
    "Timestamp: $timestamp`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
    $performanceData[$timestamp] | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8
    "`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
}

Write-Host "Drive performance data saved to $outputFile"