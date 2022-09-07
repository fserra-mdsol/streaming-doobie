## Doobie + http4s streaming POC

Sample project to demonstrate a simple composed stream that looks like the following: 

![Graph](src/main/resources/graph.png?raw=true "Graph")

It simulates an external API (provided by the `server` object) which is queried by an `http4s` `Ember` client.

The Api streams its response to the client, so that the stream of entities (`Foo`s) returned can be composed (zipped)
with a second stream of entities (`Bar`s) that comes from a local DB, using the streaming capabilities of `doobie`.
The composed stream then inserts a new entity (`Baz`) derived by each composed entity from the zipped stream, into the same DB.

# Run

To run the components, first start a docker container with

`docker run --name streamings -p5432:5432  -e POSTGRES_PASSWORD="postgres" -d postgres`

then start first the server process running the `server` object and, on a separate terminal, run the `streamings` object.

The output can be observed both on the latter's STDOUT and by querying the postgres instance in the container.