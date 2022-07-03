docker run --rm -d --name postgres -e POSTGRES_USER=root -e POSTGRES_PASSWORD=toor -e POSTGRES_DB=data -p 5429:5432 -v /data/trading-bot:/var/lib/postgresql/data postgres
