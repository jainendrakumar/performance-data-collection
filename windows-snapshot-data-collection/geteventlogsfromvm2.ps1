# Define the log file path
$LogPath = "C:\Logs\SystemAndEventLogs.txt"
if (!(Test-Path "C:\Logs")) { New-Item -ItemType Directory -Path "C:\Logs" }

# Get System Information
$SystemInfo = @"
====================================================
SYSTEM INFORMATION
====================================================
"@
$SystemInfo += (Get-ComputerInfo | Select-Object CsName, WindowsVersion, OsArchitecture, OsBuildNumber, TotalPhysicalMemory, BiosManufacturer, BiosSMBIOSBIOSVersion, CsNumberOfLogicalProcessors, CsNumberOfProcessors, CsHypervisorPresent, CsModel, CsManufacturer | Format-List | Out-String)

# Define event logs to check
$LogNames = @(
    "Application", "System", "Security", "Setup", "Microsoft-Windows-TaskScheduler/Operational",
    "Microsoft-Windows-NetworkProfile/Operational", "Microsoft-Windows-Diagnostics-Performance/Operational",
    "Microsoft-Windows-FailoverClustering/Operational", "Microsoft-Windows-DriverFrameworks-UserMode/Operational",
    "Microsoft-Windows-Authentication/Authentication", "Microsoft-Windows-GroupPolicy/Operational",
    "Microsoft-Windows-Ntfs/Operational", "Microsoft-Windows-Kernel-General", "Microsoft-Windows-Kernel-Power"
)

# Get logs from the last 15 days with specified levels (1=Critical, 2=Error, 3=Warning)
$StartDate = (Get-Date).AddDays(-15)
$EventLogs = Get-WinEvent -FilterHashtable @{
    LogName=$LogNames
    Level=1,2,3
    StartTime=$StartDate
} | Select-Object TimeCreated, Id, LevelDisplayName, ProviderName, Message |
Format-Table -AutoSize | Out-String

# Save both system information and event logs to the file
$SystemInfo | Out-File -FilePath $LogPath
$EventLogs | Out-File -FilePath $LogPath -Append

Write-Host "System information and logs saved to: $LogPath"
