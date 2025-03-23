Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Windows.Forms.DataVisualization

# Path to the CSV file
$csvPath = "C:\Drive_Performance_Log.csv"

# Check if file exists
if (!(Test-Path $csvPath)) {
    Write-Host "❌ CSV file not found: $csvPath"
    exit
}

# Load data from CSV
$data = Import-Csv $csvPath
$drives = $data | Select-Object -ExpandProperty Drive -Unique

foreach ($drive in $drives) {
    # Filter only rows with numeric read/write values
    $driveData = $data | Where-Object {
        $_.Drive -eq $drive -and 
        [double]::TryParse($_.'Disk Read Bytes/sec', [ref]0) -and 
        [double]::TryParse($_.'Disk Write Bytes/sec', [ref]0)
    }

    if ($driveData.Count -eq 0) {
        Write-Host "⚠️ No valid data to plot for $drive"
        continue
    }

    # Create Form
    $form = New-Object Windows.Forms.Form
    $form.Text = "Drive Performance - $drive"
    $form.Width = 1000
    $form.Height = 600

    $chart = New-Object Windows.Forms.DataVisualization.Charting.Chart
    $chart.Width = 950
    $chart.Height = 550
    $chart.Dock = 'Fill'

    $chartArea = New-Object Windows.Forms.DataVisualization.Charting.ChartArea
    $chartArea.AxisX.Title = "Time"
    $chartArea.AxisY.Title = "Bytes/sec"
    $chartArea.AxisX.LabelStyle.Format = "HH:mm:ss"
    $chart.ChartAreas.Add($chartArea)

    $seriesRead = New-Object Windows.Forms.DataVisualization.Charting.Series
    $seriesRead.Name = "Read Bytes/sec"
    $seriesRead.ChartType = 'Line'
    $seriesRead.XValueType = 'DateTime'

    $seriesWrite = New-Object Windows.Forms.DataVisualization.Charting.Series
    $seriesWrite.Name = "Write Bytes/sec"
    $seriesWrite.ChartType = 'Line'
    $seriesWrite.XValueType = 'DateTime'

    # Add data points to chart
    foreach ($row in $driveData) {
        $timestamp = [datetime]::ParseExact($row.Timestamp, 'yyyy-MM-dd HH:mm:ss', $null)
        $read = [double]$row.'Disk Read Bytes/sec'
        $write = [double]$row.'Disk Write Bytes/sec'
        $seriesRead.Points.AddXY($timestamp, $read)
        $seriesWrite.Points.AddXY($timestamp, $write)
    }

    $chart.Series.Add($seriesRead)
    $chart.Series.Add($seriesWrite)
    $form.Controls.Add($chart)

    # Display
    $form.Add_Shown({ $form.Activate() })
    [System.Windows.Forms.Application]::Run($form)
}
