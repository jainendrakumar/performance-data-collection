# Comprehensive Windows VM Information and Performance Monitoring Script

# Define Output File
$outputFile = "C:\VM_Information_Report_v4.txt"

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

# Capture Top 10 Resource-Consuming Applications and Services
$topProcesses = Get-Process | Sort-Object CPU -Descending | Select-Object -First 10 ProcessName, CPU, WS, VM
$topServices = Get-WmiObject Win32_Service | Sort-Object ProcessId | Select-Object -First 10 Name, State, StartMode, ProcessId

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
"Total RAM (MB): $totalRAM" | Out-File -FilePath $outputFile -Append -Encoding utf8
"Total CPU Cores: $totalCores" | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nTop 10 Resource-Consuming Applications:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$topProcesses | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

"`nTop 10 Running Services:`n" | Out-File -FilePath $outputFile -Append -Encoding utf8
$topServices | Format-Table -AutoSize | Out-File -FilePath $outputFile -Append -Encoding utf8

# Monitor Performance for 2 Minutes (120 seconds)
Write-Host "Monitoring Performance for 2 minutes..."
$endTime = (Get-Date).AddMinutes(2)
while ((Get-Date) -lt $endTime) {
    Capture-PerformanceMetrics
    Start-Sleep -Seconds 1  # Capture metrics every second
}

Write-Host "VM information and performance metrics saved to $outputFile"