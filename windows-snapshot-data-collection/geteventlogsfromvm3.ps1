# Define the output file path
$LogPath = "C:\Logs\ComprehensiveEventLogs.txt"
if (!(Test-Path "C:\Logs")) { New-Item -ItemType Directory -Path "C:\Logs" }

# Header Information
$Header = @"
=====================================================
COMPREHENSIVE SYSTEM EVENTS REPORT
=====================================================
DATE RANGE: Last 15 Days
INCLUDES: Errors, Critical, Warnings, Crashes, Shutdowns
=====================================================
"@

# Get System Information
$SystemInfo = @"
=====================================================
SYSTEM INFORMATION
=====================================================
"@
$SystemInfo += (Get-ComputerInfo | Select-Object CsName, WindowsVersion, OsArchitecture, OsBuildNumber, TotalPhysicalMemory, BiosManufacturer, BiosSMBIOSBIOSVersion, CsNumberOfLogicalProcessors, CsNumberOfProcessors, CsHypervisorPresent, CsModel, CsManufacturer | Format-List | Out-String)

# Define logs to extract
$LogNames = @(
    "Application", "System", "Security", "Setup",
    "Microsoft-Windows-TaskScheduler/Operational",
    "Microsoft-Windows-NetworkProfile/Operational",
    "Microsoft-Windows-Diagnostics-Performance/Operational",
    "Microsoft-Windows-FailoverClustering/Operational",
    "Microsoft-Windows-DriverFrameworks-UserMode/Operational",
    "Microsoft-Windows-Authentication/Authentication",
    "Microsoft-Windows-GroupPolicy/Operational",
    "Microsoft-Windows-Ntfs/Operational",
    "Microsoft-Windows-Kernel-General", "Microsoft-Windows-Kernel-Power",
    "Microsoft-Windows-UserModePowerService/Operational",
    "Microsoft-Windows-WindowsUpdateClient/Operational"
)

# Get logs for the last 15 days
$StartDate = (Get-Date).AddDays(-15)

# Extracting logs: Critical (1), Error (2), Warning (3)
$CriticalErrorWarningLogs = Get-WinEvent -FilterHashtable @{
    LogName = $LogNames
    Level = 1,2,3
    StartTime = $StartDate
} | Select-Object TimeCreated, Id, LevelDisplayName, ProviderName, Message |
Format-Table -AutoSize | Out-String

# Extracting logs for Crashes and Shutdowns
$CrashShutdownLogs = Get-WinEvent -FilterHashtable @{
    LogName = "System"
    Id = 41, 6005, 6006, 6008, 1074, 109, 110
    StartTime = $StartDate
} | Select-Object TimeCreated, Id, LevelDisplayName, ProviderName, Message |
Format-Table -AutoSize | Out-String

# Extract Application-Specific Errors
$AppErrors = Get-WinEvent -FilterHashtable @{
    LogName = "Application"
    Level = 1,2,3
    StartTime = $StartDate
} | Select-Object TimeCreated, Id, LevelDisplayName, ProviderName, Message |
Format-Table -AutoSize | Out-String

# Save logs to file
$Header | Out-File -FilePath $LogPath
$SystemInfo | Out-File -FilePath $LogPath -Append

@"
=====================================================
CRITICAL, ERROR & WARNING EVENTS
=====================================================
"@ | Out-File -FilePath $LogPath -Append
$CriticalErrorWarningLogs | Out-File -FilePath $LogPath -Append

@"
=====================================================
SYSTEM CRASHES & SHUTDOWNS
=====================================================
"@ | Out-File -FilePath $LogPath -Append
$CrashShutdownLogs | Out-File -FilePath $LogPath -Append

@"
=====================================================
APPLICATION ERRORS
=====================================================
"@ | Out-File -FilePath $LogPath -Append
$AppErrors | Out-File -FilePath $LogPath -Append

Write-Host "Logs saved successfully to: $LogPath"
