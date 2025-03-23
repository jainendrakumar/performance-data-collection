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
$totalRAM = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1MB
$totalCores = (Get-CimInstance Win32_Processor | Measure-Object NumberOfCores -Sum).Sum

# Capture All Applications and Services
$allProcesses = Get-Process | Select-Object ProcessName, CPU, WS, VM, Id, Threads, Handles, Path
$allServices = Get-Service | Select-Object Name, DisplayName, Status, StartType

# Capture All Disk-Utilizing Applications
$diskUtilizingProcesses = Get-WmiObject Win32_PerfFormattedData_PerfProc_Process | Where-Object { $_.IODataOperationsPerSec -gt 0 } | Sort-Object IODataOperationsPerSec -Descending | Select-Object Name, IODataOperationsPerSec, IOReadOperationsPerSec, IOWriteOperationsPerSec

# Capture All Network-Utilizing Applications
$networkUtilizingProcesses = Get-WmiObject Win32_PerfFormattedData_Tcpip_NetworkInterface | Where-Object { $_.BytesSentPerSec -gt 0 -or $_.BytesReceivedPerSec -gt 0 } | Sort-Object BytesSentPerSec -Descending | Select-Object Name, BytesSentPerSec, BytesReceivedPerSec

# Capture Processes Using Threads, Handles, Cache
$threadProcesses = Get-Process | Sort-Object Threads -Descending | Select-Object -First 10 ProcessName, Id, Threads, Handles, Path
$cacheProcesses = Get-Process | Sort-Object VirtualMemorySize64 -Descending | Select-Object -First 10 ProcessName, Id, VirtualMemorySize64, Path

# Capture System Profiling Data (Interrupts, DPC, Loaders)
$interrupts = Get-Counter '\Processor(_Total)\% Interrupt Time'
$dpcTime = Get-Counter '\Processor(_Total)\% DPC Time'

# Get List of Excessive WMI Queries by Processes
$wmiQueries = Get-WmiObject Win32_Process | Where-Object { $_.CommandLine -match "wmic" } | Select-Object ProcessId, Name, CommandLine

# Get List of Processes Using CPU
$cpuProcesses = Get-Process | Sort-Object CPU -Descending | Select-Object ProcessName, CPU, Id, Threads, Handles

# Get List of All svchost.exe Processes and Their CPU Utilization
$svchostProcesses = Get-Process -Name svchost | Select-Object ProcessName, CPU, Id, Threads, Handles

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

"`nAll Disk-Utilizing Applications:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$diskUtilizingProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nAll Network-Utilizing Applications:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$networkUtilizingProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nProcesses Using Threads and Handles:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$threadProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nProcesses Using Cache:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$cacheProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nSystem Profiling Data (Interrupts, DPC, Loaders):`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$interrupts | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$dpcTime | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nList of Excessive WMI Queries by Processes:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$wmiQueries | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nList of Processes Using CPU:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$cpuProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nList of All svchost.exe Processes and CPU Utilization:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$svchostProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

Write-Host "VM information and performance metrics saved to $outputFile"