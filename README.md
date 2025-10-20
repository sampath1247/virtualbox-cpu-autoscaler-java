# virtualbox-cpu-autoscaler-java
Java host controller that hot-plugs CPUs into a VirtualBox VM based on Guest/CPU/Load/Idle.
# VirtualBox CPU Hot-Plug Controller 

Elastic CPU scaling for a VirtualBox VM using Java. The host program monitors the VM's `Guest/CPU/Load/Idle` metric and hot-plugs/unplugs vCPUs on-the-fly based on load. A guest-side program generates CPU-intensive load to demonstrate elasticity.

## Demo
- Key behavior: When idle% drops below 75%, plug a CPU; when idle% stays above 85% for 2 intervals, unplug a CPU.
- Suggested screenshots:
  - `demo/screenshots/guest-additions.png` — Guest Additions verified
  - `demo/screenshots/metrics-idle-values.png` — `monitor.dat` with idle% samples
  - `demo/screenshots/cpu-plugged.png` — CPU count increased in VBox
  - `demo/screenshots/cpu-unplugged.png` — CPU count decreased in VBox
- Optional video: Add a 90s walkthrough link here.

## Problem
Demonstrate VM elasticity by adjusting CPU capacity at runtime. Monitor the guest OS idle percentage and automatically plug/unplug CPUs in response to changing workload.

## Solution
- Collect `Guest/CPU/Load/Idle` from VirtualBox metrics (2s period).
- Host program (`p04.java`) parses the latest idle% and, using thresholds, decides to plug/unplug CPUs via `VBoxManage controlvm`.
- Guest program (`p04_load.java`) creates CPU load to trigger scaling behavior.

## Key Results
- Under load (e.g., idle% ≈ 66%), the controller plugged in an additional CPU (from 1 to 2).
- After load ceased and idle% > 85% for two checks, it unplugged the extra CPU back to 1.
- Behavior verified in console output, `monitor.dat`, and VirtualBox CPU count.

## Final CPU Usage Formula
`CPU Usage (%) = 100 - Idle%`

- Example: Idle = 95.50% → Usage = 4.50%. Idle = 20.00% → Usage = 80.00%.
- Thresholds (based on idle%):
  - Plug CPU when `idle < 75%`
  - Unplug CPU when `idle > 85%` for 2 consecutive intervals

## Architecture & Design
- Metric: `Guest/CPU/Load/Idle` (requires VirtualBox Guest Additions).
- Period: 2 seconds sampling; host polls approx every 10 seconds.
- Storage: Host writes metric query output to `monitor.dat` (latest line parsed).
- Control:
  - `VBoxManage controlvm <VM> plugcpu <index>`
  - `VBoxManage controlvm <VM> unplugcpu <index>` (CPU 0 cannot be removed)
- Idle% is aggregated across all currently plugged vCPUs.

## Project Structure
```
.
├── src/
│   ├── p04.java         # Host-side controller (metrics + plug/unplug)
│   └── p04_load.java    # Guest-side CPU load generator
├── demo/
│   ├── screenshots/     # Add your screenshots here
│   └── monitor-sample.dat (optional)
├── docs/
│   └── Report.pdf       # Exported from Report.docx (optional)
├── .github/workflows/ci.yml
├── .gitignore
└── README.md
```

## Prerequisites
- VirtualBox 6.1+ (tested on Windows; also works on macOS/Linux)
- VirtualBox Guest Additions installed inside the guest VM
- Java JDK 11+ (recommended: 17)
- VBoxManage in PATH (or call it with full path)
  - Windows default: `"C:\Program Files\Oracle\VirtualBox\VBoxManage.exe"`

## Setup (one time)
1) Enable CPU hot-plug and set max CPUs (with VM powered off)
```powershell
# Replace cicc2_group06 with your VM name
"C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" modifyvm cicc2_group06 --cpu-hotplug on
"C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" modifyvm cicc2_group06 --cpus 4
```

2) Start the VM (GUI or headless)

3) Enable metrics collection (host)
```powershell
"C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" metrics setup --period 2 cicc2_group06 Guest/CPU/Load/Idle
```

## How to Run

A) Inside the guest VM: start CPU load (p04_load.java)
```bash
# In the guest OS terminal
javac p04_load.java
java p04_load
```

B) On the host: run the controller (p04.java)
```bash
# In the host terminal
javac src/p04.java
java -cp src p04
```

What happens:
- The host queries `Guest/CPU/Load/Idle`, writes to `monitor.dat`, extracts the latest idle%.
- If `idle < 75%` and current CPUs < 4 → plugs next CPU.
- If `idle > 85%` for 2 consecutive intervals and current CPUs > 1 → unplugs last CPU.
- CPU 0 cannot be unplugged.

Note on file paths:
- If `VBoxManage` isn’t in PATH, either add it to PATH or replace `VBoxManage` with the full path in your code/commands.

## Verification Tips
- Show VM CPU count changes: VirtualBox Manager → Settings → System → Processor (or use `VBoxManage showvminfo cicc2_group06`).
- Show `monitor.dat` changing idle% values as load starts/stops.
- Confirm Guest Additions are installed inside the VM.

## Error Handling & Robustness
- The host program logs parsing or command errors but continues running.
- If unplugging fails, ensure Guest Additions are installed.
- For large artifacts, use Git LFS or keep them out of the repo.

## What I Learned (aligns to job keywords)
- Elastic resource provisioning on VirtualBox (CPU hot-plugging)
- System metrics collection and parsing (`Guest/CPU/Load/Idle`)
- Java process control (`ProcessBuilder`) and resilience
- Performance-aware scaling thresholds and hysteresis
- Windows/Unix tooling for automation (VBoxManage, PowerShell)

## License
MIT (or Apache-2.0). Add a LICENSE file to declare usage terms.
