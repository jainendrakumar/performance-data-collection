# Define the log file path
$LogPath = "C:\EventViewerLogs.txt"
if (!(Test-Path "C:\Logs")) { New-Item -ItemType Directory -Path "C:\Logs" }

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
Get-WinEvent -FilterHashtable @{
    LogName=$LogNames
    Level=1,2,3
    StartTime=$StartDate
} | Select-Object TimeCreated, Id, LevelDisplayName, ProviderName, Message |
Format-Table -AutoSize | Out-File -FilePath $LogPath

Write-Host "Logs saved to: $LogPath"