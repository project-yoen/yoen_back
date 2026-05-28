# EC2 Docker Compose Deployment

This deployment shape runs Spring Boot, PostgreSQL, and Redis in Docker on one EC2 Ubuntu host.

## Files

- `Dockerfile`: builds the Spring Boot jar and runs it with Java 17.
- `compose.ec2.yaml`: runs `app`, `postgres`, and `redis` together.
- `.env.ec2.example`: template for the production `.env`.

## EC2 Setup

Install Docker and the Compose plugin on the EC2 instance, then clone this repository.

Create the runtime environment file:

```bash
cp .env.ec2.example .env
```

Edit `.env` and replace every placeholder value.

Create the Firebase secret file:

```bash
mkdir -p secrets
```

Place the Firebase service account JSON at:

```text
secrets/firebase-service-account.json
```

Do not commit `.env` or files in `secrets/`.

## Run

```bash
docker compose -f compose.ec2.yaml up -d --build
```

Check status and logs:

```bash
docker compose -f compose.ec2.yaml ps
docker compose -f compose.ec2.yaml logs -f app
```

Stop:

```bash
docker compose -f compose.ec2.yaml down
```

## Network Notes

Inside Docker Compose, Spring must use service names:

- PostgreSQL host: `postgres`
- Redis host: `redis`

That is why `compose.ec2.yaml` overrides:

```properties
DATABASE_URL=jdbc:postgresql://postgres:5432/${DATABASE_NAME}
REDIS_HOST=redis
```

PostgreSQL and Redis are not published to the EC2 public network in this configuration. Only the Spring Boot port `8080` is published.

## EC2 Security Group

For a direct test, open inbound TCP `8080` only from your IP. For production, put Nginx or a load balancer in front and expose `80/443` instead.
