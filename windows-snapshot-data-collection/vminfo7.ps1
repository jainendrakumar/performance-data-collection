# Comprehensive Windows VM Information and Performance Monitoring Script

# Define Output File
$outputFile = "C:\VM_Information_Report17.txt"

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
$totalRAM = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1MB
$totalCores = (Get-CimInstance Win32_Processor | Measure-Object NumberOfCores -Sum).Sum

# Capture All Applications and Services
$allProcesses = Get-Process | Select-Object ProcessName, CPU, WS, VM, Id, Threads, Handles, Path
$allServices = Get-Service | Select-Object Name, DisplayName, Status, StartType

# Capture Additional Metrics for Applications and Processes
$pageFileUsage = Get-Counter '\Paging File(_Total)\% Usage'
$diskQueueLength = Get-Counter '\PhysicalDisk(_Total)\Current Disk Queue Length'
$virtualMemoryUsage = Get-Counter '\Memory\Committed Bytes'
$processorTime = Get-Counter '\Processor(_Total)\% Processor Time'
$pagesPerSec = Get-Counter '\Memory\Pages/sec'
$logicalDiskQueueLength = Get-Counter '\LogicalDisk(_Total)\Avg. Disk Queue Length'
$physicalDiskQueueLength = Get-Counter '\PhysicalDisk(_Total)\Avg. Disk Queue Length'

# Capture All Disk-Utilizing Applications
$diskUtilizingProcesses = Get-WmiObject Win32_PerfFormattedData_PerfProc_Process | Where-Object { $_.IODataOperationsPerSec -gt 0 } | Sort-Object IODataOperationsPerSec -Descending | Select-Object Name, IODataOperationsPerSec, IOReadOperationsPerSec, IOWriteOperationsPerSec

# Capture All Network-Utilizing Applications
$networkUtilizingProcesses = Get-WmiObject Win32_PerfFormattedData_Tcpip_NetworkInterface | Where-Object { $_.BytesSentPerSec -gt 0 -or $_.BytesReceivedPerSec -gt 0 } | Sort-Object BytesSentPerSec -Descending | Select-Object Name, BytesSentPerSec, BytesReceivedPerSec

# Write Information to Output File
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
"Total RAM (MB): $totalRAM" | Out-File -FilePath $outputFile -Append -Encoding utf8
"Total CPU Cores: $totalCores" | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nAll Running Processes:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$allProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nAll Running Services:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$allServices | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nPage File Usage:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$pageFileUsage | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nDisk Queue Length:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$diskQueueLength | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nVirtual Memory Usage:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$virtualMemoryUsage | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nProcessor Time (%):`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$processorTime | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nPages/sec:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$pagesPerSec | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nLogical Disk Avg. Disk Queue Length:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$logicalDiskQueueLength | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nPhysical Disk Avg. Disk Queue Length:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$physicalDiskQueueLength | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nAll Disk-Utilizing Applications:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$diskUtilizingProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nAll Network-Utilizing Applications:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$networkUtilizingProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

Write-Host "VM information and performance metrics saved to $outputFile"