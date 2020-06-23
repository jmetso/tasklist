# Ansible playbooks

## Installation

File: [install.yaml](install.yaml)

This playbook can be used to install the application as an 'old school' linux service running on top of OpenJDK. The 
playbook uses the tasklist variables section in the [sample.inventory](sample.inventory) and [sample.vault](sample.vault)
files.

### Variables

|Variable name|Location|Description|
| :--- | :--- | :--- |
|install_dir|Inventory|Installation directory on remote machine|
|service_user|Inventory|Which user to run the linux service|
|vault_smtp_password|Vault|Email server password|

### Procedure

1. Create installation directory and needed subdirectories
2. Copy configuration file
3. Copy jar file
4. Install and start service

**Note** Installation playbook assumes that there is a built jar available in the target folder in the project root.

### How to run the playbook

```
ansible-playbook -i <inventory.file> -e @<vault.file> --ask-vault-pass install.yaml
```

## Uninstallation

File: [uninstall.yml](uninstall.yml)

This playbook can be used to uninstall the application from an 'old school' linux service installation using OpenJDK. The
playbook uses the tasklist variables section in the [sample.inventory](sample.inventory) file.

### Variables

|Variable name|Location|Description|
| :--- | :--- | :--- |
|install_dir|Inventory|Installation directory on remote machine|

### Procedure

1. Stop and remove service
2. Delete installation directory and it's contents

**Note** Installation playbook assumes that there is a built jar available in the target folder in the project root.

### How to run the playbook

```
ansible-playbook -i <inventory.file> uninstall.yml
```

## Setup CI/CD environment with Jenkins

to-be-defined