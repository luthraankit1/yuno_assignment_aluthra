docker build --no-cache db -t db
docker build --no-cache app -t app
docker compose up -d