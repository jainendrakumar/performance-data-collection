Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Windows.Forms.DataVisualization

# Input CSV path
$csvPath = "C:\Drive_Performance_Log.csv"

# Read CSV
$data = Import-Csv $csvPath

# Get unique drives
$drives = $data | Select-Object -ExpandProperty Drive -Unique

foreach ($drive in $drives) {
    $chartForm = New-Object Windows.Forms.Form
    $chartForm.Text = "Drive Performance - $drive"
    $chartForm.Width = 1000
    $chartForm.Height = 600

    $chart = New-Object Windows.Forms.DataVisualization.Charting.Chart
    $chart.Width = 950
    $chart.Height = 550

    $chartArea = New-Object Windows.Forms.DataVisualization.Charting.ChartArea
    $chart.ChartAreas.Add($chartArea)

    $seriesRead = New-Object Windows.Forms.DataVisualization.Charting.Series
    $seriesRead.Name = "Read Bytes/sec"
    $seriesRead.ChartType = "Line"

    $seriesWrite = New-Object Windows.Forms.DataVisualization.Charting.Series
    $seriesWrite.Name = "Write Bytes/sec"
    $seriesWrite.ChartType = "Line"

    # Add data points
    $driveData = $data | Where-Object { $_.Drive -eq $drive -and $_.'Disk Read Bytes/sec' -ne "N/A" }

    foreach ($row in $driveData) {
        $timestamp = [datetime]::Parse($row.Timestamp)
        $read = [double]::Parse($row.'Disk Read Bytes/sec')
        $write = [double]::Parse($row.'Disk Write Bytes/sec')
        $seriesRead.Points.AddXY($timestamp, $read)
        $seriesWrite.Points.AddXY($timestamp, $write)
    }

    $chart.Series.Add($seriesRead)
    $chart.Series.Add($seriesWrite)
    $chart.Dock = 'Fill'

    $chartForm.Controls.Add($chart)
    $chartForm.Add_Shown({ $chartForm.Activate() })
    $chartForm.ShowDialog()
}
