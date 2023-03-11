# Discord Role Bot

## Setup

docker-compose.yml
```yml
version: '3.7'

services:
  role-bot:
    image: ghcr.io/techtony96/discord-role-bot:master
    restart: unless-stopped
    environment:
      - BOT_TOKEN=foo
```