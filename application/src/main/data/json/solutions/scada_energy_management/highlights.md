The SCADA Energy management template provides a comprehensive solution for real-time monitoring, control, and optimization of energy systems.
It includes a feature-rich dashboard for managing devices, visualizing energy flow, and configuring alarms—all in a single, intuitive interface.

Solution automatically configures the IoT gateway and creates seven key energy devices representing all layers of the energy infrastructure.

**Hardware Components**:

* **Solar panel**: Simulates solar power generation, with real-time energy output.
* **Wind turbine**: Tracks wind-generated power and allows control of turbine state.
* **Battery**: Monitors charge level and output; supports charging/discharging modes.
* **Inverter**: Displays operational mode (Inverter/Charger), power status, and system alerts (overload, temperature).
* **Generator**: Enables control and status monitoring of on-demand power generation.
* **Power transformer**: Provides voltage insights and tap-specific power output.
* **Consumption**: Aggregates and visualizes energy usage data across the system.

**Software Components**:

* **IoT Gateway**: Utilizes the ModBus protocol to communicate with physical energy equipment.
* **Device Emulator**: Modbus energy emulator that simulates seven virtual devices for easy deployment and testing.
* **Dashboard**: An intuitive dashboard presents a live overview of the entire energy system, allowing users to monitor energy flow and manage device states with precision.
