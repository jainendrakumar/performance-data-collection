# Comprehensive Windows VM Information and Performance Monitoring Script

# Define Output File
$outputFile = "C:\VM_Information_Report.txt"

# Collect System Information
$systemInfo = Get-ComputerInfo
$osInfo = Get-CimInstance Win32_OperatingSystem
$cpuInfo = Get-CimInstance Win32_Processor
$memoryInfo = Get-CimInstance Win32_PhysicalMemory
$diskInfo = Get-CimInstance Win32_DiskDrive
$networkInfo = Get-NetAdapter
$servicesInfo = Get-Service | Select-Object Name, DisplayName, Status, StartType
$vmInfo = Get-CimInstance Win32_ComputerSystem
$biosInfo = Get-CimInstance Win32_BIOS
$motherboardInfo = Get-CimInstance Win32_BaseBoard
$gpuInfo = Get-CimInstance Win32_VideoController

# Write System Information to a Single File
"System Information:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$systemInfo | Out-File -FilePath $outputFile -Append -Encoding utf8
$osInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$cpuInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$memoryInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$diskInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$networkInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$servicesInfo | Out-File -FilePath $outputFile -Append -Encoding utf8
$vmInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$biosInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$motherboardInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$gpuInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

# Initialize CSV Headers in the Same File
"`nPerformance Metrics:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
"Timestamp,CPU Usage (%),Processor Queue Length,Memory Usage (MB),Committed Memory (MB),Page File Usage (MB),Disk Read (MB/s),Disk Write (MB/s),Disk Queue Length,Network Sent (MB/s),Network Received (MB/s),Network Packets Sent/sec,Network Packets Received/sec,Logical Disk % Free Inodes,Logical Disk % Free Space,Logical Disk % Used Inodes,Logical Disk % Used Space,Logical Disk Disk Read Bytes/sec,Logical Disk Disk Reads/sec,Logical Disk Disk Transfers/sec,Logical Disk Disk Write Bytes/sec,Logical Disk Disk Writes/sec,Logical Disk Free Megabytes,Logical Disk Logical Disk Bytes/sec,Memory % Available Memory,Memory % Available Swap Space,Memory % Used Memory,Memory % Used Swap Space,Memory Available MBytes Memory,Memory Available MBytes Swap,Memory Page Reads/sec,Memory Page Writes/sec,Memory Pages/sec,Memory Used MBytes Swap Space,Memory Used Memory MBytes,Network Total Bytes Transmitted,Network Total Bytes Received,Network Total Bytes,Network Total Packets Transmitted,Network Total Packets Received,Network Total Rx Errors,Network Total Tx Errors,Network Total Collisions,Physical Disk Avg. Disk sec/Read,Physical Disk Avg. Disk sec/Transfer,Physical Disk Avg. Disk sec/Write,Physical Disk Physical Disk Bytes/sec,Process Pct Privileged Time,Process Pct User Time,Process Used Memory kBytes,Process Virtual Shared Memory,Processor % DPC Time,Processor % Idle Time,Processor % Interrupt Time,Processor % IO Wait Time,Processor % Nice Time,Processor % Privileged Time,Processor % Processor Time,Processor % User Time,System Free Physical Memory,System Free Space in Paging Files,System Free Virtual Memory,System Processes,System Size Stored In Paging Files,System Uptime,System Users" | Out-File -FilePath $outputFile -Append -Encoding utf8

# Function to Capture Performance Metrics
function Capture-PerformanceMetrics {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $counters = @(
        '\Processor(_Total)\% Processor Time',
        '\System\Processor Queue Length',
        '\Memory\Available MBytes',
        '\Memory\Committed Bytes',
        '\Paging File(_Total)\% Usage',
        '\PhysicalDisk(_Total)\Disk Read Bytes/sec',
        '\PhysicalDisk(_Total)\Disk Write Bytes/sec',
        '\PhysicalDisk(_Total)\Current Disk Queue Length',
        '\Network Interface(*)\Bytes Sent/sec',
        '\Network Interface(*)\Bytes Received/sec',
        '\Network Interface(*)\Packets Sent/sec',
        '\Network Interface(*)\Packets Received/sec'
    )
    
    $values = $counters | ForEach-Object {
        try {
            (Get-Counter $_).CounterSamples.CookedValue
        } catch {
            "N/A"
        }
    }
    
    "$timestamp,$($values -join ',')" | Out-File -FilePath $outputFile -Append -Encoding utf8
}

# Monitor Performance for 2 Minutes (120 seconds)
Write-Host "Monitoring Performance for 2 minutes..."
$endTime = (Get-Date).AddMinutes(2)
while ((Get-Date) -lt $endTime) {
    Capture-PerformanceMetrics
    Start-Sleep -Seconds 1  # Capture metrics every second
}

Write-Host "VM information and performance metrics saved to $outputFile"