## Synopsis

This is a sample project how to work with akka-http

## API Reference

##### POST /tickets/register
```json
{
  "imdbId": "tt0111161",
  "availableSeats": 100,
  "screenId": "screen_123456"
}  
```
##### POST /tickets/reserve
```json
{
  "imdbId": "tt0111161",
  "screenId": "screen_123456"
}  
```
##### GET /tickets/state
```json
{
  "imdbId": "tt0111161",
  "screenId": "screen_123456"
}  
```

## Tests

To run tests type "sbt test" in the root project folder.

### Run

To run the application type "sbt run" in the root project folder.

### Curl queries
 
```
curl -H "Content-Type: application/json" -X POST -d '{"imdbId":"tt0137523", "availableSeats":10, "screenId":"screen_123456"}' http://localhost:8080/tickets/register
curl -H "Content-Type: application/json" -X POST -d '{"imdbId":"tt0137523", "screenId":"screen_123456"}' http://localhost:8080/tickets/reserve
curl -H "Content-Type: application/json" -X GET -d '{"imdbId":"tt0137523", "screenId":"screen_123456"}' http://localhost:8080/tickets/state
```
