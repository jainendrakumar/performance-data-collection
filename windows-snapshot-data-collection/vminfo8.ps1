# Define Output File
$outputFile = "C:\VM_Information_Report.txt"

# Collect System Information
$systemInfo = Get-ComputerInfo
$osInfo = Get-CimInstance Win32_OperatingSystem
$cpuInfo = Get-CimInstance Win32_Processor
$memoryInfo = Get-CimInstance Win32_PhysicalMemory
$diskInfo = Get-CimInstance Win32_DiskDrive
$networkInfo = Get-NetAdapter
$vmInfo = Get-CimInstance Win32_ComputerSystem
$totalRAM = (Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1MB
$totalCores = (Get-CimInstance Win32_Processor | Measure-Object NumberOfCores -Sum).Sum

# Capture All Processes and Services
$allProcesses = Get-Process | Select-Object ProcessName, CPU, WS, VM, Id, Threads, Handles, Path
$allServices = Get-Service | Select-Object Name, DisplayName, Status, StartType

# Monitor I/O Wait Time and System Responsiveness
$ioWaitTime = Get-Counter '\Processor(_Total)\% IO Wait Time'
$diskQueueLength = Get-Counter '\PhysicalDisk(_Total)\Current Disk Queue Length'

# Capture Memory Leaks (Processes Consuming High Memory)
$memoryLeaks = Get-Process | Where-Object { $_.VirtualMemorySize64 -gt 1GB } | Select-Object ProcessName, Id, VirtualMemorySize64

# Monitor Threads Usage by Processes and Applications
$highThreadUsage = Get-Process | Sort-Object Threads -Descending | Select-Object -First 10 ProcessName, Id, Threads, Handles, Path

# Network Analysis Per Process
$networkUsage = Get-WmiObject Win32_PerfFormattedData_Tcpip_NetworkInterface | 
    Where-Object { $_.BytesSentPerSec -gt 0 -or $_.BytesReceivedPerSec -gt 0 } | 
    Sort-Object BytesSentPerSec -Descending | 
    Select-Object Name, BytesSentPerSec, BytesReceivedPerSec

# Write Information to Output File
"System Information:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$systemInfo | Out-File -FilePath $outputFile -Append -Encoding utf8
$osInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$cpuInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$memoryInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$diskInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$networkInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
$vmInfo | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8
"Total RAM (MB): $totalRAM" | Out-File -FilePath $outputFile -Append -Encoding utf8
"Total CPU Cores: $totalCores" | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nAll Running Processes:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$allProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nAll Running Services:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$allServices | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nMonitor I/O Wait Time:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$ioWaitTime | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nDisk Queue Length:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$diskQueueLength | Format-List | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nMemory Leaks (High Virtual Memory Processes):`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$memoryLeaks | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nProcesses Using High Threads and Handles:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$highThreadUsage | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nNetwork Analysis Per Process:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$networkUsage | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

Write-Host "VM information and performance metrics saved to $outputFile"
