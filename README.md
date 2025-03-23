
# 📊 VM Information and Performance Monitoring Script

This PowerShell script (`vminfo7.ps1`) provides a comprehensive snapshot of system configuration and performance metrics for a Windows virtual machine. It is designed to assist in analyzing and monitoring system resource utilization including CPU, memory, disk, and network statistics.

---

## 🧩 Features

- 📌 Collects detailed OS and hardware specifications
- 📌 Lists all running processes and services
- 📌 Tracks I/O wait time and system responsiveness
- 📌 Detects potential memory leaks
- 📌 Reports on thread usage and handles per process
- 📌 Performs network usage analysis per process
- 📌 Supports scheduled execution every 10 seconds for 2 hours
- 📌 Output stored in `.csv` or `.txt` format for easy analysis

---

## 🛠️ Design Components

| Category       | Metrics Captured |
|----------------|------------------|
| **System Info** | OS version, CPU, RAM, BIOS, GPU, Disk, Network |
| **Processes**   | Name, ID, CPU, RAM (WS), VM, Threads, Handles, Path |
| **Performance Counters** | I/O Wait, Page File %, Virtual Memory, Pages/sec, Disk Queue |
| **Services**    | Status, StartType, DisplayName |
| **Network**     | Bytes sent/received per interface |
| **Memory Leaks**| Processes with VM > 1GB |
| **Threads Usage**| Top 10 processes by thread count |

---

## 🖥️ Usage Instructions

### 🔹 1. Manual Execution

```powershell
.minfo7.ps1
```

### 🔹 2. Schedule for Continuous Logging (Every 10s for 2 Hours)

```powershell
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -WindowStyle Hidden -File C:\vminfo7.ps1"
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date).AddSeconds(10) -RepetitionInterval (New-TimeSpan -Seconds 10) -RepetitionDuration (New-TimeSpan -Hours 2)
Register-ScheduledTask -TaskName "VM_Monitoring_Task" -Action $action -Trigger $trigger
```

### 🔹 3. Stop the Scheduled Task

```powershell
Unregister-ScheduledTask -TaskName "VM_Monitoring_Task" -Confirm:$false
```

---

## 📁 Output Format

- **File Path**: `C:\VM_Information_Report.txt` or `.csv`
- **Contents**: Structured summary of metrics per timestamp

Example for process stats:
```
ProcessName, CPU, WS, VM, Id, Threads, Handles, Path
QTCE, 78.2, 1345MB, 2048MB, 4321, 100, 500, C:\Program Files\QTCE\qtce.exe
```

---

## 📊 Interpretation and Recommendations

| Indicator | Possible Cause | Suggested Action |
|----------|----------------|------------------|
| High CPU (QTCE) | Heavy computation, poor threading | Optimize algorithm and threads |
| High I/O Wait | Disk bottleneck | Check SSD/IO configuration |
| Large VM Memory | Memory leaks | Profile and optimize allocations |
| svchost CPU spikes | Windows services | Audit services via `tasklist /svc` |
| High Pages/sec | Memory pressure | Add RAM or optimize usage |

---

## ⚙️ Requirements

- PowerShell 5.1 or newer
- Admin privileges (recommended for full access)

---

## 📌 Notes

- Adapt the script to `.csv` output for better parsing
- Ensure task scheduler policy permits high-frequency triggers
- Useful for DevOps, IT admin audits, and performance benchmarking

---

## 🚀 Future Enhancements

- Export to Excel format
- Email alerts for threshold breaches
- Graphical summary using Power BI or Python
