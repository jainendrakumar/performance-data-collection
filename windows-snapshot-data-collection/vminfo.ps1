# Get System Information
$computerInfo = Get-ComputerInfo
$os = Get-CimInstance Win32_OperatingSystem
$cpu = Get-CimInstance Win32_Processor
$memory = Get-CimInstance Win32_PhysicalMemory
$diskDrives = Get-CimInstance Win32_DiskDrive
$networkAdapters = Get-NetAdapter

# Output System Information
$systemReport = @{
    "Computer Name" = $computerInfo.CsName
    "OS" = "$($os.Caption) ($($os.Version))"
    "Manufacturer" = $computerInfo.Manufacturer
    "Model" = $computerInfo.Model
    "CPU" = "$($cpu.Name) ($($cpu.NumberOfCores) Cores, $($cpu.NumberOfLogicalProcessors) Threads)"
    "Total RAM (GB)" = [math]::round(($os.TotalVisibleMemorySize / 1MB), 2)
}

# Get Disk Information
$diskReport = $diskDrives | ForEach-Object {
    "Drive: $($_.DeviceID) - Model: $($_.Model) - Size: $([math]::round($_.Size / 1GB, 2)) GB"
}

# Get Network Information
$networkReport = $networkAdapters | ForEach-Object {
    "Adapter: $($_.Name) - Status: $($_.Status) - MAC: $($_.MacAddress) - Speed: $($_.LinkSpeed) bps"
}

# Output Report
Write-Host "--- System Information ---"
$systemReport.GetEnumerator() | ForEach-Object { Write-Host "$($_.Key): $($_.Value)" }

Write-Host "`n--- Disk Information ---"
$diskReport | ForEach-Object { Write-Host $_ }

Write-Host "`n--- Network Information ---"
$networkReport | ForEach-Object { Write-Host $_ }

# Save to File
$outputFile = "C:\VM_Details_Report.txt"
$reportContent = @()
$reportContent += "--- System Information ---"
$reportContent += $systemReport.GetEnumerator() | ForEach-Object { "$_" }
$reportContent += "`n--- Disk Information ---"
$reportContent += $diskReport
$reportContent += "`n--- Network Information ---"
$reportContent += $networkReport
$reportContent | Out-File -FilePath $outputFile -Encoding utf8

Write-Host "`nReport saved to $outputFile"