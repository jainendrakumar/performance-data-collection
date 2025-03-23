# Continuous Performance Monitoring Script

# Define Output File
$outputFile = "C:\Performance_Metrics_Report.csv"

# Initialize CSV with Headers
"Timestamp,CPU Usage (%),Memory Usage (MB),Disk Read (MB/s),Disk Write (MB/s),Network Sent (MB/s),Network Received (MB/s)" | Out-File -FilePath $outputFile -Encoding utf8

# Function to Capture Performance Metrics
function Capture-PerformanceMetrics {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $cpuUsage = (Get-Counter '\Processor(_Total)\% Processor Time').CounterSamples.CookedValue
    $memoryUsage = (Get-Counter '\Memory\Available MBytes').CounterSamples.CookedValue
    $diskRead = (Get-Counter '\PhysicalDisk(_Total)\Disk Read Bytes/sec').CounterSamples.CookedValue / 1MB
    $diskWrite = (Get-Counter '\PhysicalDisk(_Total)\Disk Write Bytes/sec').CounterSamples.CookedValue / 1MB
    $networkSent = (Get-Counter '\Network Interface(*)\Bytes Sent/sec').CounterSamples.CookedValue / 1MB
    $networkReceived = (Get-Counter '\Network Interface(*)\Bytes Received/sec').CounterSamples.CookedValue / 1MB
    
    "$timestamp,$cpuUsage,$memoryUsage,$diskRead,$diskWrite,$networkSent,$networkReceived" | Out-File -FilePath $outputFile -Append -Encoding utf8
}

# Monitor Performance for 2 Minutes (120 seconds)
Write-Host "Monitoring Performance for 2 minutes..."
$endTime = (Get-Date).AddMinutes(2)
while ((Get-Date) -lt $endTime) {
    Capture-PerformanceMetrics
    Start-Sleep -Seconds 1  # Capture metrics every second
}

# Analysis: Read and Analyze Data
$metrics = Import-Csv -Path $outputFile
$metrics | ConvertTo-Csv -NoTypeInformation | Out-File "C:\Performance_Metrics_Processed.csv"

# Generate Graphs using PowerShell Graphing Module
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Windows.Forms.DataVisualization

$Form = New-Object Windows.Forms.Form
$Form.Text = "Performance Metrics Graph"
$Form.Size = New-Object Drawing.Size(800,600)

$Chart = New-Object Windows.Forms.DataVisualization.Charting.Chart
$Chart.Width = 750
$Chart.Height = 500
$Chart.Left = 25
$Chart.Top = 25

$ChartArea = New-Object Windows.Forms.DataVisualization.Charting.ChartArea
$Chart.ChartAreas.Add($ChartArea)

$metrics[0].PSObject.Properties.Name | Where-Object { $_ -ne "Timestamp" } | ForEach-Object {
    $series = New-Object Windows.Forms.DataVisualization.Charting.Series
    $series.ChartType = [System.Windows.Forms.DataVisualization.Charting.SeriesChartType]::Line
    $series.Name = $_
    
    $metrics | ForEach-Object {
        $time = [datetime]::Parse($_.Timestamp)
        $value = [double]$_.$_
        $series.Points.AddXY($time, $value)
    }
    
    $Chart.Series.Add($series)
}

$Form.Controls.Add($Chart)
$Form.ShowDialog()
