# Configuration Specification - Machine Maintenance Portal server

Target node: Ubuntu 24.04 (WSL2). Managed with Ansible (`site.yml`, `inventory.ini`).

| Area | Item | Details |
|---|---|---|
| Packages | openjdk-17-jre-headless | Java runtime for the Spring Boot jar |
| Packages | mysql-server | Database server |
| Packages | python3-pymysql | Lets Ansible manage MySQL |
| Packages | curl | Health checks and troubleshooting |
| Group / user | portal / portal | System account, no login shell, runs the application |
| Folders | /opt/machine-portal | Application home (owner portal) |
| Folders | /opt/machine-portal/releases/<version> | One folder per released version |
| Folders | /etc/machine-portal | Configuration (root:portal, mode 0750) |
| Folders | /var/log/machine-portal | Application log (owner portal) |
| Files | releases/<version>/app.jar | The application jar |
| Files | /opt/machine-portal/current | Symlink to the active release (used for rollback) |
| Files | /etc/machine-portal/portal.env | DB_URL, DB_USERNAME, DB_PASSWORD, SERVER_PORT (mode 0640) |
| Files | /etc/systemd/system/machine-portal.service | Service definition |
| Database | machine_maintenance, user portal | Created by Ansible, privileges limited to that database |
| Ports | 8082/tcp | Portal web application |
| Ports | 3306/tcp (localhost only) | MySQL |
| Services | mysql | Enabled, started |
| Services | machine-portal | Enabled, started, restarted on failure |

Variables are in `group_vars/all.yml` (version, port, database settings, jar path).
Idempotent: running the playbook again changes nothing unless a file or setting changed.
