# Home

This is a personal dashboard implemented as a web application. It is intended to show news, twitter user timelines, sports results, and personal reminders - all in a single web page.

Current development progress:

- [x] news feeds via RSS
- [ ] twitter timelines
- [ ] sports results
- [ ] reminders

## Launching

The simplest way to launch the dashboard app is by using Docker Compose. [Install it](https://docs.docker.com/compose/install/), then grab the [compose file](deploy/docker-compose.yml) from this repository and run `docker-compose up -d` - this will launch both the backend application and the Datomic database.

Then point your browser to http://localhost:9999, add some RSS feeds and enjoy!
