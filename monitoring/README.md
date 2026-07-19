# YOEN Monitoring

The repository provides two monitoring modes.

- Local lab: Prometheus, Loki, Grafana, and Alloy all run in Docker.
- Production: only Alloy runs on the EC2 instance and sends metrics and logs to Grafana Cloud.

## 1. Application metrics

The Spring Boot application exposes Prometheus metrics at:

```text
http://localhost:8080/actuator/prometheus
```

The endpoint includes JVM, HTTP server, process, system, and HikariCP metrics when the corresponding components are active.

## 2. Run the local lab

Start the backend on port 8080 first. Then start the monitoring stack from the repository root:

```powershell
docker compose -f compose.monitoring.local.yaml up -d
```

Open the following local-only endpoints:

| Component | URL |
| --- | --- |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Alloy UI | http://localhost:12345 |
| Loki readiness | http://localhost:3100/ready |

The initial Grafana credentials are `admin` / `admin`. Override the password before starting if desired:

```powershell
$env:GRAFANA_ADMIN_PASSWORD="a-local-password"
docker compose -f compose.monitoring.local.yaml up -d
```

Grafana provisions both data sources and the `YOEN / YOEN Overview` dashboard automatically.

Useful checks:

```powershell
docker compose -f compose.monitoring.local.yaml ps
docker compose -f compose.monitoring.local.yaml logs alloy
docker compose -f compose.monitoring.local.yaml logs loki
```

Stop the lab without deleting collected data:

```powershell
docker compose -f compose.monitoring.local.yaml down
```

Do not add `-v` unless deleting all local Prometheus, Loki, Alloy, and Grafana data is intentional.

## 3. Create Grafana Cloud credentials

Create a Grafana Cloud stack and obtain these values from its metrics and logs send-data configuration:

- Prometheus remote-write URL
- Prometheus username / instance ID
- Loki push URL
- Loki username / instance ID
- An access policy token with `metrics:write` and `logs:write`

Copy `monitoring/.env.cloud.example` to a server-only file named `.env.monitoring` and replace every placeholder. Never commit that file.

## 4. Run Alloy on the EC2 instance

Before deploying the application metric endpoint, block public access to it in the public Nginx server while leaving normal API proxying unchanged. Alloy connects directly to the application container and does not use Nginx.

```nginx
location = /actuator/prometheus {
    return 404;
}
```

Reload Nginx only after validating its configuration:

```bash
sudo docker exec yoen-back-nginx nginx -t
sudo docker exec yoen-back-nginx nginx -s reload
```

Place the following repository files under `/opt/yoen` while preserving their relative paths:

```text
/opt/yoen/compose.alloy.cloud.yaml
/opt/yoen/.env.monitoring
/opt/yoen/monitoring/alloy/cloud.config.alloy
```

Protect the credentials and validate the Compose model:

```bash
cd /opt/yoen
sudo chmod 600 .env.monitoring
sudo docker compose --env-file .env.monitoring -f compose.alloy.cloud.yaml config
```

Start Alloy:

```bash
sudo docker compose --env-file .env.monitoring -f compose.alloy.cloud.yaml up -d
```

Verify it:

```bash
sudo docker ps --filter name=yoen-monitoring-alloy
sudo docker logs --tail 100 yoen-monitoring-alloy
curl --fail --silent http://127.0.0.1:12345/-/ready
```

The production configuration:

- discovers only the currently running `app-blue` or `app-green` container for Spring metrics;
- collects host CPU, memory, filesystem, load, network, and VM metrics;
- collects logs from containers whose names start with `yoen-back-`;
- sends metrics and logs directly to Grafana Cloud;
- limits Alloy to 192 MiB RAM and 0.25 CPU;
- exposes the Alloy UI only on EC2 loopback.

To inspect the Alloy UI from a workstation, create an SSH tunnel:

```powershell
ssh -i "C:\path\yoen.pem" -L 12345:localhost:12345 ubuntu@EC2_PUBLIC_IP
```

Then open http://localhost:12345.

## 5. Verify data in Grafana Cloud

In Grafana Cloud Explore:

- Metrics: query `up{job="yoen-backend"}`.
- JVM heap: query `jvm_memory_used_bytes{job="yoen-backend", area="heap"}`.
- Logs: query `{project="yoen", environment="production"}`.
- Host memory: query `node_memory_MemAvailable_bytes{project="yoen"}`.

The Spring application image containing this branch must be deployed before `/actuator/prometheus` can be scraped successfully.
